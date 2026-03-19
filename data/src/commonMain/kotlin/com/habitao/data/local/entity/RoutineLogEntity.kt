package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.util.randomUUID
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

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
    val id: String = randomUUID(),
    val routineId: String,
    val date: LocalDate,
    val completedStepIds: List<String> = emptyList(),
    val totalSteps: Int,
    val completionPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val completedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null,
)
