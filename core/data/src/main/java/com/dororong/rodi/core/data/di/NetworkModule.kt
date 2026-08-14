package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.BuildConfig
import com.dororong.rodi.core.data.mock.MockResponseInterceptor
import com.dororong.rodi.core.data.source.remote.api.AuthApi
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.api.OnboardingApi
import com.dororong.rodi.core.data.source.remote.api.PlaceApi
import com.dororong.rodi.core.data.source.remote.api.PracticeApi
import com.dororong.rodi.core.data.source.remote.api.RecentSearchApi
import com.dororong.rodi.core.data.source.remote.api.ReviewApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// 로그인/토큰 API 서버. Notion "카카오 로그인 API 연동 가이드" 기준.
private const val BASE_URL = "https://api.stillstar.store/api/v1/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            // MockResponseRegistry.enabled가 true고 경로가 등록돼 있을 때만 가로챈다. 서버에
            // 아직 없는 API를 화면에서 미리 확인할 때 쓴다 — 릴리스에는 아예 붙지 않는다.
            builder.addInterceptor(MockResponseInterceptor())
        }
        return builder
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMemberApi(retrofit: Retrofit): MemberApi = retrofit.create(MemberApi::class.java)

    @Provides
    @Singleton
    fun provideOnboardingApi(retrofit: Retrofit): OnboardingApi = retrofit.create(OnboardingApi::class.java)

    @Provides
    @Singleton
    fun providePlaceApi(retrofit: Retrofit): PlaceApi = retrofit.create(PlaceApi::class.java)

    @Provides
    @Singleton
    fun providePracticeApi(retrofit: Retrofit): PracticeApi = retrofit.create(PracticeApi::class.java)

    @Provides
    @Singleton
    fun provideRecentSearchApi(retrofit: Retrofit): RecentSearchApi = retrofit.create(RecentSearchApi::class.java)

    @Provides
    @Singleton
    fun provideReviewApi(retrofit: Retrofit): ReviewApi = retrofit.create(ReviewApi::class.java)
}
