package com.habitao.domain.model

data class RoutineStep(
    val id: String,
    val routineId: String,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null
)
