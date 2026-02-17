# Technical Architecture (HLD)
## Habitao - High-Level Design

**Version:** 1.0  
**Last Updated:** February 13, 2026  
**Architecture Pattern:** Clean Architecture + MVI (Model-View-Intent)  
**Tech Stack:** Kotlin Multiplatform + Jetpack Compose

---

## 1. Executive Summary

### 1.1 Architecture Decision

**Chosen Stack: Kotlin Multiplatform (KMP) + Jetpack Compose**

**Rationale:**
1. **Native Performance:** Direct compilation to native code, no JS bridge
2. **Material Design 3 Expressive:** First-class support in Jetpack Compose
3. **Widget Support:** Native `RemoteViews` without cross-platform bridging complexity
4. **Background Reliability:** Direct access to `AlarmManager`, `WorkManager`, `ForegroundService`
5. **Future-Proof:** Share business logic with iOS via KMP without rewriting Android app
6. **Industry Validation:** Used by Todoist (pioneer), Netflix, Cash App, Shopify

### 1.2 Architecture Principles

| Principle | Implementation | Benefit |
|-----------|----------------|---------|
| **Separation of Concerns** | Clean Architecture layers (Domain, Data, Presentation) | Testable, maintainable, swappable components |
| **Unidirectional Data Flow** | MVI pattern with StateFlow | Predictable state management, easy debugging |
| **Dependency Inversion** | Hilt for DI | Loose coupling, easy mocking for tests |
| **Offline-First** | Local database as source of truth | Works without internet, fast UX |
| **Reactive Programming** | Kotlin Flow for data streams | Declarative, composable, lifecycle-aware |

---

## 2. System Architecture

### 2.1 Layered Architecture (Clean Architecture)

```

                    PRESENTATION LAYER                        
            
     UI (Compose)      ViewModel       Navigation        
     Screens           (MVI State)     (Voyager)         
            

                          State (Flow), Intents

                      DOMAIN LAYER                            
            
    Use Cases       Entities        Repository        
    (Business       (Models)        Interfaces        
     Logic)                                           
            

                          Domain Models

                       DATA LAYER                             
            
    Repository      Local DB        Remote API        
    Impl            (Room)          (Future)          
            
                                                              
            
    Data Source     DAO             API Service       
    (Proto DS)      (Room)          (Ktor)            
            



                    SYSTEM LAYER                              
            
    Notifications    WorkManager     Alarms            
    (NotificationMgr)  (Sync, Backup)  (Reminders)       
            

```

### 2.2 Module Structure (Gradle Multi-Module)

```
habitao/
 app/                          # Android app module
    src/main/
       java/com/habitao/
          HabitaoApplication.kt
          MainActivity.kt
          di/               # Hilt modules
       res/                  # Resources, themes
    build.gradle.kts

 feature/                      # Feature modules (presentation)
    habits/
       src/main/java/
          ui/
             HabitsScreen.kt
             HabitDetailScreen.kt
             components/
          viewmodel/
              HabitsViewModel.kt
       build.gradle.kts
   
    routines/
       ui/
       viewmodel/
   
    tasks/
       ui/
       viewmodel/
   
    pomodoro/
        ui/
        viewmodel/

 domain/                       # Business logic (pure Kotlin, KMP-ready)
    src/commonMain/kotlin/
       model/                # Domain entities
          Habit.kt
          Task.kt
          Routine.kt
       repository/           # Repository interfaces
          HabitRepository.kt
          TaskRepository.kt
       usecase/              # Use cases (business logic)
           habit/
              CreateHabitUseCase.kt
              GetTodaysHabitsUseCase.kt
              CalculateStreakUseCase.kt
           task/
           routine/
    build.gradle.kts

 data/                         # Data layer (KMP-ready)
    src/commonMain/kotlin/
       repository/           # Repository implementations
          HabitRepositoryImpl.kt
       local/
          database/         # Room database
             HabitaoDatabase.kt
             dao/
                HabitDao.kt
                TaskDao.kt
             entity/       # Room entities (separate from domain)
                 HabitEntity.kt
                 HabitLogEntity.kt
          preferences/      # Proto DataStore
              AppPreferences.kt
       remote/               # Future: API client (Ktor)
           api/
    build.gradle.kts

 core/                         # Shared utilities (KMP-ready)
    common/                   # Pure Kotlin utilities
       DateTimeUtils.kt
       StringUtils.kt
       Result.kt             # Result wrapper for error handling
    ui/                       # Shared Compose UI components
       theme/
          Theme.kt
          Color.kt
          Typography.kt
       components/
           HabitCard.kt
           ProgressIndicator.kt
    testing/                  # Test utilities
        FakeRepositories.kt

 system/                       # Android system integrations
    notifications/
       NotificationManager.kt
       NotificationChannels.kt
    alarms/
       AlarmScheduler.kt
    work/
       BackupWorker.kt
       SyncWorker.kt
    widget/
        HabitWidgetProvider.kt
        TaskWidgetProvider.kt

 build.gradle.kts              # Root build configuration
```

