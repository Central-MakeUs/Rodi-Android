package com.dororong.rodi.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 로컬 DB 스켈레톤. [SchemaPlaceholderEntity]는 실제 엔티티가 생기기 전까지의 자리표시자로,
 * 첫 실제 엔티티 추가 시 제거하고 entities 목록을 교체한다.
 */
@Database(entities = [SchemaPlaceholderEntity::class], version = 1, exportSchema = false)
abstract class RodiDatabase : RoomDatabase() {
    abstract fun schemaPlaceholderDao(): SchemaPlaceholderDao
}
