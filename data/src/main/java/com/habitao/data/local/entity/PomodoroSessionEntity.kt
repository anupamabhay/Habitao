package com.habitao.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.habitao.domain.model.PomodoroSession
import com.habitao.domain.model.PomodoroType

@Entity(
    tableName = "pomodoro_sessions",
    indices = [
        Index(value = ["startedAt"]),
    ],
)
data class PomodoroSessionEntity(
    @PrimaryKey
    val id: String,
    val sessionType: String,
    val workDurationSeconds: Int,
    val breakDurationSeconds: Int,
    val startedAt: Long,
    val completedAt: Long? = null,
    val wasInterrupted: Boolean = false,
    val actualDurationSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomain(): PomodoroSession {
        return PomodoroSession(
            id = id,
            sessionType = PomodoroType.valueOf(sessionType),
            workDurationSeconds = workDurationSeconds,
            breakDurationSeconds = breakDurationSeconds,
            startedAt = startedAt,
            completedAt = completedAt,
            wasInterrupted = wasInterrupted,
            actualDurationSeconds = actualDurationSeconds,
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomain(session: PomodoroSession): PomodoroSessionEntity {
            return PomodoroSessionEntity(
                id = session.id,
                sessionType = session.sessionType.name,
                workDurationSeconds = session.workDurationSeconds,
                breakDurationSeconds = session.breakDurationSeconds,
                startedAt = session.startedAt,
                completedAt = session.completedAt,
                wasInterrupted = session.wasInterrupted,
                actualDurationSeconds = session.actualDurationSeconds,
                createdAt = session.createdAt,
            )
        }
    }
}