---

## 3. Technology Stack

### 3.1 Core Technologies

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Language** | Kotlin | 2.0+ | Modern, concise, null-safe |
| **UI Framework** | Jetpack Compose | 1.7.0+ | Declarative UI, Material 3 |
| **Architecture** | MVI + Clean Arch | - | State management, separation of concerns |
| **Dependency Injection** | Hilt | 2.51+ | Compile-time DI, Android optimized |
| **Database** | Room | 2.7.0+ (KMP) | Local persistence, type-safe queries |
| **Preferences** | Proto DataStore | 1.1.0+ | Encrypted settings storage |
| **Async** | Kotlin Coroutines | 1.8+ | Structured concurrency |
| **Reactive Streams** | Flow | (stdlib) | Reactive data streams |
| **Navigation** | Voyager | 1.1.0+ | Compose-first navigation, KMP-ready |

### 3.2 Android System Libraries

| Library | Purpose |
|---------|---------|
| **AlarmManager** | Exact-time reminders for habits/tasks |
| **WorkManager** | Background jobs (backup, sync, widget updates) |
| **NotificationCompat** | Local notifications with channels |
| **RemoteViews** | Home screen widgets |
| **Android Keystore** | Secure key storage for encryption |

### 3.3 UI/UX Libraries

| Library | Purpose | Justification |
|---------|---------|---------------|
| **Material 3 Compose** | UI components | Official MD3 Expressive support |
| **Accompanist** | System UI, Permissions | Compose utilities |
| **Coil** | Image loading | Compose-native, lightweight |
| **Vico** | Charts/graphs | Compose-native charting for statistics |
| **kizitonwose/Calendar** | Calendar views | Best Compose calendar library |

### 3.4 Testing Libraries

| Library | Purpose |
|---------|---------|
| **JUnit 5** | Unit testing framework |
| **Mockk** | Mocking for Kotlin |
| **Turbine** | Testing Kotlin Flows |
| **Compose Test** | UI testing for Compose |
| **Maestro** | E2E testing |
| **Truth** | Fluent assertions |

### 3.5 Code Quality & DevOps

| Tool | Purpose |
|------|---------|
| **Detekt** | Kotlin static analysis |
| **Ktlint** | Kotlin code formatting |
| **Gradle Version Catalog** | Centralized dependency management |
| **GitHub Actions** | CI/CD pipeline |
| **Firebase Crashlytics** | Crash reporting |
| **Firebase Performance** | Performance monitoring |

---

## 4. Design Patterns

### 4.1 MVI (Model-View-Intent) Pattern

**Why MVI over MVVM?**
- Jetpack Compose is declarative  MVI's unidirectional flow is natural fit
- Single immutable state eliminates state desync bugs
- Easier debugging (all state changes via explicit intents)
- Better testability (state transitions are pure functions)

**Structure:**

```kotlin
// State: Single immutable data class
data class HabitsState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now()
)

// Intent: User actions
sealed class HabitsIntent {
    data class LoadHabits(val date: LocalDate) : HabitsIntent()
    data class MarkComplete(val habitId: String, val count: Int) : HabitsIntent()
    data class DeleteHabit(val habitId: String) : HabitsIntent()
}

// ViewModel: Process intents, emit states
class HabitsViewModel @Inject constructor(
    private val getTodaysHabitsUseCase: GetTodaysHabitsUseCase,
    private val updateHabitProgressUseCase: UpdateHabitProgressUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(HabitsState())
    val state: StateFlow<HabitsState> = _state.asStateFlow()
    
    fun processIntent(intent: HabitsIntent) {
        when (intent) {
            is HabitsIntent.LoadHabits -> loadHabits(intent.date)
            is HabitsIntent.MarkComplete -> markComplete(intent.habitId, intent.count)
            is HabitsIntent.DeleteHabit -> deleteHabit(intent.habitId)
        }
    }
    
    private fun loadHabits(date: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getTodaysHabitsUseCase(date)
                .onSuccess { habits ->
                    _state.update { it.copy(habits = habits, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}

// View: Observe state, emit intents
@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
    }
    
    HabitsContent(
        habits = state.habits,
        onHabitClick = { habit ->
            viewModel.processIntent(HabitsIntent.MarkComplete(habit.id, 1))
        }
    )
}
```

