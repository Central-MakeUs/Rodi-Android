package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.repository.AuthRepositoryImpl
import com.dororong.rodi.core.data.repository.CourseRepositoryImpl
import com.dororong.rodi.core.data.repository.EntryRepositoryImpl
import com.dororong.rodi.core.data.repository.OnboardingRepositoryImpl
import com.dororong.rodi.core.data.repository.CachedPlaceRepository
import com.dororong.rodi.core.data.repository.NaviPreferenceRepositoryImpl
import com.dororong.rodi.core.data.repository.MemberRepositoryImpl
import com.dororong.rodi.core.data.repository.RecentSearchRepositoryImpl
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.CourseRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.NaviPreferenceRepository
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    abstract fun bindNaviPreferenceRepository(impl: NaviPreferenceRepositoryImpl): NaviPreferenceRepository

    @Binds
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository

    @Binds
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository

    @Binds
    abstract fun bindPlaceRepository(impl: CachedPlaceRepository): PlaceRepository

    @Binds
    abstract fun bindRecentSearchRepository(impl: RecentSearchRepositoryImpl): RecentSearchRepository

}
