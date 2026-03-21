package com.habitao.domain.model

import com.habitao.domain.util.randomUUID
import kotlinx.datetime.Clock

data class PomodoroSession(
    val id: String = randomUUID(),
    val sessionType: PomodoroType,
    val workDurationSeconds: Int = 1500,
    val breakDurationSeconds: Int = 300,
    val linkedTaskId: String? = null,
    val linkedHabitId: String? = null,
    val startedAt: Long,
    val completedAt: Long? = null,
    val wasInterrupted: Boolean = false,
    val actualDurationSeconds: Int? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
)

enum class PomodoroType {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
}