### 4.2 Repository Pattern

**Purpose:** Abstract data source details from domain layer.

```kotlin
// Domain layer: Interface
interface HabitRepository {
    suspend fun getHabitById(id: String): Result<Habit>
    suspend fun getTodaysHabits(date: LocalDate): Result<List<Habit>>
    fun observeHabits(): Flow<List<Habit>>
    suspend fun createHabit(habit: Habit): Result<Unit>
    suspend fun updateHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(id: String): Result<Unit>
}

// Data layer: Implementation
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : HabitRepository {
    
    override suspend fun getTodaysHabits(date: LocalDate): Result<Habit> = withContext(dispatcher) {
        try {
            val entities = habitDao.getHabitsForDate(date)
            val habits = entities.map { it.toDomainModel() }
            Result.success(habits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeHabits(): Flow<List<Habit>> {
        return habitDao.observeAllHabits()
            .map { entities -> entities.map { it.toDomainModel() } }
    }
}
```

**Benefits:**
- Domain layer never depends on Room or Android
- Easy to mock for testing
- Can swap Room for Realm/SQLDelight without changing domain

### 4.3 Use Case Pattern

**Purpose:** Encapsulate single business logic operation.

```kotlin
class CalculateStreakUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: String): Result<StreakInfo> {
        return try {
            val logs = habitRepository.getHabitLogs(habitId)
            val streak = calculateConsecutiveDays(logs)
            val longestStreak = calculateLongestStreak(logs)
            
            Result.success(StreakInfo(
                currentStreak = streak,
                longestStreak = longestStreak,
                totalCompletions = logs.count { it.isCompleted }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun calculateConsecutiveDays(logs: List<HabitLog>): Int {
        // Business logic here
        val sortedLogs = logs.sortedByDescending { it.date }
        var streak = 0
        var expectedDate = LocalDate.now()
        
        for (log in sortedLogs) {
            if (log.date == expectedDate && log.isCompleted) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
}
```

**Benefits:**
- Single Responsibility Principle
- Testable in isolation
- Reusable across ViewModels

### 4.4 Mapper Pattern

**Purpose:** Convert between entity types (Database  Domain  UI).

```kotlin
// Data layer entity (Room)
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val goalCount: Int,
    val repeatPattern: String, // Stored as string
    // ... Room-specific fields
)

// Domain model (pure Kotlin)
data class Habit(
    val id: String,
    val title: String,
    val goalCount: Int,
    val repeatPattern: RepeatPattern, // Enum
    // ... business logic fields
)

// Mapper
fun HabitEntity.toDomainModel(): Habit {
    return Habit(
        id = id,
        title = title,
        goalCount = goalCount,
        repeatPattern = RepeatPattern.valueOf(repeatPattern),
    )
}

fun Habit.toEntity(): HabitEntity {
    return HabitEntity(
        id = id,
        title = title,
        goalCount = goalCount,
        repeatPattern = repeatPattern.name,
    )
}
```

---

## 5. State Management

### 5.1 State Flow Architecture

```
User Action (Intent)
        
        
    ViewModel.processIntent()
        
        
    Use Case Execution
        
        
    Repository Query
        
        
    StateFlow Update (immutable state)
        
        
    Compose Recomposition
```

### 5.2 State Types

| State Type | Use Case | Example |
|------------|----------|---------|
| **Screen State** | UI state for a single screen | `HabitsState`, `TasksState` |
| **Navigation State** | Route stack, arguments | Handled by Voyager |
| **Global State** | User settings, theme | `AppSettings` (DataStore) |
| **Cached State** | Recent data (avoid re-fetch) | Repository caching |

### 5.3 Error Handling

**Result Wrapper:**

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// Extension functions
fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
```

**Usage in ViewModel:**

```kotlin
private fun loadHabits() {
    viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        
        getTodaysHabitsUseCase(LocalDate.now())
            .onSuccess { habits ->
                _state.update { it.copy(habits = habits, isLoading = false) }
            }
            .onFailure { error ->
                _state.update { 
                    it.copy(
                        error = error.toUserMessage(), 
                        isLoading = false
                    )
                }
            }
    }
}
```

---

## 6. Navigation Architecture

### 6.1 Voyager Navigation

**Why Voyager over Jetpack Navigation?**
- Type-safe arguments (no string routes)
- Compose-first API
- KMP-ready for future iOS support
- Better nested navigation support

**Structure:**

```kotlin
// Define screens
sealed class Screen : voyager.core.screen.Screen {
    @Composable
    override fun Content() {
        // Implemented by subclasses
    }
    
