package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.AuthRepositoryImpl
import com.dororong.rodi.core.data.CourseRepositoryImpl
import com.dororong.rodi.core.data.EntryRepositoryImpl
import com.dororong.rodi.core.data.OnboardingRepositoryImpl
import com.dororong.rodi.core.data.map.LocalSyntheticMapCourseDataSource
import com.dororong.rodi.core.data.map.MapCourseDataSource
import com.dororong.rodi.core.data.navi.NaviPreferenceRepositoryImpl
import com.dororong.rodi.core.domain.AuthRepository
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.EntryRepository
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import com.dororong.rodi.core.domain.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindMapCourseDataSource(impl: LocalSyntheticMapCourseDataSource): MapCourseDataSource

    @Binds
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    abstract fun bindNaviPreferenceRepository(impl: NaviPreferenceRepositoryImpl): NaviPreferenceRepository

    @Binds
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository

    @Binds
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
