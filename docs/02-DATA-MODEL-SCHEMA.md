# Data Model & Database Schema
## Habitao - Entity Design & Relationships

**Version:** 1.0  
**Last Updated:** February 27, 2026  
**Database:** Room with SQLite  
**ORM:** Jetpack Room (KMP-compatible)

---

## 1. Overview

### 1.1 Design Principles
- **Local-First:** All data stored locally with no server dependency for MVP
- **Cloud-Ready:** Schema designed for future sync (UUIDs, timestamps, soft deletes)
- **Normalized:** Minimize data duplication, enforce referential integrity
- **Performant:** Indexed columns for common queries, optimized for mobile
- **Extensible:** Schema supports future features without breaking changes

### 1.2 Entity Categories
1. **Core Entities:** `Habit`, `Routine`, `Task`, `PomodoroSession`
2. **Relationship Entities:** `RoutineStep`, `TaskSubtask`
3. **Tracking Entities:** `HabitLog`, `RoutineLog`, `TaskCompletionLog`
4. **Configuration:** `AppSettings`, `NotificationSettings`

---

## 2. Core Entities

### 2.1 Habit

**Purpose:** Represents a recurring trackable action with count-based completion.

```kotlin
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"]),
        Index(value = ["nextScheduledDate"])
    ]
)
data class Habit(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // UUID for sync-readiness
    
    val title: String,
    val description: String? = null,
    val icon: String? = null, // Material icon name or emoji
    val color: String? = null, // Hex color for theming
    
    // Tracking Configuration
    val goalCount: Int = 1, // e.g., 8 for "8 glasses of water"
    val unit: String? = null, // e.g., "glasses", "minutes", "problems"
    val trackingType: TrackingType = TrackingType.COUNT, // COUNT, DURATION, BINARY
    
    // Scheduling
    val repeatPattern: RepeatPattern, // DAILY, WEEKLY, CUSTOM
    val repeatDays: List<DayOfWeek>? = null, // For WEEKLY (Mon, Wed, Fri)
    val customInterval: Int? = null, // For CUSTOM (every N days)
    val startDate: LocalDate,
    val endDate: LocalDate? = null, // Null = no end
    val nextScheduledDate: LocalDate, // Computed field for performance
    
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val reminderDays: List<DayOfWeek>? = null, // Can differ from repeat days
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0, // User-defined ordering
    
    // Sync (for future cloud sync)
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null // Soft delete for sync
)

enum class TrackingType { COUNT, DURATION, BINARY }
enum class RepeatPattern { DAILY, WEEKLY, CUSTOM, SPECIFIC_DATES }
enum class SyncStatus { LOCAL, SYNCED, PENDING_SYNC, CONFLICT }
```

**Business Rules:**
- `goalCount` must be > 0
- If `repeatPattern = WEEKLY`, `repeatDays` must not be empty
- If `repeatPattern = CUSTOM`, `customInterval` must be > 0
- `reminderTime` required if `reminderEnabled = true`
- `nextScheduledDate` recalculated on each completion or at midnight

**Indexes:**
- `createdAt`: Sort habits by creation date
- `isArchived`: Filter active vs archived habits
- `nextScheduledDate`: Efficiently query "today's habits"

---

### 2.2 HabitLog

**Purpose:** Records daily completion progress for habits.

```kotlin
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "date"], unique = true), // One log per habit per day
        Index(value = ["date"])
    ]
)
data class HabitLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val habitId: String,
    val date: LocalDate, // The day this log applies to
    
    // Progress Tracking
    val currentCount: Int = 0, // e.g., 5 out of 8 glasses
    val goalCount: Int, // Snapshot of habit's goalCount (may change over time)
    val isCompleted: Boolean = false, // True if currentCount >= goalCount
    
    // Duration tracking (if trackingType = DURATION)
    val durationSeconds: Int? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null, // Timestamp when isCompleted became true
    
    // Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null
)
```

**Business Rules:**
- One log per habit per day (enforced by unique index)
- `isCompleted` auto-updates when `currentCount >= goalCount`
- `completedAt` set when `isCompleted` transitions from false  true
- Logs older than 2 years can be archived/compressed (future optimization)

