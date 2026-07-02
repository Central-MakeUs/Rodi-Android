package com.dororong.rodi.core.data.db

import androidx.room.Dao
import androidx.room.Query

/**
 * [SchemaPlaceholderEntity]와 함께 첫 실제 DAO 추가 시 삭제한다.
 */
@Dao
interface SchemaPlaceholderDao {
    @Query("SELECT COUNT(*) FROM schema_placeholder")
    suspend fun count(): Int
}
