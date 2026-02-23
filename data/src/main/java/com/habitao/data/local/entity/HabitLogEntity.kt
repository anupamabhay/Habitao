package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Daily progress log entity for a habit on a specific date
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["date"]),
    ],
)
data class HabitLogEntity(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val date: Long, // Epoch millis for start of day
    // Progress Tracking
    val currentValue: Int = 0,
    val targetValue: Int = 1,
    val isCompleted: Boolean = false,
    // Checklist progress (JSON array of completed item IDs)
    val completedChecklistItemsJson: String? = null,
    // Legacy fields for backward compatibility
    val currentCount: Int = 0,
    val goalCount: Int = 1,
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)
