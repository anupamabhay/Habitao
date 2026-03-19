package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.util.randomUUID
import kotlinx.datetime.Clock

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["routineId", "stepOrder"]),
    ],
)
data class RoutineStepEntity(
    @PrimaryKey
    val id: String = randomUUID(),
    val routineId: String,
    val stepOrder: Int,
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null,
)
