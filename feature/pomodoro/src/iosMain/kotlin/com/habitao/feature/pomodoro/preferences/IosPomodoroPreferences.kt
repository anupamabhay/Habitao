package com.habitao.feature.pomodoro.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * iOS implementation of [PomodoroPreferencesSource] backed by in-memory state.
 * Persists for the app's lifetime; for persistent storage, back this with NSUserDefaults.
 */
class IosPomodoroPreferences : PomodoroPreferencesSource {
    private val _changes = MutableStateFlow(0)

    override var workDurationMinutes: Int = DEFAULT_WORK_MINUTES
        set(value) { field = value; _changes.value++ }

    override var shortBreakDurationMinutes: Int = DEFAULT_SHORT_BREAK_MINUTES
        set(value) { field = value; _changes.value++ }

    override var longBreakDurationMinutes: Int = DEFAULT_LONG_BREAK_MINUTES
        set(value) { field = value; _changes.value++ }

    override var sessionsBeforeLongBreak: Int = DEFAULT_SESSIONS_BEFORE_LONG_BREAK
        set(value) { field = value; _changes.value++ }

    override var totalSessions: Int = DEFAULT_TOTAL_SESSIONS
        set(value) { field = value; _changes.value++ }

    override var autoStartNextPomo: Boolean = false
        set(value) { field = value; _changes.value++ }

    override var autoStartBreak: Boolean = false
        set(value) { field = value; _changes.value++ }

    override var vibrateEnabled: Boolean = true
        set(value) { field = value; _changes.value++ }

    override var vibrateDurationSeconds: Int = DEFAULT_VIBRATE_DURATION_SECONDS
        set(value) { field = value; _changes.value++ }

    override var pomoEndingSoundUri: String = ""
        set(value) { field = value; _changes.value++ }

    override var breakEndingSoundUri: String = ""
        set(value) { field = value; _changes.value++ }

    private var completedRoundsToday: Int = 0
    private var lastRoundResetDate: String = ""

    override fun getTodaysRounds(): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return if (lastRoundResetDate != today) 0 else completedRoundsToday
    }

    override fun incrementRound() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        if (lastRoundResetDate != today) {
            completedRoundsToday = 0
            lastRoundResetDate = today
        }
        completedRoundsToday++
        _changes.value++
    }

    override fun observeChanges(): Flow<Unit> = _changes.asStateFlow().map { }

    companion object {
        const val DEFAULT_WORK_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
        const val DEFAULT_TOTAL_SESSIONS = 5
        const val DEFAULT_VIBRATE_DURATION_SECONDS = 2
    }
}
