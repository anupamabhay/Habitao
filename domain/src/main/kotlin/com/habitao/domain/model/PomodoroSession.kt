package com.habitao.domain.model

import java.util.UUID

data class PomodoroSession(
    val id: String = UUID.randomUUID().toString(),
    val sessionType: PomodoroType,
    val workDurationSeconds: Int = 1500,
    val breakDurationSeconds: Int = 300,
    val startedAt: Long,
    val completedAt: Long? = null,
    val wasInterrupted: Boolean = false,
    val actualDurationSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class PomodoroType {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
}
