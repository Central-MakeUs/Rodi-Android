package com.dororong.rodi.core.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dororong.rodi.core.data.source.local.database.dao.PlaceCacheDao
import com.dororong.rodi.core.data.source.local.database.entity.PlaceCoordinateEntity
import com.dororong.rodi.core.data.source.local.database.entity.PlaceSummaryEntity

/**
 * 지도 장소 좌표와 목록을 서버 갱신 전에도 즉시 표시하기 위한 로컬 캐시다.
 */
@Database(
    entities = [PlaceCoordinateEntity::class, PlaceSummaryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class RodiDatabase : RoomDatabase() {
    abstract fun placeCacheDao(): PlaceCacheDao
}
