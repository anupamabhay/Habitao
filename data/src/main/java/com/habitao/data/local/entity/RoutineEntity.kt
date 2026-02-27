package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.SyncStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"]),
        Index(value = ["nextScheduledDate"])
    ]
)
data class RoutineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val repeatPattern: RepeatPattern,
    val repeatDays: List<DayOfWeek>? = null,
    val customInterval: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextScheduledDate: LocalDate,
    val completionThreshold: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null
)
