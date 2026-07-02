package com.dororong.rodi.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 실제 엔티티가 생기기 전까지의 자리표시자이며, 첫 실제 엔티티 추가 시 삭제한다.
 */
@Entity(tableName = "schema_placeholder")
data class SchemaPlaceholderEntity(
    @PrimaryKey val id: Int,
)