    object HabitsTab : Screen()
    object TasksTab : Screen()
    object PomodoroTab : Screen()
    data class HabitDetail(val habitId: String) : Screen()
}

// Bottom navigation
@Composable
fun MainScreen() {
    TabNavigator(Screen.HabitsTab) { tabNavigator ->
        Scaffold(
            bottomBar = {
                BottomNavigation {
                    TabNavigationItem(Screen.HabitsTab)
                    TabNavigationItem(Screen.TasksTab)
                    TabNavigationItem(Screen.PomodoroTab)
                }
            }
        ) {
            CurrentTab()
        }
    }
}

// Navigate to detail
val navigator = LocalNavigator.current
Button(onClick = { navigator.push(Screen.HabitDetail(habit.id)) }) {
    Text("View Details")
}
```

---

## 7. Background Operations

### 7.1 Notification System

**Exact Alarm Scheduling (Android 12+):**

```kotlin
class AlarmScheduler @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun scheduleHabitReminder(habit: Habit) {
        if (!habit.reminderEnabled || habit.reminderTime == null) return
        
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habit.id)
            putExtra("HABIT_TITLE", habit.title)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = calculateNextTriggerTime(habit.reminderTime, habit.reminderDays)
        
        // Use exact alarm for critical reminders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback or request permission
                requestExactAlarmPermission()
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
```

### 7.2 WorkManager for Background Tasks

**Daily Backup:**

```kotlin
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val database = HabitaoDatabase.getInstance(applicationContext)
            val backupFile = createBackupFile()
            
            database.close()
            copyDatabaseToBackup(backupFile)
            encryptBackup(backupFile)
            
            cleanOldBackups()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule in Application.onCreate()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "daily_backup",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiresCharging(true)
                .build()
        )
        .build()
)
```

### 7.3 Widget Architecture

**Widget Provider:**

```kotlin
class HabitWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}

class HabitWidget : GlanceAppWidget() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val habits = getHabits(context) // From WorkManager updated cache
        
        GlanceTheme {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text("Today's Habits", style = TextStyle(fontWeight = FontWeight.Bold))
                
                habits.forEach { habit ->
                    HabitWidgetItem(
                        habit = habit,
                        onClick = actionRunCallback<CompleteHabitCallback>(
                            actionParametersOf(KEY_HABIT_ID to habit.id)
                        )
                    )
                }
            }
        }
    }
}

// Update widget data via WorkManager
class WidgetUpdateWorker : CoroutineWorker {
    override suspend fun doWork(): Result {
        val habits = habitRepository.getTodaysHabits()
        saveToWidgetCache(habits)
        
        HabitWidget().updateAll(context)
        return Result.success()
    }
}
```

---

## 8. Security Architecture

### 8.1 Data Encryption

**Proto DataStore with Tink:**

```kotlin
object SecurityUtil {
    fun getOrCreateMasterKey(context: Context): String {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "habitao_keyset", "habitao_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://habitao_master_key")
            .build()
            .keysetHandle
        
        val aead = keysetHandle.getPrimitive(Aead::class.java)
        return Base64.encodeToString(aead.encrypt(...), Base64.DEFAULT)
    }
}

// Proto DataStore Serializer with encryption
object EncryptedSettingsSerializer : Serializer<AppSettings> {
    private val aead: Aead by lazy { getAeadPrimitive() }
    
