package com.habitao.data.repository

import com.habitao.data.local.entity.TaskEntity
import com.habitao.domain.model.Task

fun TaskEntity.toDomainModel(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        parentTaskId = parentTaskId,
        projectId = projectId,
        dueDate = dueDate,
        dueTime = dueTime,
        isRecurring = isRecurring,
        repeatPattern = repeatPattern,
        repeatDays = repeatDays,
        priority = priority,
        tags = tags,
        isCompleted = isCompleted,
        completedAt = completedAt,
        reminderEnabled = reminderEnabled,
        reminderMinutesBefore = reminderMinutesBefore,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sortOrder = sortOrder,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        deletedAt = deletedAt,
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        parentTaskId = parentTaskId,
        projectId = projectId,
        dueDate = dueDate,
        dueTime = dueTime,
        isRecurring = isRecurring,
        repeatPattern = repeatPattern,
        repeatDays = repeatDays,
        priority = priority,
        tags = tags,
        isCompleted = isCompleted,
        completedAt = completedAt,
        reminderEnabled = reminderEnabled,
        reminderMinutesBefore = reminderMinutesBefore,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sortOrder = sortOrder,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        deletedAt = deletedAt,
    )
}
