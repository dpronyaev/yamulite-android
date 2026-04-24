package dev.pdv.yamulite.data.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamUrlResolver @Inject constructor(
    private val api: MusicApi,
    private val okHttp: OkHttpClient,
) {
    suspend fun resolve(trackId: String): String? = withContext(Dispatchers.IO) {
        val options = api.downloadInfo(trackId).result
        val pick = options
            .filter { it.codec.equals("mp3", ignoreCase = true) }
            .maxByOrNull { it.bitrateInKbps }
            ?: options.firstOrNull()
            ?: return@withContext null

        val xml = okHttp.newCall(Request.Builder().url(pick.downloadInfoUrl).build())
            .execute()
            .use { it.body?.string().orEmpty() }
        val info = parseInfoXml(xml) ?: return@withContext null
        signedMp3Url(info)
    }

    private data class InfoFields(val host: String, val path: String, val ts: String, val s: String)

    private fun parseInfoXml(xml: String): InfoFields? {
        fun field(tag: String) = Regex("<$tag>(.+?)</$tag>").find(xml)?.groupValues?.get(1)
        val host = field("host") ?: return null
        val path = field("path") ?: return null
        val ts = field("ts") ?: return null
        val s = field("s") ?: return null
        return InfoFields(host, path, ts, s)
    }

    private fun signedMp3Url(info: InfoFields): String {
        val pathNoSlash = info.path.removePrefix("/")
        val signed = md5Hex(SALT + pathNoSlash + info.s)
        return "https://${info.host}/get-mp3/$signed/${info.ts}${info.path}"
    }

    private fun md5Hex(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val SALT = "XGRlBW9FXlekgbPrRHuSiA"
    }
}