**Queries:**
- **Current Streak:** Find consecutive days where `isCompleted = true` from today backwards
- **Completion Rate:** `COUNT(isCompleted = true) / COUNT(*)` over date range
- **Heatmap Data:** Group by month, count completed days

---

### 2.3 Routine

**Purpose:** Represents an ordered sequence of steps that repeats as a unit.

```kotlin
@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"])
    ]
)
data class Routine(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    
    // Scheduling (same as Habit)
    val repeatPattern: RepeatPattern,
    val repeatDays: List<DayOfWeek>? = null,
    val customInterval: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextScheduledDate: LocalDate,
    
    // Completion Logic
    val completionThreshold: Float = 1.0f, // 1.0 = all steps, 0.8 = 80% of steps
    
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    
    // Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null
)
```

---

### 2.4 RoutineStep

**Purpose:** Individual steps within a routine (ordered).

```kotlin
@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId", "stepOrder"])
    ]
)
data class RoutineStep(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val routineId: String,
    val stepOrder: Int, // 0-based ordering
    
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int? = null, // Optional time estimate
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null
)
```

**Business Rules:**
- Steps are ordered by `stepOrder` (0, 1, 2, ...)
- When step deleted, remaining steps re-ordered
- Steps belong to exactly one routine

---

### 2.5 RoutineLog

**Purpose:** Tracks daily completion of routine and its steps.

```kotlin
@Entity(
    tableName = "routine_logs",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class RoutineLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val routineId: String,
    val date: LocalDate,
    
    // Step Completion (stored as JSON array of step IDs)
    val completedStepIds: List<String> = emptyList(),
    val totalSteps: Int, // Snapshot of step count
    
    // Derived fields
    val completionPercentage: Float = 0f, // completedSteps / totalSteps
    val isCompleted: Boolean = false, // Based on routine's completionThreshold
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    
    // Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null
)
```

**Business Rules:**
- One log per routine per day
- `completionPercentage` = `completedStepIds.size / totalSteps`
- `isCompleted` = `completionPercentage >= routine.completionThreshold`

---

### 2.6 Task

**Purpose:** Todoist/TickTick-style tasks with due dates, priorities, and subtasks.

```kotlin
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["priority"]),
        Index(value = ["isCompleted"]),
        Index(value = ["projectId"])
    ]
)
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val title: String,
    val description: String? = null,
    
    // Hierarchy
    val parentTaskId: String? = null, // For MVP: 1 level deep only
    val projectId: String? = null, // Future: organize into projects
    
    // Scheduling
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val isRecurring: Boolean = false,
    val repeatPattern: RepeatPattern? = null,
    val repeatDays: List<DayOfWeek>? = null,
    
    // Prioritization
    val priority: TaskPriority = TaskPriority.NONE,
    val tags: List<String> = emptyList(), // Future: tag system
    
    // Completion
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    
    // Reminders
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 60, // Remind 60 min before due time
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    
    // Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null
)

enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
```

**Business Rules:**
- If `isRecurring = true`, task clones itself on completion with next due date
- Parent tasks marked complete when all children complete
- Tasks with `dueDate` in the past shown in "Overdue" section
- Subtasks (MVP): Only 1 level deep (`parentTaskId` points to root task)

---

### 2.7 PomodoroSession

**Purpose:** Tracks focus timer sessions with optional task association.

```kotlin
@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["associatedTaskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["associatedTaskId"])
    ]
)
data class PomodoroSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Timer Configuration
    val workDurationSeconds: Int = 1500, // Default: 25 min = 1500 sec
    val breakDurationSeconds: Int = 300, // Default: 5 min = 300 sec
    val sessionType: PomodoroType, // WORK, SHORT_BREAK, LONG_BREAK
    
    // Association
    val associatedTaskId: String? = null, // Optional: link to task
    
    // Completion
    val startedAt: Long,
    val completedAt: Long? = null,
    val wasInterrupted: Boolean = false, // User manually stopped early
    val actualDurationSeconds: Int? = null, // Null if not completed
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    
    // Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    val deletedAt: Long? = null
)

enum class PomodoroType { WORK, SHORT_BREAK, LONG_BREAK }
```

