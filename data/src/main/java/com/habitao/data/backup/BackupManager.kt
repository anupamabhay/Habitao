package com.habitao.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.dao.PomodoroSessionDao
import com.habitao.data.local.dao.RoutineDao
import com.habitao.data.local.dao.TaskDao
import com.habitao.data.local.database.HabitaoDatabase
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.data.local.entity.PomodoroSessionEntity
import com.habitao.data.local.entity.RoutineEntity
import com.habitao.data.local.entity.RoutineLogEntity
import com.habitao.data.local.entity.RoutineStepEntity
import com.habitao.data.local.entity.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles full database backup (export) and restore (import) via JSON.
 * All entity fields are serialized using the Room type-converter conventions.
 */
@Singleton
class BackupManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: HabitaoDatabase,
        private val habitDao: HabitDao,
        private val habitLogDao: HabitLogDao,
        private val taskDao: TaskDao,
        private val routineDao: RoutineDao,
        private val pomodoroSessionDao: PomodoroSessionDao,
    ) {
        companion object {
            const val BACKUP_VERSION = 1
            const val MIME_TYPE = "application/json"
            const val DEFAULT_FILENAME = "habitao_backup.json"
        }

        /**
         * Export the entire database to JSON and write it to the given [uri].
         * Returns the number of records exported.
         */
        suspend fun exportToUri(uri: Uri): Result<Int> =
            runCatching {
                val habits = habitDao.getAllHabitsIncludingArchived()
                val habitLogs = habitLogDao.getAllLogs()
                val tasks = taskDao.getAllTasks()
                val routines = routineDao.getAllRoutines()
                val routineSteps = routineDao.getAllRoutineSteps()
                val routineLogs = routineDao.getAllRoutineLogs()
                val pomodoroSessions = pomodoroSessionDao.getAllSessions()

                val json =
                    JSONObject().apply {
                        put("version", BACKUP_VERSION)
                        put("exportedAt", System.currentTimeMillis())
                        put("appVersion", getAppVersion())
                        put("habits", habitsToJson(habits))
                        put("habitLogs", habitLogsToJson(habitLogs))
                        put("tasks", tasksToJson(tasks))
                        put("routines", routinesToJson(routines))
                        put("routineSteps", routineStepsToJson(routineSteps))
                        put("routineLogs", routineLogsToJson(routineLogs))
                        put("pomodoroSessions", pomodoroSessionsToJson(pomodoroSessions))
                    }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toString(2).toByteArray(Charsets.UTF_8))
                } ?: throw Exception("Failed to open output stream")

                habits.size + habitLogs.size + tasks.size + routines.size +
                    routineSteps.size + routineLogs.size + pomodoroSessions.size
            }

        /**
         * Import data from the given [uri], replacing all existing data.
         * Returns the number of records imported.
         */
        suspend fun importFromUri(uri: Uri): Result<Int> =
            runCatching {
                val jsonString =
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw Exception("Failed to open input stream")

                val json = JSONObject(jsonString)
                val version = json.optInt("version", 0)
                if (version < 1 || version > BACKUP_VERSION) {
                    throw Exception("Unsupported backup version: $version (current: $BACKUP_VERSION)")
                }

                val habits = habitsFromJson(json.optJSONArray("habits"))
                val habitLogs = habitLogsFromJson(json.optJSONArray("habitLogs"))
                val tasks = tasksFromJson(json.optJSONArray("tasks"))
                val routines = routinesFromJson(json.optJSONArray("routines"))
                val routineSteps = routineStepsFromJson(json.optJSONArray("routineSteps"))
                val routineLogs = routineLogsFromJson(json.optJSONArray("routineLogs"))
                val pomodoroSessions =
                    pomodoroSessionsFromJson(json.optJSONArray("pomodoroSessions"))

                // Restore inside a transaction for atomicity
                database.withTransaction {
                    // Clear all existing data (order matters for foreign keys)
                    habitLogDao.deleteAllLogs()
                    habitDao.deleteAllHabits()
                    pomodoroSessionDao.deleteAllSessions()
                    routineDao.deleteAllRoutineLogs()
                    routineDao.deleteAllRoutineSteps()
                    routineDao.deleteAllRoutines()
                    taskDao.deleteAllTasks()

                    // Insert all (order matters for foreign keys)
                    if (habits.isNotEmpty()) habitDao.insertAllHabits(habits)
                    if (habitLogs.isNotEmpty()) habitLogDao.insertAllLogs(habitLogs)
                    if (tasks.isNotEmpty()) taskDao.insertAllTasks(tasks)
                    if (routines.isNotEmpty()) routineDao.insertAllRoutines(routines)
                    if (routineSteps.isNotEmpty()) routineDao.insertAllRoutineSteps(routineSteps)
                    if (routineLogs.isNotEmpty()) routineDao.insertAllRoutineLogs(routineLogs)
                    if (pomodoroSessions.isNotEmpty()) {
                        pomodoroSessionDao.insertAllSessions(pomodoroSessions)
                    }
                }

                habits.size + habitLogs.size + tasks.size + routines.size +
                    routineSteps.size + routineLogs.size + pomodoroSessions.size
            }

        private fun getAppVersion(): String =
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "unknown"
            } catch (e: Exception) {
                "unknown"
            }

        // ── Habits ──────────────────────────────────────────

        private fun habitsToJson(habits: List<HabitEntity>): JSONArray =
            JSONArray().apply {
                habits.forEach { h ->
                    put(
                        JSONObject().apply {
                            put("id", h.id)
                            put("title", h.title)
                            put("description", h.description ?: JSONObject.NULL)
                            put("icon", h.icon ?: JSONObject.NULL)
                            put("color", h.color ?: JSONObject.NULL)
                            put("habitType", h.habitType)
                            put("targetValue", h.targetValue)
                            put("unit", h.unit ?: JSONObject.NULL)
                            put("targetOperator", h.targetOperator)
                            put("checklistJson", h.checklistJson ?: JSONObject.NULL)
                            put("goalCount", h.goalCount)
                            put("frequencyType", h.frequencyType)
                            put("frequencyValue", h.frequencyValue)
                            put("scheduledDaysJson", h.scheduledDaysJson ?: JSONObject.NULL)
                            put("startDate", h.startDate)
                            put("endDate", h.endDate ?: JSONObject.NULL)
                            put("reminderEnabled", h.reminderEnabled)
                            put("reminderTimeMinutes", h.reminderTimeMinutes ?: JSONObject.NULL)
                            put("createdAt", h.createdAt)
                            put("updatedAt", h.updatedAt)
                            put("isArchived", h.isArchived)
                            put("sortOrder", h.sortOrder)
                        },
                    )
                }
            }

        private fun habitsFromJson(array: JSONArray?): List<HabitEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                HabitEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    description = o.optNullableString("description"),
                    icon = o.optNullableString("icon"),
                    color = o.optNullableString("color"),
                    habitType = o.optString("habitType", "SIMPLE"),
                    targetValue = o.optInt("targetValue", 1),
                    unit = o.optNullableString("unit"),
                    targetOperator = o.optString("targetOperator", "AT_LEAST"),
                    checklistJson = o.optNullableString("checklistJson"),
                    goalCount = o.optInt("goalCount", 1),
                    frequencyType = o.optString("frequencyType", "DAILY"),
                    frequencyValue = o.optInt("frequencyValue", 1),
                    scheduledDaysJson = o.optNullableString("scheduledDaysJson"),
                    startDate = o.getLong("startDate"),
                    endDate = o.optNullableLong("endDate"),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderTimeMinutes = o.optNullableInt("reminderTimeMinutes"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    isArchived = o.optBoolean("isArchived", false),
                    sortOrder = o.optInt("sortOrder", 0),
                )
            }
        }

        // ── Habit Logs ──────────────────────────────────────

        private fun habitLogsToJson(logs: List<HabitLogEntity>): JSONArray =
            JSONArray().apply {
                logs.forEach { l ->
                    put(
                        JSONObject().apply {
                            put("id", l.id)
                            put("habitId", l.habitId)
                            put("date", l.date)
                            put("currentValue", l.currentValue)
                            put("targetValue", l.targetValue)
                            put("isCompleted", l.isCompleted)
                            put(
                                "completedChecklistItemsJson",
                                l.completedChecklistItemsJson ?: JSONObject.NULL,
                            )
                            put("currentCount", l.currentCount)
                            put("goalCount", l.goalCount)
                            put("createdAt", l.createdAt)
                            put("updatedAt", l.updatedAt)
                            put("completedAt", l.completedAt ?: JSONObject.NULL)
                        },
                    )
                }
            }

        private fun habitLogsFromJson(array: JSONArray?): List<HabitLogEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                HabitLogEntity(
                    id = o.getString("id"),
                    habitId = o.getString("habitId"),
                    date = o.getLong("date"),
                    currentValue = o.optInt("currentValue", 0),
                    targetValue = o.optInt("targetValue", 1),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedChecklistItemsJson =
                        o.optNullableString("completedChecklistItemsJson"),
                    currentCount = o.optInt("currentCount", 0),
                    goalCount = o.optInt("goalCount", 1),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    completedAt = o.optNullableLong("completedAt"),
                )
            }
        }

        // ── Tasks ───────────────────────────────────────────

        private fun tasksToJson(tasks: List<TaskEntity>): JSONArray =
            JSONArray().apply {
                tasks.forEach { t ->
                    put(
                        JSONObject().apply {
                            put("id", t.id)
                            put("title", t.title)
                            put("description", t.description ?: JSONObject.NULL)
                            put("parentTaskId", t.parentTaskId ?: JSONObject.NULL)
                            put("projectId", t.projectId ?: JSONObject.NULL)
                            put("dueDate", t.dueDate?.toString() ?: JSONObject.NULL)
                            put("dueTime", t.dueTime?.toString() ?: JSONObject.NULL)
                            put("isRecurring", t.isRecurring)
                            put("repeatPattern", t.repeatPattern?.name ?: JSONObject.NULL)
                            put(
                                "repeatDays",
                                t.repeatDays?.let { days ->
                                    JSONArray(days.map { it.name })
                                } ?: JSONObject.NULL,
                            )
                            put("priority", t.priority.name)
                            put("tags", JSONArray(t.tags))
                            put("isCompleted", t.isCompleted)
                            put("completedAt", t.completedAt ?: JSONObject.NULL)
                            put("reminderEnabled", t.reminderEnabled)
                            put("reminderMinutesBefore", t.reminderMinutesBefore)
                            put("createdAt", t.createdAt)
                            put("updatedAt", t.updatedAt)
                            put("sortOrder", t.sortOrder)
                            put("syncStatus", t.syncStatus.name)
                            put("lastSyncedAt", t.lastSyncedAt ?: JSONObject.NULL)
                            put("deletedAt", t.deletedAt ?: JSONObject.NULL)
                        },
                    )
                }
            }

        private fun tasksFromJson(array: JSONArray?): List<TaskEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                TaskEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    description = o.optNullableString("description"),
                    parentTaskId = o.optNullableString("parentTaskId"),
                    projectId = o.optNullableString("projectId"),
                    dueDate =
                        o.optNullableString("dueDate")
                            ?.let { java.time.LocalDate.parse(it) },
                    dueTime =
                        o.optNullableString("dueTime")
                            ?.let { java.time.LocalTime.parse(it) },
                    isRecurring = o.optBoolean("isRecurring", false),
                    repeatPattern =
                        o.optNullableString("repeatPattern")
                            ?.let { com.habitao.domain.model.RepeatPattern.valueOf(it) },
                    repeatDays =
                        o.optJSONArray("repeatDays")?.let { arr ->
                            (0 until arr.length()).map {
                                java.time.DayOfWeek.valueOf(arr.getString(it))
                            }
                        },
                    priority =
                        com.habitao.domain.model.TaskPriority.valueOf(
                            o.optString("priority", "NONE"),
                        ),
                    tags =
                        o.optJSONArray("tags")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList(),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedAt = o.optNullableLong("completedAt"),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderMinutesBefore = o.optInt("reminderMinutesBefore", 60),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    sortOrder = o.optInt("sortOrder", 0),
                    syncStatus =
                        com.habitao.domain.model.SyncStatus.valueOf(
                            o.optString("syncStatus", "LOCAL"),
                        ),
                    lastSyncedAt = o.optNullableLong("lastSyncedAt"),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        // ── Routines ────────────────────────────────────────

        private fun routinesToJson(routines: List<RoutineEntity>): JSONArray =
            JSONArray().apply {
                routines.forEach { r ->
                    put(
                        JSONObject().apply {
                            put("id", r.id)
                            put("title", r.title)
                            put("description", r.description ?: JSONObject.NULL)
                            put("icon", r.icon ?: JSONObject.NULL)
                            put("color", r.color ?: JSONObject.NULL)
                            put("repeatPattern", r.repeatPattern.name)
                            put(
                                "repeatDays",
                                r.repeatDays?.let { days ->
                                    JSONArray(days.map { it.name })
                                } ?: JSONObject.NULL,
                            )
                            put("customInterval", r.customInterval ?: JSONObject.NULL)
                            put("startDate", r.startDate.toString())
                            put("endDate", r.endDate?.toString() ?: JSONObject.NULL)
                            put("nextScheduledDate", r.nextScheduledDate.toString())
                            put("completionThreshold", r.completionThreshold.toDouble())
                            put("reminderEnabled", r.reminderEnabled)
                            put("reminderTime", r.reminderTime?.toString() ?: JSONObject.NULL)
                            put("createdAt", r.createdAt)
                            put("updatedAt", r.updatedAt)
                            put("isArchived", r.isArchived)
                            put("sortOrder", r.sortOrder)
                            put("syncStatus", r.syncStatus.name)
                            put("lastSyncedAt", r.lastSyncedAt ?: JSONObject.NULL)
                            put("deletedAt", r.deletedAt ?: JSONObject.NULL)
                        },
                    )
                }
            }

        private fun routinesFromJson(array: JSONArray?): List<RoutineEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                RoutineEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    description = o.optNullableString("description"),
                    icon = o.optNullableString("icon"),
                    color = o.optNullableString("color"),
                    repeatPattern =
                        com.habitao.domain.model.RepeatPattern.valueOf(
                            o.getString("repeatPattern"),
                        ),
                    repeatDays =
                        o.optJSONArray("repeatDays")?.let { arr ->
                            (0 until arr.length()).map {
                                java.time.DayOfWeek.valueOf(arr.getString(it))
                            }
                        },
                    customInterval = o.optNullableInt("customInterval"),
                    startDate = java.time.LocalDate.parse(o.getString("startDate")),
                    endDate =
                        o.optNullableString("endDate")
                            ?.let { java.time.LocalDate.parse(it) },
                    nextScheduledDate =
                        java.time.LocalDate.parse(o.getString("nextScheduledDate")),
                    completionThreshold =
                        o.optDouble("completionThreshold", 1.0).toFloat(),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderTime =
                        o.optNullableString("reminderTime")
                            ?.let { java.time.LocalTime.parse(it) },
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    isArchived = o.optBoolean("isArchived", false),
                    sortOrder = o.optInt("sortOrder", 0),
                    syncStatus =
                        com.habitao.domain.model.SyncStatus.valueOf(
                            o.optString("syncStatus", "LOCAL"),
                        ),
                    lastSyncedAt = o.optNullableLong("lastSyncedAt"),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        // ── Routine Steps ───────────────────────────────────

        private fun routineStepsToJson(steps: List<RoutineStepEntity>): JSONArray =
            JSONArray().apply {
                steps.forEach { s ->
                    put(
                        JSONObject().apply {
                            put("id", s.id)
                            put("routineId", s.routineId)
                            put("stepOrder", s.stepOrder)
                            put("title", s.title)
                            put("description", s.description ?: JSONObject.NULL)
                            put(
                                "estimatedDurationMinutes",
                                s.estimatedDurationMinutes ?: JSONObject.NULL,
                            )
                            put("createdAt", s.createdAt)
                            put("updatedAt", s.updatedAt)
                            put("syncStatus", s.syncStatus.name)
                            put("deletedAt", s.deletedAt ?: JSONObject.NULL)
                        },
                    )
                }
            }

        private fun routineStepsFromJson(array: JSONArray?): List<RoutineStepEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                RoutineStepEntity(
                    id = o.getString("id"),
                    routineId = o.getString("routineId"),
                    stepOrder = o.getInt("stepOrder"),
                    title = o.getString("title"),
                    description = o.optNullableString("description"),
                    estimatedDurationMinutes = o.optNullableInt("estimatedDurationMinutes"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    syncStatus =
                        com.habitao.domain.model.SyncStatus.valueOf(
                            o.optString("syncStatus", "LOCAL"),
                        ),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        // ── Routine Logs ────────────────────────────────────

        private fun routineLogsToJson(logs: List<RoutineLogEntity>): JSONArray =
            JSONArray().apply {
                logs.forEach { l ->
                    put(
                        JSONObject().apply {
                            put("id", l.id)
                            put("routineId", l.routineId)
                            put("date", l.date.toString())
                            put("completedStepIds", JSONArray(l.completedStepIds))
                            put("totalSteps", l.totalSteps)
                            put("completionPercentage", l.completionPercentage.toDouble())
                            put("isCompleted", l.isCompleted)
                            put("createdAt", l.createdAt)
                            put("updatedAt", l.updatedAt)
                            put("completedAt", l.completedAt ?: JSONObject.NULL)
                            put("syncStatus", l.syncStatus.name)
                            put("deletedAt", l.deletedAt ?: JSONObject.NULL)
                        },
                    )
                }
            }

        private fun routineLogsFromJson(array: JSONArray?): List<RoutineLogEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                RoutineLogEntity(
                    id = o.getString("id"),
                    routineId = o.getString("routineId"),
                    date = java.time.LocalDate.parse(o.getString("date")),
                    completedStepIds =
                        o.optJSONArray("completedStepIds")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList(),
                    totalSteps = o.getInt("totalSteps"),
                    completionPercentage =
                        o.optDouble("completionPercentage", 0.0).toFloat(),
                    isCompleted = o.optBoolean("isCompleted", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    completedAt = o.optNullableLong("completedAt"),
                    syncStatus =
                        com.habitao.domain.model.SyncStatus.valueOf(
                            o.optString("syncStatus", "LOCAL"),
                        ),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        // ── Pomodoro Sessions ───────────────────────────────

        private fun pomodoroSessionsToJson(sessions: List<PomodoroSessionEntity>): JSONArray =
            JSONArray().apply {
                sessions.forEach { s ->
                    put(
                        JSONObject().apply {
                            put("id", s.id)
                            put("sessionType", s.sessionType)
                            put("workDurationSeconds", s.workDurationSeconds)
                            put("breakDurationSeconds", s.breakDurationSeconds)
                            put("linkedTaskId", s.linkedTaskId ?: JSONObject.NULL)
                            put("linkedHabitId", s.linkedHabitId ?: JSONObject.NULL)
                            put("startedAt", s.startedAt)
                            put("completedAt", s.completedAt ?: JSONObject.NULL)
                            put("wasInterrupted", s.wasInterrupted)
                            put(
                                "actualDurationSeconds",
                                s.actualDurationSeconds ?: JSONObject.NULL,
                            )
                            put("createdAt", s.createdAt)
                        },
                    )
                }
            }

        private fun pomodoroSessionsFromJson(array: JSONArray?): List<PomodoroSessionEntity> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                PomodoroSessionEntity(
                    id = o.getString("id"),
                    sessionType = o.getString("sessionType"),
                    workDurationSeconds = o.getInt("workDurationSeconds"),
                    breakDurationSeconds = o.getInt("breakDurationSeconds"),
                    linkedTaskId = o.optNullableString("linkedTaskId"),
                    linkedHabitId = o.optNullableString("linkedHabitId"),
                    startedAt = o.getLong("startedAt"),
                    completedAt = o.optNullableLong("completedAt"),
                    wasInterrupted = o.optBoolean("wasInterrupted", false),
                    actualDurationSeconds = o.optNullableInt("actualDurationSeconds"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }

        // ── JSON helpers for nullable types ─────────────────

        private fun JSONObject.optNullableString(key: String): String? =
            if (isNull(key) || !has(key)) null else optString(key)

        private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)

        private fun JSONObject.optNullableInt(key: String): Int? = if (isNull(key)) null else optInt(key)
    }
