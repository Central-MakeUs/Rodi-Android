package com.dororong.rodi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object ServerConfigModule {
    @Provides
    @Named("rodiBaseUrl")
    fun provideRodiBaseUrl(): String = BuildConfig.RODI_BASE_URL
}
