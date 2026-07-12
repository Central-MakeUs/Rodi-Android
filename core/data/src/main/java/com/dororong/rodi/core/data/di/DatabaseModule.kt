package com.dororong.rodi.core.data.di

import android.content.Context
import androidx.room.Room
import com.dororong.rodi.core.data.source.local.database.RodiDatabase
import com.dororong.rodi.core.data.source.local.database.dao.SchemaPlaceholderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "rodi.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRodiDatabase(@ApplicationContext context: Context): RodiDatabase =
        Room.databaseBuilder(context, RodiDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideSchemaPlaceholderDao(database: RodiDatabase): SchemaPlaceholderDao =
        database.schemaPlaceholderDao()
}