**Business Rules:**
- `completedAt` set when timer naturally completes
- `wasInterrupted = true` if user stopped timer before completion
- `actualDurationSeconds` = time actually spent (may be < configured duration)

---

## 3. Configuration Entities

### 3.1 AppSettings

**Purpose:** Global app configuration (singleton).

```kotlin
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // Singleton: always 1
    
    // Theme
    val themeMode: ThemeMode = ThemeMode.SYSTEM, // LIGHT, DARK, SYSTEM
    val useDynamicColors: Boolean = true, // Material You dynamic colors
    val statsGraphType: StatsGraphType = StatsGraphType.BAR, // BAR, LINE
    val maxVisibleTabs: Int = 4, // Bottom navigation tabs shown at once
    val defaultLaunchTab: String = "stats", // Default startup tab ID
    
    // Notifications
    val notificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: LocalTime? = null, // e.g., 22:00
    val quietHoursEnd: LocalTime? = null, // e.g., 08:00
    
    // Pomodoro Defaults
    val defaultWorkDuration: Int = 1500, // 25 min
    val defaultShortBreak: Int = 300, // 5 min
    val defaultLongBreak: Int = 900, // 15 min
    val sessionsBeforeLongBreak: Int = 4,
    
    // Data & Privacy
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    
    // Sync (future)
    val cloudSyncEnabled: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.MANUAL,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class StatsGraphType { BAR, LINE }
enum class SyncFrequency { MANUAL, HOURLY, DAILY, REAL_TIME }
```

---

## 4. Entity Relationships

### 4.1 ER Diagram (Text Representation)

```
         
   Habit      1     *   HabitLog    

 id (PK)               id (PK)      
 title                 habitId (FK) 
 goalCount             date         
 repeatPattern          currentCount 
 ...                   isCompleted  
         

                  
  Routine     1     *  RoutineStep             RoutineLog  
         
 id (PK)               id (PK)                id (PK)      
 title                 routineId(FK)          routineId(FK)
 repeatPattern          stepOrder        *     date         
 ...                   title         completedIds 
             1    

                               
    Task      1                           *  PomodoroSession  

 id (PK)                                     id (PK)          
 title                                       associatedTaskId 
 parentTaskId (Self-referencing for subtasks) sessionType      
 dueDate                                     startedAt        
 priority                                    completedAt      
 isCompleted                                

       
        (Self-reference)
       
       
    Subtasks (same table, parentTaskId IS NOT NULL)
```

### 4.2 Relationship Summary

| Parent Entity | Child Entity | Relationship | Cascade Rule |
|---------------|--------------|--------------|--------------|
| Habit | HabitLog | 1:N | DELETE CASCADE |
| Routine | RoutineStep | 1:N | DELETE CASCADE |
| Routine | RoutineLog | 1:N | DELETE CASCADE |
| Task | Task (subtasks) | 1:N (self) | SET NULL or manual handling |
| Task | PomodoroSession | 1:N | SET NULL (sessions persist) |

---

## 5. Indexes & Performance

### 5.1 Critical Indexes

**Purpose:** Optimize common queries for mobile performance.

| Table | Index | Reason |
|-------|-------|--------|
| `habits` | `(isArchived, nextScheduledDate)` | "Today's active habits" query |
| `habit_logs` | `(habitId, date)` UNIQUE | Prevent duplicate logs, fast streak calculation |
| `habit_logs` | `(date)` | Date range queries for statistics |
| `routines` | `(isArchived, nextScheduledDate)` | "Today's active routines" |
| `routine_steps` | `(routineId, stepOrder)` | Ordered step retrieval |
| `tasks` | `(dueDate, isCompleted)` | "Today's tasks" query |
| `tasks` | `(parentTaskId)` | Subtask queries |
| `pomodoro_sessions` | `(associatedTaskId, startedAt)` | Task time tracking |

### 5.2 Query Optimization

**Critical Queries (must be < 50ms on mid-range device):**

1. **Today's Habits:**
   ```sql
   SELECT * FROM habits 
   WHERE isArchived = 0 AND nextScheduledDate <= :today
   ORDER BY sortOrder
   ```

2. **Current Streak (Single Habit):**
   ```sql
   SELECT COUNT(*) as streak FROM (
     SELECT date FROM habit_logs 
     WHERE habitId = :id AND isCompleted = 1 
     ORDER BY date DESC
   ) WHERE date = date('now', '-' || (rownum - 1) || ' days')
   ```

