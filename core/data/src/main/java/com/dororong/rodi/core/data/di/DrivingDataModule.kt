package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.repository.DrivingSessionRepositoryImpl
import com.dororong.rodi.core.domain.repository.DrivingSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DrivingDataModule {
    @Binds
    @Singleton
    abstract fun bindDrivingSessionRepository(
        impl: DrivingSessionRepositoryImpl,
    ): DrivingSessionRepository
}
