package dev.pdv.yamulite.data.music

import dev.pdv.yamulite.data.auth.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.tokenFlow.first() }
        val req = chain.request().newBuilder()
            .header("Accept-Language", "ru")
            .header("User-Agent", "YaMuLite/0.1 (Android)")
            .apply { if (token != null) header("Authorization", "OAuth $token") }
            .build()
        return chain.proceed(req)
    }
}
