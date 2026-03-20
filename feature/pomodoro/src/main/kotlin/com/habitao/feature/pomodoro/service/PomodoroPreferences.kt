package com.habitao.feature.pomodoro.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PomodoroPreferences
    constructor(
        context: Context,
    ) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var workDurationMinutes: Int
            get() = sharedPreferences.getInt(KEY_WORK_MINUTES, DEFAULT_WORK_MINUTES)
            set(value) = sharedPreferences.edit().putInt(KEY_WORK_MINUTES, value).apply()

        var shortBreakDurationMinutes: Int
            get() = sharedPreferences.getInt(KEY_SHORT_BREAK_MINUTES, DEFAULT_SHORT_BREAK_MINUTES)
            set(value) = sharedPreferences.edit().putInt(KEY_SHORT_BREAK_MINUTES, value).apply()

        var longBreakDurationMinutes: Int
            get() = sharedPreferences.getInt(KEY_LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES)
            set(value) = sharedPreferences.edit().putInt(KEY_LONG_BREAK_MINUTES, value).apply()

        var sessionsBeforeLongBreak: Int
            get() = sharedPreferences.getInt(KEY_SESSIONS_BEFORE_LONG_BREAK, DEFAULT_SESSIONS_BEFORE_LONG_BREAK)
            set(value) = sharedPreferences.edit().putInt(KEY_SESSIONS_BEFORE_LONG_BREAK, value).apply()

        var totalSessions: Int
            get() = sharedPreferences.getInt(KEY_TOTAL_SESSIONS, DEFAULT_TOTAL_SESSIONS)
            set(value) = sharedPreferences.edit().putInt(KEY_TOTAL_SESSIONS, value).apply()

        var autoStartNextPomo: Boolean
            get() = sharedPreferences.getBoolean(KEY_AUTO_START_NEXT_POMO, DEFAULT_AUTO_START_NEXT_POMO)
            set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_START_NEXT_POMO, value).apply()

        var autoStartBreak: Boolean
            get() = sharedPreferences.getBoolean(KEY_AUTO_START_BREAK, DEFAULT_AUTO_START_BREAK)
            set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_START_BREAK, value).apply()

        var pomoEndingSoundUri: String
            get() =
                sharedPreferences.getString(KEY_POMO_ENDING_SOUND_URI, DEFAULT_POMO_ENDING_SOUND_URI)
                    ?: DEFAULT_POMO_ENDING_SOUND_URI
            set(value) = sharedPreferences.edit().putString(KEY_POMO_ENDING_SOUND_URI, value).apply()

        var breakEndingSoundUri: String
            get() =
                sharedPreferences.getString(KEY_BREAK_ENDING_SOUND_URI, DEFAULT_BREAK_ENDING_SOUND_URI)
                    ?: DEFAULT_BREAK_ENDING_SOUND_URI
            set(value) = sharedPreferences.edit().putString(KEY_BREAK_ENDING_SOUND_URI, value).apply()

        var vibrateEnabled: Boolean
            get() = sharedPreferences.getBoolean(KEY_VIBRATE_ENABLED, DEFAULT_VIBRATE_ENABLED)
            set(value) = sharedPreferences.edit().putBoolean(KEY_VIBRATE_ENABLED, value).apply()

        var vibrateDurationSeconds: Int
            get() = sharedPreferences.getInt(KEY_VIBRATE_DURATION_SECONDS, DEFAULT_VIBRATE_DURATION_SECONDS)
            set(value) = sharedPreferences.edit().putInt(KEY_VIBRATE_DURATION_SECONDS, value).apply()

        var completedRoundsToday: Int
            get() = sharedPreferences.getInt(KEY_COMPLETED_ROUNDS_TODAY, DEFAULT_COMPLETED_ROUNDS_TODAY)
            set(value) = sharedPreferences.edit().putInt(KEY_COMPLETED_ROUNDS_TODAY, value).apply()

        var lastRoundResetDate: String
            get() =
                sharedPreferences.getString(KEY_LAST_ROUND_RESET_DATE, DEFAULT_LAST_ROUND_RESET_DATE)
                    ?: DEFAULT_LAST_ROUND_RESET_DATE
            set(value) = sharedPreferences.edit().putString(KEY_LAST_ROUND_RESET_DATE, value).apply()

        fun incrementRound() {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            if (lastRoundResetDate != today) {
                completedRoundsToday = 0
                lastRoundResetDate = today
            }
            completedRoundsToday = completedRoundsToday + 1
        }

        fun getTodaysRounds(): Int {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            return if (lastRoundResetDate != today) 0 else completedRoundsToday
        }

        companion object {
            private const val PREFS_NAME = "pomodoro_timer_prefs"
            private const val KEY_WORK_MINUTES = "pomo_work_minutes"
            private const val KEY_SHORT_BREAK_MINUTES = "pomo_short_break_minutes"
            private const val KEY_LONG_BREAK_MINUTES = "pomo_long_break_minutes"
            private const val KEY_SESSIONS_BEFORE_LONG_BREAK = "pomo_sessions_before_long_break"
            private const val KEY_TOTAL_SESSIONS = "pomo_total_sessions"
            private const val KEY_AUTO_START_NEXT_POMO = "pomo_auto_start_next_pomo"
            private const val KEY_AUTO_START_BREAK = "pomo_auto_start_break"
            private const val KEY_POMO_ENDING_SOUND_URI = "pomo_ending_sound_uri"
            private const val KEY_BREAK_ENDING_SOUND_URI = "break_ending_sound_uri"
            private const val KEY_VIBRATE_ENABLED = "pomo_vibrate_enabled"
            private const val KEY_VIBRATE_DURATION_SECONDS = "pomo_vibrate_duration_seconds"
            private const val KEY_COMPLETED_ROUNDS_TODAY = "pomo_completed_rounds_today"
            private const val KEY_LAST_ROUND_RESET_DATE = "pomo_last_round_reset_date"

            const val DEFAULT_WORK_MINUTES = 25
            const val DEFAULT_SHORT_BREAK_MINUTES = 5
            const val DEFAULT_LONG_BREAK_MINUTES = 15
            const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
            const val DEFAULT_TOTAL_SESSIONS = 5
            const val DEFAULT_AUTO_START_NEXT_POMO = false
            const val DEFAULT_AUTO_START_BREAK = false
            const val DEFAULT_POMO_ENDING_SOUND_URI = ""
            const val DEFAULT_BREAK_ENDING_SOUND_URI = ""
            const val DEFAULT_VIBRATE_ENABLED = true
            const val DEFAULT_VIBRATE_DURATION_SECONDS = 2
            const val DEFAULT_COMPLETED_ROUNDS_TODAY = 0
            const val DEFAULT_LAST_ROUND_RESET_DATE = ""
        }
    }
