package dev.pdv.yamulite.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pdv.yamulite.data.music.AuthInterceptor
import dev.pdv.yamulite.data.music.MusicApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MusicRetrofit

@Module
@InstallIn(SingletonComponent::class)
object MusicModule {

    @Provides @Singleton
    @MusicRetrofit
    fun provideMusicRetrofit(
        json: Json,
        authInterceptor: AuthInterceptor,
    ): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.music.yandex.net/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun provideMusicApi(@MusicRetrofit retrofit: Retrofit): MusicApi =
        retrofit.create(MusicApi::class.java)
}
