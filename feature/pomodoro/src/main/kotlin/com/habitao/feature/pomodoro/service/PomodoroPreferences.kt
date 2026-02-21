package com.habitao.feature.pomodoro.service

import android.content.Context
import android.content.SharedPreferences

class PomodoroPreferences(context: Context) {
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

    companion object {
        private const val PREFS_NAME = "pomodoro_timer_prefs"
        private const val KEY_WORK_MINUTES = "pomo_work_minutes"
        private const val KEY_SHORT_BREAK_MINUTES = "pomo_short_break_minutes"
        private const val KEY_LONG_BREAK_MINUTES = "pomo_long_break_minutes"
        private const val KEY_SESSIONS_BEFORE_LONG_BREAK = "pomo_sessions_before_long_break"

        const val DEFAULT_WORK_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
    }
}
