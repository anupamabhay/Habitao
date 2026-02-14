package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a habit log (daily progress)
 * Based on data model defined in docs/02-DATA-MODEL-SCHEMA.md
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class HabitLogEntity(
    @PrimaryKey
    val id: String,
    
    val habitId: String,
    val date: Long, // Epoch millis for the day
    
    // Progress Tracking
    val currentCount: Int = 0,
    val goalCount: Int,
    val isCompleted: Boolean = false,
    
    // Duration tracking (if trackingType = DURATION)
    val durationSeconds: Int? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