3. **Overdue Tasks:**
   ```sql
   SELECT * FROM tasks 
   WHERE isCompleted = 0 AND dueDate < :today
   ORDER BY priority DESC, dueDate ASC
   ```

---

## 6. Data Migrations

### 6.1 Version Strategy

**Room Version Management:**
- Version 1: Initial schema (MVP)
- Version 2: Add cloud sync fields
- Version 3: Add projects/categories
- Version 4: Add habit templates

### 6.2 Migration Example (v1  v2)

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add cloud sync fields
        database.execSQL("""
            ALTER TABLE habits 
            ADD COLUMN cloudId TEXT
        """)
        database.execSQL("""
            ALTER TABLE habits 
            ADD COLUMN lastSyncedAt INTEGER
        """)
        
        // Create index for sync queries
        database.execSQL("""
            CREATE INDEX index_habits_cloudId ON habits(cloudId)
        """)
    }
}
```

### 6.3 Soft Delete Strategy

**For Cloud Sync Compatibility:**
- Do NOT use `ON DELETE CASCADE` in production with sync
- Add `deletedAt` timestamp field to all sync-enabled entities
- Filter `WHERE deletedAt IS NULL` in all queries
- Periodically purge records where `deletedAt < (now - 90 days)`

---

## 7. Data Validation Rules

### 7.1 Entity-Level Validation

**Habit:**
- `title` must be 1-100 characters
- `goalCount` must be > 0 and < 1000
- `repeatDays` must contain at least 1 day if `repeatPattern = WEEKLY`
- `reminderTime` required if `reminderEnabled = true`

**Task:**
- `title` must be 1-500 characters
- `dueTime` requires `dueDate` to be set
- Subtasks cannot have subtasks (enforce `MAX_DEPTH = 1`)

**Routine:**
- Must have at least 1 step
- `stepOrder` must be sequential (0, 1, 2, ...) with no gaps

### 7.2 Database Constraints

```kotlin
@Database(
    entities = [
        Habit::class,
        HabitLog::class,
        Routine::class,
        RoutineStep::class,
        RoutineLog::class,
        Task::class,
        PomodoroSession::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = true // Generate schema JSON for version control
)
abstract class HabitaoDatabase : RoomDatabase() {
    // Enable foreign keys
    override fun init(configuration: DatabaseConfiguration) {
        super.init(configuration)
        configuration.sqliteOpenHelperFactory?.create(configuration)?.let { helper ->
            helper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}
```

---

## 8. Backup & Export

### 8.1 Export Format (JSON)

**Structure:**
```json
{
  "version": "1.0",
  "exportedAt": "2026-02-13T02:23:35Z",
  "habits": [ /* array of Habit objects */ ],
  "habitLogs": [ /* array of HabitLog objects */ ],
  "routines": [ /* array of Routine objects */ ],
  "routineSteps": [ /* array of RoutineStep objects */ ],
  "tasks": [ /* array of Task objects */ ],
  "pomodoroSessions": [ /* array of PomodoroSession objects */ ],
  "settings": { /* AppSettings object */ }
}
```

### 8.2 Local Backup Strategy

- **Auto-Backup:** Daily at 3 AM (WorkManager)
- **Location:** `<app-files>/backups/habitao_backup_YYYYMMDD.db`
- **Retention:** Keep last 7 backups, delete older
- **Encryption:** Use Android Keystore + Tink to encrypt backup files

---

## 9. Future Schema Extensions

### 9.1 Cloud Sync (v2.0)

**New Fields Needed:**
- `cloudId: String?` - Server-assigned ID
- `version: Int` - Optimistic locking
- `conflictData: String?` - JSON of conflicting version

**New Tables:**
- `SyncQueue` - Pending local changes to upload
- `SyncLog` - History of sync operations

### 9.2 Projects/Categories (v1.1)

**New Table:**
```kotlin
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val icon: String?,
    val sortOrder: Int
)
```

**Updated:** Add `projectId` foreign key to `Task`, `Habit`, `Routine`

---

**End of Data Model & Schema Document**
