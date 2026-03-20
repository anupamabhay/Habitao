package com.habitao.feature.pomodoro.preferences

import kotlinx.coroutines.flow.Flow

interface PomodoroPreferencesSource {
    var workDurationMinutes: Int
    var shortBreakDurationMinutes: Int
    var longBreakDurationMinutes: Int
    var sessionsBeforeLongBreak: Int
    var totalSessions: Int
    var autoStartNextPomo: Boolean
    var autoStartBreak: Boolean
    var vibrateEnabled: Boolean
    var vibrateDurationSeconds: Int
    var pomoEndingSoundUri: String
    var breakEndingSoundUri: String

    fun getTodaysRounds(): Int
    fun incrementRound()

    /** Emits whenever any preference changes. */
    fun observeChanges(): Flow<Unit>
}
