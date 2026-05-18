package dev.pdv.yamulite.data.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import dev.pdv.yamulite.data.music.StreamUrlResolver
import dev.pdv.yamulite.data.settings.CodecPreference
import dev.pdv.yamulite.data.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolvingDataSourceFactory @Inject constructor(
    private val resolver: StreamUrlResolver,
    private val settings: SettingsStore,
    private val downloads: DownloadManager,
) : DataSource.Factory {

    // ExoPlayer calls open() on a background loader thread — runBlocking is safe here
    private val httpFactory = DefaultHttpDataSource.Factory()

    override fun createDataSource(): DataSource = ResolvingDataSource()

    private inner class ResolvingDataSource : DataSource {
        private var delegate: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            delegate?.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            if (dataSpec.uri.scheme != "yamulite") {
                val ds = httpFactory.createDataSource()
                delegate = ds
                return ds.open(dataSpec)
            }

            val trackId = dataSpec.uri.lastPathSegment
                ?: throw IOException("Missing track id in ${dataSpec.uri}")

            val localPath = downloads.localPath(trackId)
            val realUri = if (localPath != null) {
                Uri.fromFile(File(localPath))
            } else {
                val q = runBlocking { settings.currentQuality() }
                val c = runBlocking { settings.currentCodec() }
                val resolved = runBlocking { resolver.resolve(trackId, q.bitrate, c) }
                    // If preferred codec failed and it wasn't already mp3, retry with mp3
                    ?: if (c != CodecPreference.Mp3Only)
                        runBlocking { resolver.resolve(trackId, q.bitrate, CodecPreference.Mp3Only) }
                    else null
                val url = resolved?.url
                    ?: throw IOException("Could not resolve stream URL for track $trackId")
                Uri.parse(url)
            }

            val realSpec = dataSpec.buildUpon().setUri(realUri).build()
            val ds: DataSource = if (localPath != null) FileDataSource() else httpFactory.createDataSource()
            delegate = ds
            return ds.open(realSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

        override fun getUri(): Uri? = delegate?.uri

        override fun close() {
            delegate?.close()
            delegate = null
        }
    }
}
