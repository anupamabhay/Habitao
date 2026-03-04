package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.SyncStatus
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "routine_logs",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["routineId", "date"], unique = true),
        Index(value = ["date"]),
    ],
)
data class RoutineLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val date: LocalDate,
    val completedStepIds: List<String> = emptyList(),
    val totalSteps: Int,
    val completionPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null,
)
