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
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.SyncStatus
import com.habitao.domain.model.TaskPriority
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Handles full database backup (export) and restore (import) via JSON.
 * All entity fields are serialized using the Room type-converter conventions.
 */
class BackupManager
    constructor(
        private val context: Context,
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

        suspend fun exportToUri(uri: Uri): Result<Int> =
            runCatching {
                val habits = habitDao.getAllHabitsIncludingArchived()
                val habitLogs = habitLogDao.getAllLogs()
                val tasks = taskDao.getAllTasks()
                val routines = routineDao.getAllRoutines()
                val routineSteps = routineDao.getAllRoutineSteps()
                val routineLogs = routineDao.getAllRoutineLogs()
                val pomodoroSessions = pomodoroSessionDao.getAllSessions()

                val json = buildJsonObject {
                    put("version", BACKUP_VERSION)
                    put("exportedAt", Clock.System.now().toEpochMilliseconds())
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
                    outputStream.write(json.toString().toByteArray(Charsets.UTF_8))
                } ?: throw Exception("Failed to open output stream")

                habits.size + habitLogs.size + tasks.size + routines.size +
                    routineSteps.size + routineLogs.size + pomodoroSessions.size
            }

        suspend fun importFromUri(uri: Uri): Result<Int> =
            runCatching {
                val jsonString =
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw Exception("Failed to open input stream")

                val json = Json.parseToJsonElement(jsonString).jsonObject
                val version = json.optInt("version", 0)
                if (version < 1 || version > BACKUP_VERSION) {
                    throw Exception("Unsupported backup version: $version (current: $BACKUP_VERSION)")
                }

                val habits = habitsFromJson(json["habits"]?.jsonArray)
                val habitLogs = habitLogsFromJson(json["habitLogs"]?.jsonArray)
                val tasks = tasksFromJson(json["tasks"]?.jsonArray)
                val routines = routinesFromJson(json["routines"]?.jsonArray)
                val routineSteps = routineStepsFromJson(json["routineSteps"]?.jsonArray)
                val routineLogs = routineLogsFromJson(json["routineLogs"]?.jsonArray)
                val pomodoroSessions = pomodoroSessionsFromJson(json["pomodoroSessions"]?.jsonArray)

                database.withTransaction {
                    habitLogDao.deleteAllLogs()
                    habitDao.deleteAllHabits()
                    pomodoroSessionDao.deleteAllSessions()
                    routineDao.deleteAllRoutineLogs()
                    routineDao.deleteAllRoutineSteps()
                    routineDao.deleteAllRoutines()
                    taskDao.deleteAllTasks()

                    if (habits.isNotEmpty()) habitDao.insertAllHabits(habits)
                    if (habitLogs.isNotEmpty()) habitLogDao.insertAllLogs(habitLogs)
                    if (tasks.isNotEmpty()) taskDao.insertAllTasks(tasks)
                    if (routines.isNotEmpty()) routineDao.insertAllRoutines(routines)
                    if (routineSteps.isNotEmpty()) routineDao.insertAllRoutineSteps(routineSteps)
                    if (routineLogs.isNotEmpty()) routineDao.insertAllRoutineLogs(routineLogs)
                    if (pomodoroSessions.isNotEmpty()) pomodoroSessionDao.insertAllSessions(pomodoroSessions)
                }

                habits.size + habitLogs.size + tasks.size + routines.size +
                    routineSteps.size + routineLogs.size + pomodoroSessions.size
            }

        private fun getAppVersion(): String =
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "unknown"
            } catch (e: Exception) { "unknown" }

        private fun habitsToJson(habits: List<HabitEntity>): JsonArray =
            buildJsonArray {
                habits.forEach { h ->
                    add(buildJsonObject {
                        put("id", h.id); put("title", h.title)
                        put("description", h.description); put("icon", h.icon); put("color", h.color)
                        put("habitType", h.habitType); put("targetValue", h.targetValue)
                        put("unit", h.unit); put("targetOperator", h.targetOperator)
                        put("checklistJson", h.checklistJson); put("goalCount", h.goalCount)
                        put("frequencyType", h.frequencyType); put("frequencyValue", h.frequencyValue)
                        put("scheduledDaysJson", h.scheduledDaysJson)
                        put("startDate", h.startDate); put("endDate", h.endDate)
                        put("reminderEnabled", h.reminderEnabled)
                        put("reminderTimeMinutes", h.reminderTimeMinutes)
                        put("createdAt", h.createdAt); put("updatedAt", h.updatedAt)
                        put("isArchived", h.isArchived); put("sortOrder", h.sortOrder)
                    })
                }
            }

        private fun habitsFromJson(array: JsonArray?): List<HabitEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                HabitEntity(
                    id = o.getString("id"), title = o.getString("title"),
                    description = o.optNullableString("description"),
                    icon = o.optNullableString("icon"), color = o.optNullableString("color"),
                    habitType = o.optString("habitType", "SIMPLE"),
                    targetValue = o.optInt("targetValue", 1),
                    unit = o.optNullableString("unit"),
                    targetOperator = o.optString("targetOperator", "AT_LEAST"),
                    checklistJson = o.optNullableString("checklistJson"),
                    goalCount = o.optInt("goalCount", 1),
                    frequencyType = o.optString("frequencyType", "DAILY"),
                    frequencyValue = o.optInt("frequencyValue", 1),
                    scheduledDaysJson = o.optNullableString("scheduledDaysJson"),
                    startDate = o.getLong("startDate"), endDate = o.optNullableLong("endDate"),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderTimeMinutes = o.optNullableInt("reminderTimeMinutes"),
                    createdAt = o.optLong("createdAt", now), updatedAt = o.optLong("updatedAt", now),
                    isArchived = o.optBoolean("isArchived", false), sortOrder = o.optInt("sortOrder", 0),
                )
            }
        }

        private fun habitLogsToJson(logs: List<HabitLogEntity>): JsonArray =
            buildJsonArray {
                logs.forEach { l ->
                    add(buildJsonObject {
                        put("id", l.id); put("habitId", l.habitId); put("date", l.date)
                        put("currentValue", l.currentValue); put("targetValue", l.targetValue)
                        put("isCompleted", l.isCompleted)
                        put("completedChecklistItemsJson", l.completedChecklistItemsJson)
                        put("currentCount", l.currentCount); put("goalCount", l.goalCount)
                        put("createdAt", l.createdAt); put("updatedAt", l.updatedAt)
                        put("completedAt", l.completedAt)
                    })
                }
            }

        private fun habitLogsFromJson(array: JsonArray?): List<HabitLogEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                HabitLogEntity(
                    id = o.getString("id"), habitId = o.getString("habitId"),
                    date = o.getLong("date"),
                    currentValue = o.optInt("currentValue", 0),
                    targetValue = o.optInt("targetValue", 1),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedChecklistItemsJson = o.optNullableString("completedChecklistItemsJson"),
                    currentCount = o.optInt("currentCount", 0),
                    goalCount = o.optInt("goalCount", 1),
                    createdAt = o.optLong("createdAt", now), updatedAt = o.optLong("updatedAt", now),
                    completedAt = o.optNullableLong("completedAt"),
                )
            }
        }

        private fun tasksToJson(tasks: List<TaskEntity>): JsonArray =
            buildJsonArray {
                tasks.forEach { t ->
                    add(buildJsonObject {
                        put("id", t.id); put("title", t.title)
                        put("description", t.description); put("parentTaskId", t.parentTaskId)
                        put("projectId", t.projectId)
                        put("dueDate", t.dueDate?.toString()); put("dueTime", t.dueTime?.toString())
                        put("isRecurring", t.isRecurring); put("repeatPattern", t.repeatPattern?.name)
                        put("repeatDays", t.repeatDays?.let { days -> buildJsonArray { days.forEach { add(JsonPrimitive(it.name)) } } })
                        put("priority", t.priority.name)
                        put("tags", buildJsonArray { t.tags.forEach { add(JsonPrimitive(it)) } })
                        put("isCompleted", t.isCompleted); put("completedAt", t.completedAt)
                        put("reminderEnabled", t.reminderEnabled)
                        put("reminderMinutesBefore", t.reminderMinutesBefore)
                        put("createdAt", t.createdAt); put("updatedAt", t.updatedAt)
                        put("sortOrder", t.sortOrder); put("syncStatus", t.syncStatus.name)
                        put("lastSyncedAt", t.lastSyncedAt); put("deletedAt", t.deletedAt)
                    })
                }
            }

        private fun tasksFromJson(array: JsonArray?): List<TaskEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                TaskEntity(
                    id = o.getString("id"), title = o.getString("title"),
                    description = o.optNullableString("description"),
                    parentTaskId = o.optNullableString("parentTaskId"),
                    projectId = o.optNullableString("projectId"),
                    dueDate = o.optNullableString("dueDate")?.let { LocalDate.parse(it) },
                    dueTime = o.optNullableString("dueTime")?.let { LocalTime.parse(it) },
                    isRecurring = o.optBoolean("isRecurring", false),
                    repeatPattern = o.optNullableString("repeatPattern")
                        ?.let { runCatching { RepeatPattern.valueOf(it) }.getOrNull() },
                    repeatDays = o["repeatDays"]?.jsonArray
                        ?.map { DayOfWeek.valueOf(it.jsonPrimitive.content) },
                    priority = runCatching { TaskPriority.valueOf(o.optString("priority", "NONE")) }.getOrDefault(TaskPriority.NONE),
                    tags = o["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedAt = o.optNullableLong("completedAt"),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderMinutesBefore = o.optInt("reminderMinutesBefore", 60),
                    createdAt = o.optLong("createdAt", now), updatedAt = o.optLong("updatedAt", now),
                    sortOrder = o.optInt("sortOrder", 0),
                    syncStatus = runCatching { SyncStatus.valueOf(o.optString("syncStatus", "LOCAL")) }.getOrDefault(SyncStatus.LOCAL),
                    lastSyncedAt = o.optNullableLong("lastSyncedAt"),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        private fun routinesToJson(routines: List<RoutineEntity>): JsonArray =
            buildJsonArray {
                routines.forEach { r ->
                    add(buildJsonObject {
                        put("id", r.id); put("title", r.title)
                        put("description", r.description); put("icon", r.icon); put("color", r.color)
                        put("repeatPattern", r.repeatPattern.name)
                        put("repeatDays", r.repeatDays?.let { days -> buildJsonArray { days.forEach { add(JsonPrimitive(it.name)) } } })
                        put("customInterval", r.customInterval)
                        put("startDate", r.startDate.toString())
                        put("endDate", r.endDate?.toString())
                        put("nextScheduledDate", r.nextScheduledDate.toString())
                        put("completionThreshold", r.completionThreshold.toDouble())
                        put("reminderEnabled", r.reminderEnabled)
                        put("reminderTime", r.reminderTime?.toString())
                        put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
                        put("isArchived", r.isArchived); put("sortOrder", r.sortOrder)
                        put("syncStatus", r.syncStatus.name)
                        put("lastSyncedAt", r.lastSyncedAt); put("deletedAt", r.deletedAt)
                    })
                }
            }

        private fun routinesFromJson(array: JsonArray?): List<RoutineEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                RoutineEntity(
                    id = o.getString("id"), title = o.getString("title"),
                    description = o.optNullableString("description"),
                    icon = o.optNullableString("icon"), color = o.optNullableString("color"),
                    repeatPattern = runCatching { RepeatPattern.valueOf(o.getString("repeatPattern")) }.getOrDefault(RepeatPattern.DAILY),
                    repeatDays = o["repeatDays"]?.jsonArray?.map { DayOfWeek.valueOf(it.jsonPrimitive.content) },
                    customInterval = o.optNullableInt("customInterval"),
                    startDate = LocalDate.parse(o.getString("startDate")),
                    endDate = o.optNullableString("endDate")?.let { LocalDate.parse(it) },
                    nextScheduledDate = LocalDate.parse(o.getString("nextScheduledDate")),
                    completionThreshold = o.optDouble("completionThreshold", 1.0).toFloat(),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderTime = o.optNullableString("reminderTime")?.let { LocalTime.parse(it) },
                    createdAt = o.optLong("createdAt", now), updatedAt = o.optLong("updatedAt", now),
                    isArchived = o.optBoolean("isArchived", false), sortOrder = o.optInt("sortOrder", 0),
                    syncStatus = runCatching { SyncStatus.valueOf(o.optString("syncStatus", "LOCAL")) }.getOrDefault(SyncStatus.LOCAL),
                    lastSyncedAt = o.optNullableLong("lastSyncedAt"),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        private fun routineStepsToJson(steps: List<RoutineStepEntity>): JsonArray =
            buildJsonArray {
                steps.forEach { s ->
                    add(buildJsonObject {
                        put("id", s.id); put("routineId", s.routineId)
                        put("stepOrder", s.stepOrder); put("title", s.title)
                        put("description", s.description)
                        put("estimatedDurationMinutes", s.estimatedDurationMinutes)
                        put("createdAt", s.createdAt); put("updatedAt", s.updatedAt)
                        put("syncStatus", s.syncStatus.name); put("deletedAt", s.deletedAt)
                    })
                }
            }

        private fun routineStepsFromJson(array: JsonArray?): List<RoutineStepEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                RoutineStepEntity(
                    id = o.getString("id"), routineId = o.getString("routineId"),
                    stepOrder = o.getInt("stepOrder"), title = o.getString("title"),
                    description = o.optNullableString("description"),
                    estimatedDurationMinutes = o.optNullableInt("estimatedDurationMinutes"),
                    createdAt = o.optLong("createdAt", now), updatedAt = o.optLong("updatedAt", now),
                    syncStatus = runCatching { SyncStatus.valueOf(o.optString("syncStatus", "LOCAL")) }.getOrDefault(SyncStatus.LOCAL),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        private fun routineLogsToJson(logs: List<RoutineLogEntity>): JsonArray =
            buildJsonArray {
                logs.forEach { l ->
                    add(buildJsonObject {
                        put("id", l.id); put("routineId", l.routineId)
                        put("date", l.date.toString())
                        put("completedStepIds", buildJsonArray { l.completedStepIds.forEach { add(JsonPrimitive(it)) } })
                        put("totalSteps", l.totalSteps)
                        put("completionPercentage", l.completionPercentage.toDouble())
                        put("isCompleted", l.isCompleted)
                        put("createdAt", l.createdAt); put("updatedAt", l.updatedAt)
                        put("completedAt", l.completedAt)
                        put("syncStatus", l.syncStatus.name); put("deletedAt", l.deletedAt)
                    })
                }
            }

        private fun routineLogsFromJson(array: JsonArray?): List<RoutineLogEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                RoutineLogEntity(
                    id = o.getString("id"), routineId = o.getString("routineId"),
                    date = LocalDate.parse(o.getString("date")),
                    completedStepIds = o["completedStepIds"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    totalSteps = o.getInt("totalSteps"),
                    completionPercentage = o.optDouble("completionPercentage", 0.0).toFloat(),
                    isCompleted = o.optBoolean("isCompleted", false),
                    createdAt = o.optLong("createdAt", now), updatedAt = o.optLong("updatedAt", now),
                    completedAt = o.optNullableLong("completedAt"),
                    syncStatus = runCatching { SyncStatus.valueOf(o.optString("syncStatus", "LOCAL")) }.getOrDefault(SyncStatus.LOCAL),
                    deletedAt = o.optNullableLong("deletedAt"),
                )
            }
        }

        private fun pomodoroSessionsToJson(sessions: List<PomodoroSessionEntity>): JsonArray =
            buildJsonArray {
                sessions.forEach { s ->
                    add(buildJsonObject {
                        put("id", s.id); put("sessionType", s.sessionType)
                        put("workDurationSeconds", s.workDurationSeconds)
                        put("breakDurationSeconds", s.breakDurationSeconds)
                        put("linkedTaskId", s.linkedTaskId); put("linkedHabitId", s.linkedHabitId)
                        put("startedAt", s.startedAt); put("completedAt", s.completedAt)
                        put("wasInterrupted", s.wasInterrupted)
                        put("actualDurationSeconds", s.actualDurationSeconds)
                        put("createdAt", s.createdAt)
                    })
                }
            }

        private fun pomodoroSessionsFromJson(array: JsonArray?): List<PomodoroSessionEntity> {
            if (array == null) return emptyList()
            val now = Clock.System.now().toEpochMilliseconds()
            return array.map { element ->
                val o = element.jsonObject
                PomodoroSessionEntity(
                    id = o.getString("id"), sessionType = o.getString("sessionType"),
                    workDurationSeconds = o.getInt("workDurationSeconds"),
                    breakDurationSeconds = o.getInt("breakDurationSeconds"),
                    linkedTaskId = o.optNullableString("linkedTaskId"),
                    linkedHabitId = o.optNullableString("linkedHabitId"),
                    startedAt = o.getLong("startedAt"), completedAt = o.optNullableLong("completedAt"),
                    wasInterrupted = o.optBoolean("wasInterrupted", false),
                    actualDurationSeconds = o.optNullableInt("actualDurationSeconds"),
                    createdAt = o.optLong("createdAt", now),
                )
            }
        }

        private fun JsonObject.getString(key: String): String = this[key]!!.jsonPrimitive.content
        private fun JsonObject.getLong(key: String): Long = this[key]!!.jsonPrimitive.content.toLong()
        private fun JsonObject.getInt(key: String): Int = this[key]!!.jsonPrimitive.content.toInt()
        private fun JsonObject.optString(key: String, default: String = ""): String =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: default
        private fun JsonObject.optNullableString(key: String): String? =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
        private fun JsonObject.optLong(key: String, default: Long = 0L): Long =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toLongOrNull() ?: default
        private fun JsonObject.optNullableLong(key: String): Long? =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toLongOrNull()
        private fun JsonObject.optInt(key: String, default: Int = 0): Int =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toIntOrNull() ?: default
        private fun JsonObject.optNullableInt(key: String): Int? =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toIntOrNull()
        private fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: default
        private fun JsonObject.optDouble(key: String, default: Double = 0.0): Double =
            this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toDoubleOrNull() ?: default
    }
