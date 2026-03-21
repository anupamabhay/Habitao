package com.habitao.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Task(
    val id: String,
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

enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
