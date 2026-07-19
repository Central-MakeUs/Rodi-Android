package com.dororong.rodi.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dororong.rodi.core.data.source.local.database.RodiDatabase
import com.dororong.rodi.core.data.source.local.database.dao.PlaceCacheDao
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
        Room.databaseBuilder(context, RodiDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePlaceCacheDao(database: RodiDatabase): PlaceCacheDao = database.placeCacheDao()
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS schema_placeholder")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS place_coordinates (id INTEGER NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, address TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, PRIMARY KEY(id))",
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS place_summaries (id INTEGER NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, address TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, practiceTypes TEXT NOT NULL, description TEXT, distanceMeters INTEGER, capacity INTEGER, openTime TEXT, PRIMARY KEY(id))",
        )
    }
}
