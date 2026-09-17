package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.BuildConfig
import com.dororong.rodi.core.data.mock.MockResponseInterceptor
import com.dororong.rodi.core.data.source.remote.api.AuthApi
import com.dororong.rodi.core.data.source.remote.api.CourseApi
import com.dororong.rodi.core.data.source.remote.api.KakaoLocalApi
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.api.OnboardingApi
import com.dororong.rodi.core.data.source.remote.api.PlaceApi
import com.dororong.rodi.core.data.source.remote.api.PracticeApi
import com.dororong.rodi.core.data.source.remote.api.RecentSearchApi
import com.dororong.rodi.core.data.source.remote.api.ReviewApi
import com.dororong.rodi.core.data.source.remote.network.AuthHeaderInterceptor
import com.dororong.rodi.core.data.source.remote.network.TokenAuthenticator
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
import javax.inject.Named
import javax.inject.Singleton

// 로그인/토큰 API 서버. Notion "카카오 로그인 API 연동 가이드" 기준.
private const val BASE_URL = "https://api.stillstar.store/api/v1/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 인증이 붙지 않은 기본 클라이언트. 카카오 로컬 API와 로그인·재발급(AuthApi)이 쓴다.
     * 여기에 인증을 붙이면 카카오 요청에도 Bearer가 실리고, 재발급 요청이 자기 자신을 다시 부른다.
     */
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

    /** 보호 API 전용. 토큰 주입(Interceptor)과 401 재발급(Authenticator)이 여기에만 붙는다. */
    @Provides
    @Singleton
    @Named("authenticated")
    fun provideAuthenticatedOkHttpClient(
        okHttpClient: OkHttpClient,
        authHeaderInterceptor: AuthHeaderInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = okHttpClient.newBuilder()
        .addInterceptor(authHeaderInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    /** 로그인·재발급용. 토큰을 본문으로 주고받으므로 인증을 붙이지 않는다. */
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
    @Named("authenticated")
    fun provideAuthenticatedRetrofit(
        @Named("authenticated") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @Named("kakaoLocal")
    fun provideKakaoLocalRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCourseApi(@Named("authenticated") retrofit: Retrofit): CourseApi = retrofit.create(CourseApi::class.java)

    @Provides
    @Singleton
    fun provideKakaoLocalApi(@Named("kakaoLocal") retrofit: Retrofit): KakaoLocalApi =
        retrofit.create(KakaoLocalApi::class.java)

    @Provides
    @Singleton
    fun provideMemberApi(@Named("authenticated") retrofit: Retrofit): MemberApi = retrofit.create(MemberApi::class.java)

    @Provides
    @Singleton
    fun provideOnboardingApi(@Named("authenticated") retrofit: Retrofit): OnboardingApi = retrofit.create(OnboardingApi::class.java)

    @Provides
    @Singleton
    fun providePlaceApi(@Named("authenticated") retrofit: Retrofit): PlaceApi = retrofit.create(PlaceApi::class.java)

    @Provides
    @Singleton
    fun providePracticeApi(@Named("authenticated") retrofit: Retrofit): PracticeApi = retrofit.create(PracticeApi::class.java)

    @Provides
    @Singleton
    fun provideRecentSearchApi(@Named("authenticated") retrofit: Retrofit): RecentSearchApi = retrofit.create(RecentSearchApi::class.java)

    @Provides
    @Singleton
    fun provideReviewApi(@Named("authenticated") retrofit: Retrofit): ReviewApi = retrofit.create(ReviewApi::class.java)
}