    override suspend fun readFrom(input: InputStream): AppSettings {
        val encryptedData = input.readBytes()
        val decryptedData = aead.decrypt(encryptedData, null)
        return AppSettings.parseFrom(decryptedData)
    }
    
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        val plainData = t.toByteArray()
        val encryptedData = aead.encrypt(plainData, null)
        output.write(encryptedData)
    }
}
```

### 8.2 Permissions

**Required Permissions:**

```xml
<!-- Exact alarms for reminders -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Foreground service for Pomodoro -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Boot receiver to reschedule alarms -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Vibration for notifications -->
<uses-permission android:name="android.permission.VIBRATE" />
```

**Runtime Permission Handling:**

```kotlin
@Composable
fun RequestNotificationPermission() {
    val permissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS
    )
    
    if (!permissionState.status.isGranted) {
        PermissionRationaleDialog(
            onConfirm = { permissionState.launchPermissionRequest() }
        )
    }
}
```

---

## 9. Performance Optimizations

### 9.1 Database Optimization

**Lazy Loading:**
```kotlin
@Dao
interface HabitDao {
    // Use Flow for reactive updates
    @Query("SELECT * FROM habits WHERE isArchived = 0 LIMIT :limit OFFSET :offset")
    fun observeHabitsPaged(limit: Int, offset: Int): Flow<List<HabitEntity>>
}
```

**Indexing:**
- Index `nextScheduledDate` for "today's habits" query
- Index `dueDate` for "overdue tasks" query
- Composite index `(habitId, date)` for logs

### 9.2 UI Performance

**LazyColumn with Keys:**
```kotlin
LazyColumn {
    items(
        items = habits,
        key = { it.id } // Stable key for efficient recomposition
    ) { habit ->
        HabitCard(habit = habit)
    }
}
```

**Remember Expensive Calculations:**
```kotlin
@Composable
fun StatisticsScreen(logs: List<HabitLog>) {
    val completionRate = remember(logs) {
        logs.count { it.isCompleted } / logs.size.toFloat()
    }
}
```

### 9.3 Memory Management

**Paging for Large Lists:**
```kotlin
val habitsPager = Pager(
    config = PagingConfig(pageSize = 20),
    pagingSourceFactory = { habitDao.getHabitsPagingSource() }
).flow.cachedIn(viewModelScope)
```

---

## 10. Testing Strategy

### 10.1 Test Pyramid

```
        
            E2E       10% - Maestro (critical user flows)
          (Maestro) 
        
     
       Integration     20% - Repository + Room, ViewModel + UseCase
        (JUnit +     
         Room Test)  
     
  
     Unit Tests         70% - Use Cases, ViewModels (mocked repos)
    (JUnit + Mockk)   
  
```

### 10.2 Test Examples

**Unit Test (Use Case):**
```kotlin
class CalculateStreakUseCaseTest {
    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: CalculateStreakUseCase
    
    @BeforeEach
    fun setup() {
        habitRepository = mockk()
        useCase = CalculateStreakUseCase(habitRepository)
    }
    
    @Test
    fun `consecutive completed days returns correct streak`() = runTest {
        // Given
        val logs = listOf(
            HabitLog(date = LocalDate.now(), isCompleted = true),
            HabitLog(date = LocalDate.now().minusDays(1), isCompleted = true),
            HabitLog(date = LocalDate.now().minusDays(2), isCompleted = true)
        )
        coEvery { habitRepository.getHabitLogs(any()) } returns Result.success(logs)
        
        // When
        val result = useCase("habit-123")
        
        // Then
        assertThat(result.getOrNull()?.currentStreak).isEqualTo(3)
    }
}
```

**Integration Test (Repository + Room):**
```kotlin
@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {
    private lateinit var database: HabitaoDatabase
    private lateinit var repository: HabitRepositoryImpl
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitaoDatabase::class.java
        ).build()
        
        repository = HabitRepositoryImpl(database.habitDao())
    }
    
    @Test
    fun getTodaysHabits_returnsOnlyScheduledHabits() = runTest {
        // Given
        val habit1 = Habit(id = "1", nextScheduledDate = LocalDate.now())
        val habit2 = Habit(id = "2", nextScheduledDate = LocalDate.now().plusDays(1))
        repository.createHabit(habit1)
        repository.createHabit(habit2)
        
        // When
        val result = repository.getTodaysHabits(LocalDate.now())
        
        // Then
        assertThat(result.getOrNull()).containsExactly(habit1)
    }
}
```

**E2E Test (Maestro):**
```yaml
appId: com.habitao.app
---
- launchApp
- tapOn: "Add Habit"
- inputText: "Drink Water"
- tapOn: "Goal Count"
- inputText: "8"
- tapOn: "Save"
- assertVisible: "Drink Water"
- tapOn:
    point: "80%,50%"  # Tap completion button
- assertVisible: "1/8"
```

---

## 11. Build Configuration

### 11.1 Gradle Version Catalog

**`gradle/libs.versions.toml`:**
```toml
[versions]
kotlin = "2.0.0"
compose = "1.7.0"
room = "2.7.0"
hilt = "2.51"

[libraries]
androidx-core = { group = "androidx.core", name = "core-ktx", version = "1.13.0" }
compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "compose" }
compose-material3 = { group = "androidx.compose.material3", name = "material3", version = "1.4.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }

[plugins]
android-application = { id = "com.android.application", version = "8.3.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 11.2 Build Types

```kotlin
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isMinifyEnabled = false
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

**End of Technical Architecture Document**
