package com.habitao.feature.pomodoro.preferences

import android.content.SharedPreferences
import com.habitao.feature.pomodoro.service.PomodoroPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Wraps the Android [PomodoroPreferences] to implement the KMP [PomodoroPreferencesSource]. */
class AndroidPomodoroPreferences(
    private val prefs: PomodoroPreferences,
) : PomodoroPreferencesSource {
    override var workDurationMinutes: Int
        get() = prefs.workDurationMinutes
        set(value) { prefs.workDurationMinutes = value }

    override var shortBreakDurationMinutes: Int
        get() = prefs.shortBreakDurationMinutes
        set(value) { prefs.shortBreakDurationMinutes = value }

    override var longBreakDurationMinutes: Int
        get() = prefs.longBreakDurationMinutes
        set(value) { prefs.longBreakDurationMinutes = value }

    override var sessionsBeforeLongBreak: Int
        get() = prefs.sessionsBeforeLongBreak
        set(value) { prefs.sessionsBeforeLongBreak = value }

    override var totalSessions: Int
        get() = prefs.totalSessions
        set(value) { prefs.totalSessions = value }

    override var autoStartNextPomo: Boolean
        get() = prefs.autoStartNextPomo
        set(value) { prefs.autoStartNextPomo = value }

    override var autoStartBreak: Boolean
        get() = prefs.autoStartBreak
        set(value) { prefs.autoStartBreak = value }

    override var vibrateEnabled: Boolean
        get() = prefs.vibrateEnabled
        set(value) { prefs.vibrateEnabled = value }

    override var vibrateDurationSeconds: Int
        get() = prefs.vibrateDurationSeconds
        set(value) { prefs.vibrateDurationSeconds = value }

    override var pomoEndingSoundUri: String
        get() = prefs.pomoEndingSoundUri
        set(value) { prefs.pomoEndingSoundUri = value }

    override var breakEndingSoundUri: String
        get() = prefs.breakEndingSoundUri
        set(value) { prefs.breakEndingSoundUri = value }

    override fun getTodaysRounds(): Int = prefs.getTodaysRounds()

    override fun incrementRound() = prefs.incrementRound()

    override fun observeChanges(): Flow<Unit> =
        callbackFlow {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    trySend(Unit)
                }
            prefs.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(Unit) // emit initial value
            awaitClose {
                prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
}
