package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.util.randomUUID
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"]),
        Index(value = ["nextScheduledDate"]),
    ],
)
data class RoutineEntity(
    @PrimaryKey
    val id: String = randomUUID(),
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
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null,
)
