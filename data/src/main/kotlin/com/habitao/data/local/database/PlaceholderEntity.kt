package com.habitao.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placeholders")
data class PlaceholderEntity(
    @PrimaryKey val id: String = "",
)
