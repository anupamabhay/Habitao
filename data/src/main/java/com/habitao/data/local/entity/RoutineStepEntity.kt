package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.SyncStatus
import java.util.UUID

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId", "stepOrder"])
    ]
)
data class RoutineStepEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
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
