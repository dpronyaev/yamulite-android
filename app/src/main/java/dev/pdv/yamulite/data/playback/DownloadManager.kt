package dev.pdv.yamulite.data.playback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pdv.yamulite.data.music.StreamUrlResolver
import dev.pdv.yamulite.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
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
    @ApplicationContext context: Context,
    private val resolver: StreamUrlResolver,
    private val settings: SettingsStore,
    private val okHttp: OkHttpClient,
) {
    private val baseDir: File = File(context.filesDir, "tracks").also { it.mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, DownloadInfo>>(scanLocal())
    val downloads: StateFlow<Map<String, DownloadInfo>> = _downloads.asStateFlow()

    fun localPath(trackId: String): String? {
        val f = fileFor(trackId)
        return if (f.exists()) f.absolutePath else null
    }

    fun download(trackId: String) {
        val current = _downloads.value[trackId]?.state
        if (current == DownloadState.Downloading || current == DownloadState.Done) return
        _downloads.update { it + (trackId to DownloadInfo(DownloadState.Downloading)) }
        scope.launch {
            runCatching {
                val q = settings.currentQuality()
                val url = resolver.resolve(trackId, q.bitrate) ?: error("Не удалось получить ссылку")
                val tmp = File(baseDir, "${safeName(trackId)}.mp3.part")
                val dest = fileFor(trackId)
                okHttp.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    val body = resp.body ?: error("Пустой ответ")
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        tmp.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var soFar = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n == -1) break
                                output.write(buf, 0, n)
                                soFar += n
                                if (total > 0) {
                                    val progress = (soFar.toFloat() / total).coerceIn(0f, 1f)
                                    _downloads.update {
                                        it + (trackId to DownloadInfo(DownloadState.Downloading, progress))
                                    }
                                }
                            }
                        }
                    }
                }
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                _downloads.update {
                    it + (trackId to DownloadInfo(DownloadState.Done, 1f, dest.absolutePath))
                }
            }.onFailure { e ->
                _downloads.update {
                    it + (trackId to DownloadInfo(DownloadState.Failed, error = e.message))
                }
            }
        }
    }

    fun delete(trackId: String) {
        fileFor(trackId).delete()
        File(baseDir, "${safeName(trackId)}.mp3.part").delete()
        _downloads.update { it - trackId }
    }

    private fun fileFor(trackId: String) = File(baseDir, "${safeName(trackId)}.mp3")
    private fun safeName(trackId: String) = trackId.replace(":", "_")

    private fun scanLocal(): Map<String, DownloadInfo> {
        if (!baseDir.exists()) return emptyMap()
        return baseDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "mp3" }
            .associate { f ->
                val id = f.nameWithoutExtension.replace("_", ":")
                id to DownloadInfo(DownloadState.Done, 1f, f.absolutePath)
            }
    }
}
