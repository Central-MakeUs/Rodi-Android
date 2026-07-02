package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.CourseRepository
import com.dororong.rodi.core.data.CourseRepositoryImpl
import com.dororong.rodi.core.data.EntryRepository
import com.dororong.rodi.core.data.EntryRepositoryImpl
import com.dororong.rodi.core.data.navi.NaviPreferenceRepository
import com.dororong.rodi.core.data.navi.NaviPreferenceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    abstract fun bindNaviPreferenceRepository(impl: NaviPreferenceRepositoryImpl): NaviPreferenceRepository

    @Binds
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository
}
