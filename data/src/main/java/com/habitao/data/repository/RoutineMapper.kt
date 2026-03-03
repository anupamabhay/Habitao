package com.habitao.data.repository

import com.habitao.data.local.entity.RoutineEntity
import com.habitao.data.local.entity.RoutineLogEntity
import com.habitao.data.local.entity.RoutineStepEntity
import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.RoutineStep

fun RoutineEntity.toDomainModel(): Routine {
    return Routine(
        id = id,
        title = title,
        description = description,
        icon = icon,
        color = color,
        repeatPattern = repeatPattern,
        repeatDays = repeatDays,
        customInterval = customInterval,
        startDate = startDate,
        endDate = endDate,
        nextScheduledDate = nextScheduledDate,
        completionThreshold = completionThreshold,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        sortOrder = sortOrder,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        deletedAt = deletedAt,
    )
}

fun Routine.toEntity(): RoutineEntity {
    return RoutineEntity(
        id = id,
        title = title,
        description = description,
        icon = icon,
        color = color,
        repeatPattern = repeatPattern,
        repeatDays = repeatDays,
        customInterval = customInterval,
        startDate = startDate,
        endDate = endDate,
        nextScheduledDate = nextScheduledDate,
        completionThreshold = completionThreshold,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = isArchived,
        sortOrder = sortOrder,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        deletedAt = deletedAt,
    )
}

fun RoutineStepEntity.toDomainModel(): RoutineStep {
    return RoutineStep(
        id = id,
        routineId = routineId,
        stepOrder = stepOrder,
        title = title,
        description = description,
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
        deletedAt = deletedAt,
    )
}

fun RoutineStep.toEntity(): RoutineStepEntity {
    return RoutineStepEntity(
        id = id,
        routineId = routineId,
        stepOrder = stepOrder,
        title = title,
        description = description,
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
        deletedAt = deletedAt,
    )
}

fun RoutineLogEntity.toDomainModel(): RoutineLog {
    return RoutineLog(
        id = id,
        routineId = routineId,
        date = date,
        completedStepIds = completedStepIds,
        totalSteps = totalSteps,
        completionPercentage = completionPercentage,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        syncStatus = syncStatus,
        deletedAt = deletedAt,
    )
}

fun RoutineLog.toEntity(): RoutineLogEntity {
    return RoutineLogEntity(
        id = id,
        routineId = routineId,
        date = date,
        completedStepIds = completedStepIds,
        totalSteps = totalSteps,
        completionPercentage = completionPercentage,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        syncStatus = syncStatus,
        deletedAt = deletedAt,
    )
}
