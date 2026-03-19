package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.model.TaskPriority
import com.habitao.domain.util.randomUUID
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["priority"]),
        Index(value = ["isCompleted"]),
        Index(value = ["projectId"]),
    ],
)
data class TaskEntity(
    @PrimaryKey
    val id: String = randomUUID(),
    val title: String,
    val description: String? = null,
    val parentTaskId: String? = null,
    val projectId: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val isRecurring: Boolean = false,
    val repeatPattern: RepeatPattern? = null,
    val repeatDays: List<DayOfWeek>? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val tags: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 60,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val sortOrder: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null,
)
