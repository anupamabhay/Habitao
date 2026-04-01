package com.habitao.app

object QuickActionIntentParser {
    const val ActionAddTask = "com.habitao.app.shortcut.ADD_TASK"
    const val ActionAddHabit = "com.habitao.app.shortcut.ADD_HABIT"
    const val ActionAddRoutine = "com.habitao.app.shortcut.ADD_ROUTINE"
    const val ActionGlobalSearch = "com.habitao.app.shortcut.GLOBAL_SEARCH"

    fun toRoute(action: String?): String? {
        return when (action) {
            ActionAddTask -> QuickActionRoute.AddTask
            ActionAddHabit -> QuickActionRoute.AddHabit
            ActionAddRoutine -> QuickActionRoute.AddRoutine
            ActionGlobalSearch -> QuickActionRoute.GlobalSearch
            else -> null
        }
    }
}

