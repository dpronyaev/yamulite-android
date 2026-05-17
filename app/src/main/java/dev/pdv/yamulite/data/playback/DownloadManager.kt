package dev.pdv.yamulite.data.playback

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadState { Downloading, Done, Failed }

data class DownloadInfo(
    val state: DownloadState,
    val progress: Float = 0f,
    val localPath: String? = null,
    val error: String? = null,
)

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
) {
    private val baseDir = File(context.filesDir, "tracks").also { it.mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, DownloadInfo>>(scanLocal())
    val downloads: StateFlow<Map<String, DownloadInfo>> = _downloads.asStateFlow()

    init {
        workManager.getWorkInfosByTagFlow(TAG_DOWNLOAD)
            .onEach { infos -> applyWorkInfos(infos) }
            .launchIn(scope)
    }

    fun localPath(trackId: String): String? {
        val f = fileFor(trackId)
        return if (f.exists()) f.absolutePath else null
    }

    fun download(trackId: String) {
        val current = _downloads.value[trackId]?.state
        if (current == DownloadState.Downloading || current == DownloadState.Done) return
        _downloads.update { it + (trackId to DownloadInfo(DownloadState.Downloading)) }
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_TRACK_ID to trackId))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag(TAG_DOWNLOAD)
            .addTag("$TAG_TRACK_PREFIX${safeName(trackId)}")
            .build()
        workManager.enqueueUniqueWork("download_${safeName(trackId)}", ExistingWorkPolicy.KEEP, request)
    }

    fun delete(trackId: String) {
        workManager.cancelUniqueWork("download_${safeName(trackId)}")
        fileFor(trackId).delete()
        File(baseDir, "${safeName(trackId)}.mp3.part").delete()
        _downloads.update { it - trackId }
    }

    private fun applyWorkInfos(infos: List<WorkInfo>) {
        _downloads.update { current ->
            val merged = current.toMutableMap()
            for (info in infos) {
                val safeId = info.tags
                    .firstOrNull { it.startsWith(TAG_TRACK_PREFIX) }
                    ?.removePrefix(TAG_TRACK_PREFIX) ?: continue
                val trackId = safeId.replace("_", ":")
                when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                        if (merged[trackId]?.state != DownloadState.Done)
                            merged[trackId] = DownloadInfo(DownloadState.Downloading, 0f)
                    }
                    WorkInfo.State.RUNNING -> {
                        if (merged[trackId]?.state != DownloadState.Done)
                            merged[trackId] = DownloadInfo(
                                DownloadState.Downloading,
                                info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f),
                            )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        if (merged[trackId]?.state != DownloadState.Done) {
                            val file = fileFor(trackId)
                            merged[trackId] = DownloadInfo(
                                DownloadState.Done, 1f,
                                file.absolutePath.takeIf { file.exists() },
                            )
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        if (merged[trackId]?.state != DownloadState.Done)
                            merged[trackId] = DownloadInfo(
                                DownloadState.Failed,
                                error = info.outputData.getString(DownloadWorker.KEY_ERROR),
                            )
                    }
                    WorkInfo.State.CANCELLED -> {
                        if (merged[trackId]?.state == DownloadState.Downloading)
                            merged.remove(trackId)
                    }
                }
            }
            merged
        }
    }

    private fun fileFor(trackId: String) = File(baseDir, "${safeName(trackId)}.mp3")
    private fun safeName(id: String) = id.replace(":", "_")

    private fun scanLocal(): Map<String, DownloadInfo> {
        if (!baseDir.exists()) return emptyMap()
        return baseDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "mp3" }
            .associate { f ->
                val id = f.nameWithoutExtension.replace("_", ":")
                id to DownloadInfo(DownloadState.Done, 1f, f.absolutePath)
            }
    }

    companion object {
        private const val TAG_DOWNLOAD = "download"
        private const val TAG_TRACK_PREFIX = "track_"
    }
}
