# Testing Strategy
## Habitao - Comprehensive Test Plan

**Version:** 1.0  
**Last Updated:** February 13, 2026  
**Test Coverage Target:** 85% overall (90% domain, 80% data, 60% UI)

---

## 1. Testing Philosophy

### 1.1 Principles
1. **Test Behavior, Not Implementation:** Focus on what code does, not how
2. **Fast Feedback:** Unit tests must run in < 1 second total
3. **Maintainable:** Tests should be as clean as production code
4. **Confidence:** High test coverage ≠ bug-free; test critical paths thoroughly
5. **Shift Left:** Catch bugs early (unit tests) rather than late (E2E)

### 1.2 Test Pyramid

```
           ┌──────────────┐
           │     E2E      │  10% (~15 tests)
           │  (Maestro)   │  Critical user flows only
           └──────────────┘  Slow, brittle, expensive
        ┌────────────────────┐
        │   Integration      │  20% (~50 tests)
        │ (Repository+Room,  │  Verify layers work together
        │  ViewModel+UseCase)│  Medium speed
        └────────────────────┘
     ┌────────────────────────┐
     │      Unit Tests        │  70% (~200 tests)
     │  (Use Cases, ViewModels│  Isolated, mocked dependencies
     │   Utilities, Mappers)  │  Fast, reliable
     └────────────────────────┘
```

**Rationale:**
- **70% Unit:** Fast, isolate failures, run on every commit
- **20% Integration:** Catch interface mismatches, DB queries, state management bugs
- **10% E2E:** Verify critical paths work end-to-end (happy paths only)

---

## 2. Unit Testing

### 2.1 Scope
- ✅ Use Cases (domain/usecase/*)
- ✅ ViewModels (feature/*/viewmodel/*)
- ✅ Repositories (data/repository/* - mocked DAO)
- ✅ Mappers (entity ↔ domain model converters)
- ✅ Utilities (DateTimeUtils, StringUtils)

### 2.2 Framework

**JUnit 5 + Mockk + Turbine + Truth**

```kotlin
// build.gradle.kts (module: domain)
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0") // Test Kotlin Flow
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
```

### 2.3 Example: Use Case Test

**Test:** `CalculateStreakUseCaseTest.kt`

```kotlin
@ExperimentalCoroutinesApi
class CalculateStreakUseCaseTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: CalculateStreakUseCase
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = mockk()
        useCase = CalculateStreakUseCase(habitRepository)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `consecutive completed days returns correct streak`() = testScope.runTest {
        // Given
        val habitId = "habit-123"
        val logs = listOf(
            HabitLog(date = LocalDate.now(), isCompleted = true),
            HabitLog(date = LocalDate.now().minusDays(1), isCompleted = true),
            HabitLog(date = LocalDate.now().minusDays(2), isCompleted = true),
            HabitLog(date = LocalDate.now().minusDays(3), isCompleted = false) // Breaks streak
        )
        coEvery { habitRepository.getHabitLogs(habitId) } returns Result.success(logs)
        
        // When
        val result = useCase(habitId)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.currentStreak).isEqualTo(3)
        assertThat(result.getOrNull()?.longestStreak).isEqualTo(3)
    }
    
    @Test
    fun `missing day breaks streak`() = testScope.runTest {
        // Given
        val habitId = "habit-456"
        val logs = listOf(
            HabitLog(date = LocalDate.now(), isCompleted = true),
            // Missing yesterday
            HabitLog(date = LocalDate.now().minusDays(2), isCompleted = true)
        )
        coEvery { habitRepository.getHabitLogs(habitId) } returns Result.success(logs)
        
        // When
        val result = useCase(habitId)
        
        // Then
        assertThat(result.getOrNull()?.currentStreak).isEqualTo(1) // Only today
    }
    
    @Test
    fun `repository failure returns error result`() = testScope.runTest {
        // Given
        val exception = IOException("Database error")
        coEvery { habitRepository.getHabitLogs(any()) } returns Result.failure(exception)
        
        // When
        val result = useCase("habit-789")
        
        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }
}
```

### 2.4 Example: ViewModel Test

**Test:** `HabitsViewModelTest.kt`

```kotlin
@ExperimentalCoroutinesApi
class HabitsViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var getTodaysHabitsUseCase: GetTodaysHabitsUseCase
    private lateinit var updateHabitProgressUseCase: UpdateHabitProgressUseCase
    private lateinit var viewModel: HabitsViewModel
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getTodaysHabitsUseCase = mockk()
        updateHabitProgressUseCase = mockk()
        viewModel = HabitsViewModel(getTodaysHabitsUseCase, updateHabitProgressUseCase)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `load habits intent updates state with habits`() = testScope.runTest {
        // Given
        val habits = listOf(
            Habit(id = "1", title = "Drink Water", goalCount = 8),
            Habit(id = "2", title = "Exercise", goalCount = 1)
        )
        coEvery { getTodaysHabitsUseCase(any()) } returns Result.success(habits)
        
        // When
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
        testScheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.habits).isEqualTo(habits)
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
        }
    }
    
    @Test
    fun `mark complete intent updates habit progress`() = testScope.runTest {
        // Given
        val habitId = "habit-123"
        val count = 3
        coEvery { updateHabitProgressUseCase(habitId, count) } returns Result.success(Unit)
        
        // When
        viewModel.processIntent(HabitsIntent.MarkComplete(habitId, count))
        testScheduler.advanceUntilIdle()
        
        // Then
        coVerify { updateHabitProgressUseCase(habitId, count) }
    }
    
    @Test
    fun `error during load shows error state`() = testScope.runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { getTodaysHabitsUseCase(any()) } returns Result.failure(exception)
        
        // When
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
        testScheduler.advanceUntilIdle()
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.error).isNotNull()
            assertThat(state.isLoading).isFalse()
        }
    }
}
```

### 2.5 Coverage Target

| Layer | Target | Rationale |
|-------|--------|-----------|
| **Domain (Use Cases)** | 90%+ | Business logic must be bulletproof |
| **Data (Repositories)** | 80%+ | Critical data operations |
| **Presentation (ViewModels)** | 70%+ | State management verification |
| **Utilities** | 90%+ | Pure functions, easy to test |

---

## 3. Integration Testing

### 3.1 Scope
- ✅ Repository + Room DAO (verify SQL queries work)
- ✅ ViewModel + Use Case (verify layers integrate correctly)
- ✅ DataStore + Serialization (verify proto serialization)

### 3.2 Framework

**AndroidJUnit4 + Room In-Memory Database**

```kotlin
// build.gradle.kts (module: data)
dependencies {
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.room:room-testing:2.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
```

### 3.3 Example: Repository + Room Test

**Test:** `HabitRepositoryImplTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class HabitRepositoryImplTest {
    
    private lateinit var database: HabitaoDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var habitLogDao: HabitLogDao
    private lateinit var repository: HabitRepositoryImpl
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            HabitaoDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()
        
        habitDao = database.habitDao()
        habitLogDao = database.habitLogDao()
        repository = HabitRepositoryImpl(habitDao, habitLogDao, Dispatchers.IO)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun getTodaysHabits_returnsOnlyScheduledHabits() = runBlocking {
        // Given: Insert habits with different scheduled dates
        val today = LocalDate.now()
        val habit1 = Habit(
            id = "1", 
            title = "Today's habit",
            nextScheduledDate = today
        )
        val habit2 = Habit(
            id = "2",
            title = "Tomorrow's habit",
            nextScheduledDate = today.plusDays(1)
        )
        val habit3 = Habit(
            id = "3",
            title = "Archived habit",
            nextScheduledDate = today,
            isArchived = true
        )
        
        repository.createHabit(habit1)
        repository.createHabit(habit2)
        repository.createHabit(habit3)
        
        // When: Get today's habits
        val result = repository.getTodaysHabits(today)
        
        // Then: Only habit1 should be returned
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(1)
        assertThat(result.getOrNull()?.first()?.id).isEqualTo("1")
    }
    
    @Test
    fun observeHabits_emitsUpdatesOnInsert() = runBlocking {
        // Given
        val flow = repository.observeHabits()
        
        // When: Collect first emission (empty)
        val emissions = mutableListOf<List<Habit>>()
        val job = launch {
            flow.take(2).toList(emissions)
        }
        
        yield() // Allow collection to start
        
        // Insert a habit
        repository.createHabit(Habit(id = "1", title = "New Habit"))
        
        job.join()
        
        // Then: Should have 2 emissions (empty, then with new habit)
        assertThat(emissions).hasSize(2)
        assertThat(emissions[0]).isEmpty()
        assertThat(emissions[1]).hasSize(1)
    }
}
```

### 3.4 Example: ViewModel + Use Case Integration

**Test:** `HabitsViewModelIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class HabitsViewModelIntegrationTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: HabitaoDatabase
    private lateinit var repository: HabitRepository
    private lateinit var viewModel: HabitsViewModel
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitaoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        repository = HabitRepositoryImpl(database.habitDao(), database.habitLogDao())
        
        val getTodaysHabitsUseCase = GetTodaysHabitsUseCase(repository)
        val updateProgressUseCase = UpdateHabitProgressUseCase(repository)
        
        viewModel = HabitsViewModel(getTodaysHabitsUseCase, updateProgressUseCase)
    }
    
    @Test
    fun endToEnd_createHabitAndLoadInViewModel() = runBlocking {
        // Given: Create habit via repository
        val habit = Habit(id = "1", title = "Drink Water", goalCount = 8)
        repository.createHabit(habit)
        
        // When: Load habits in ViewModel
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
        
        // Then: ViewModel state should contain the habit
        delay(100) // Allow async operation to complete
        assertThat(viewModel.state.value.habits).contains(habit)
    }
}
```

---

## 4. UI Testing (Compose)

### 4.1 Scope
- ✅ Individual Composables (HabitCard, TaskItem)
- ✅ Screen-level UI (HabitsScreen rendering)
- ✅ User interactions (click, swipe, input)
- ✅ State changes (loading → loaded → error)

### 4.2 Framework

**Compose Test + Turbine**

```kotlin
// build.gradle.kts (feature module)
dependencies {
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
}
```

### 4.3 Example: Composable Test

**Test:** `HabitCardTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class HabitCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun habitCard_displaysHabitInformation() {
        // Given
        val habit = Habit(
            id = "1",
            title = "Drink Water",
            goalCount = 8,
            icon = "💧"
        )
        
        // When
        composeTestRule.setContent {
            HabitCard(
                habit = habit,
                currentProgress = 5,
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Drink Water").assertExists()
        composeTestRule.onNodeWithText("💧").assertExists()
        composeTestRule.onNodeWithText("5/8").assertExists()
    }
    
    @Test
    fun habitCard_clickTriggersCallback() {
        // Given
        val habit = Habit(id = "1", title = "Exercise")
        var clickedHabitId: String? = null
        
        // When
        composeTestRule.setContent {
            HabitCard(
                habit = habit,
                currentProgress = 0,
                onClick = { clickedHabitId = it }
            )
        }
        
        composeTestRule.onNodeWithText("Exercise").performClick()
        
        // Then
        assertThat(clickedHabitId).isEqualTo("1")
    }
    
    @Test
    fun habitCard_completedState_showsCheckmark() {
        // Given
        val habit = Habit(id = "1", title = "Meditate", goalCount = 1)
        
        // When
        composeTestRule.setContent {
            HabitCard(
                habit = habit,
                currentProgress = 1, // Completed
                onClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Completed").assertExists()
    }
}
```

### 4.4 Example: Screen Test

**Test:** `HabitsScreenTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class HabitsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var viewModel: HabitsViewModel
    
    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
    }
    
    @Test
    fun habitsScreen_showsLoadingState() {
        // Given
        every { viewModel.state } returns MutableStateFlow(
            HabitsState(isLoading = true)
        )
        
        // When
        composeTestRule.setContent {
            HabitsScreen(viewModel = viewModel)
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }
    
    @Test
    fun habitsScreen_showsHabitsList() {
        // Given
        val habits = listOf(
            Habit(id = "1", title = "Habit 1"),
            Habit(id = "2", title = "Habit 2")
        )
        every { viewModel.state } returns MutableStateFlow(
            HabitsState(habits = habits, isLoading = false)
        )
        
        // When
        composeTestRule.setContent {
            HabitsScreen(viewModel = viewModel)
        }
        
        // Then
        composeTestRule.onNodeWithText("Habit 1").assertExists()
        composeTestRule.onNodeWithText("Habit 2").assertExists()
    }
    
    @Test
    fun habitsScreen_clickingAddFab_triggersNavigation() {
        // Given
        every { viewModel.state } returns MutableStateFlow(HabitsState())
        
        // When
        composeTestRule.setContent {
            HabitsScreen(viewModel = viewModel)
        }
        
        composeTestRule.onNodeWithContentDescription("Add Habit").performClick()
        
        // Then: Verify navigation intent sent (implementation-specific)
    }
}
```

---

## 5. End-to-End (E2E) Testing

### 5.1 Scope

**ONLY Critical User Flows (Happy Paths):**
- ✅ Create habit → Mark complete → Verify streak
- ✅ Create task with subtask → Mark subtask complete
- ✅ Create routine → Complete all steps → Verify routine done
- ✅ Start Pomodoro timer → Wait completion → Verify notification
- ✅ Widget interaction → Mark habit complete from widget

### 5.2 Framework: Maestro

**Why Maestro over Espresso?**
- YAML-based (easier to maintain than Kotlin/Java code)
- Less flaky (waits for elements automatically)
- Works outside app context (can test system dialogs, widgets)
- Faster to write

**Installation:**
```bash
# Install Maestro CLI
curl -Ls "https://get.maestro.mobile.dev" | bash

# Verify installation
maestro --version
```

### 5.3 Example: E2E Flow

**File:** `.maestro/create-and-complete-habit.yaml`

```yaml
appId: com.habitao.app
---
# Flow: Create habit and mark it complete

- launchApp
- assertVisible: "Habits" # Bottom nav

# Navigate to Add Habit screen
- tapOn: "Add Habit"
- assertVisible: "Create New Habit"

# Fill out habit form
- tapOn: "Habit Title"
- inputText: "Drink 8 Glasses of Water"

- tapOn: "Goal Count"
- inputText: "8"

- tapOn: "Unit (Optional)"
- inputText: "glasses"

# Save habit
- tapOn: "Save"
- assertVisible: "Drink 8 Glasses of Water"

# Mark habit complete (increment progress)
- tapOn:
    id: "habit_increment_button"  # Content description or test tag
- assertVisible: "1/8"

# Increment again
- tapOn:
    id: "habit_increment_button"
- assertVisible: "2/8"

# Verify habit card shows updated progress
- assertVisible: "2 of 8 glasses completed"
```

**File:** `.maestro/widget-interaction.yaml`

```yaml
appId: com.habitao.app
---
# Flow: Mark habit complete from home screen widget

- launchApp
- pressKey: Home  # Go to home screen
- assertVisible: "Habitao Widget"  # Widget title

# Tap checkbox on widget habit item
- tapOn: "Drink Water"  # Widget item
- assertVisible: "✓"  # Checkmark appears

# Reopen app to verify state persisted
- launchApp
- assertVisible: "Drink Water"
- assertVisible: "1/8"  # Progress updated
```

### 5.4 Running E2E Tests

**Locally:**
```bash
# Run single flow
maestro test .maestro/create-and-complete-habit.yaml

# Run all flows in directory
maestro test .maestro/

# Run with Android Emulator
maestro test --device emulator-5554 .maestro/
```

**CI Integration (GitHub Actions):**
```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests

on:
  pull_request:
  push:
    branches: [dev, main]

jobs:
  maestro-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
      
      - name: Build Release APK
        run: ./gradlew assembleDebug
      
      - name: Set up Maestro
        run: curl -Ls "https://get.maestro.mobile.dev" | bash
      
      - name: Start Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: |
            adb install app/build/outputs/apk/debug/app-debug.apk
            maestro test .maestro/
```

---

## 6. Test Organization

### 6.1 Directory Structure

```
habitao/
├── domain/
│   └── src/
│       └── test/                    # Unit tests
│           └── kotlin/
│               └── usecase/
│                   ├── CalculateStreakUseCaseTest.kt
│                   └── CreateHabitUseCaseTest.kt
│
├── data/
│   └── src/
│       ├── test/                    # Unit tests (mocked DAO)
│       │   └── repository/
│       │       └── HabitRepositoryImplTest.kt
│       └── androidTest/             # Integration tests (real Room)
│           └── repository/
│               └── HabitRepositoryIntegrationTest.kt
│
├── feature/habits/
│   └── src/
│       ├── test/                    # Unit tests (ViewModel)
│       │   └── viewmodel/
│       │       └── HabitsViewModelTest.kt
│       └── androidTest/             # UI tests (Compose)
│           └── ui/
│               ├── HabitsScreenTest.kt
│               └── components/
│                   └── HabitCardTest.kt
│
└── .maestro/                        # E2E tests
    ├── create-and-complete-habit.yaml
    ├── task-with-subtasks.yaml
    ├── routine-completion.yaml
    └── pomodoro-timer.yaml
```

### 6.2 Naming Conventions

**Test Classes:**
- `{ClassName}Test.kt` for unit tests
- `{ClassName}IntegrationTest.kt` for integration tests
- `{ClassName}E2ETest.kt` for E2E tests (if using Kotlin)

**Test Methods:**
```kotlin
@Test
fun `methodName_stateUnderTest_expectedBehavior`() {
    // given
    // when
    // then
}
```

**Examples:**
- `getTodaysHabits_whenHabitsExist_returnsHabitsList()`
- `calculateStreak_withConsecutiveDays_returnsCorrectCount()`
- `habitCard_whenCompleted_showsCheckmark()`

---

## 7. CI/CD Integration

### 7.1 GitHub Actions Workflow

**File:** `.github/workflows/tests.yml`

```yaml
name: Test Suite

on:
  pull_request:
  push:
    branches: [dev, main]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
      
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
      
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
  
  integration-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
      
      - name: Run Integration Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: ./gradlew connectedDebugAndroidTest
  
  e2e-tests:
    runs-on: macos-latest
    needs: [unit-tests, integration-tests]
    steps:
      - uses: actions/checkout@v4
      
      - name: Build APK
        run: ./gradlew assembleDebug
      
      - name: Run Maestro Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: |
            curl -Ls "https://get.maestro.mobile.dev" | bash
            adb install app/build/outputs/apk/debug/app-debug.apk
            maestro test .maestro/
```

### 7.2 Quality Gates

**Fail PR if:**
- [ ] Any unit test fails
- [ ] Code coverage drops below 80%
- [ ] Any integration test fails
- [ ] Any E2E test fails (on critical flows only)
- [ ] Detekt finds violations
- [ ] Ktlint finds formatting issues

---

## 8. Test Data Management

### 8.1 Test Fixtures

**File:** `core/testing/src/main/kotlin/TestData.kt`

```kotlin
object TestHabits {
    fun drinkWater(id: String = "habit-1") = Habit(
        id = id,
        title = "Drink Water",
        goalCount = 8,
        unit = "glasses",
        repeatPattern = RepeatPattern.DAILY,
        startDate = LocalDate.now(),
        nextScheduledDate = LocalDate.now()
    )
    
    fun exercise(id: String = "habit-2") = Habit(
        id = id,
        title = "Exercise",
        goalCount = 30,
        unit = "minutes",
        repeatPattern = RepeatPattern.DAILY,
        startDate = LocalDate.now(),
        nextScheduledDate = LocalDate.now()
    )
}

object TestTasks {
    fun simpleTask(id: String = "task-1") = Task(
        id = id,
        title = "Prepare presentation",
        dueDate = LocalDate.now().plusDays(1),
        priority = TaskPriority.HIGH
    )
}
```

**Usage:**
```kotlin
@Test
fun `test with fixture data`() {
    val habit = TestHabits.drinkWater()
    repository.createHabit(habit)
    // ...
}
```

---

## 9. Performance Testing

### 9.1 Scope
- Database query performance (large datasets)
- UI rendering performance (LazyColumn with 1000+ items)
- Memory leaks (ViewModel, Composables)

### 9.2 Benchmarking

**Macrobenchmark (Startup Time):**

```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun startupNoCompilation() {
        benchmarkRule.measureRepeated(
            packageName = "com.habitao.app",
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
```

**Target:** < 1.5s cold start

---

## 10. Test Maintenance

### 10.1 Avoiding Flaky Tests

**Common Causes:**
- Time-dependent logic (use `Clock` interface)
- Async operations (use `runBlocking`, `advanceUntilIdle()`)
- Hardcoded IDs (use test tags in Compose)
- Shared mutable state (reset in `@BeforeEach`)

**Best Practices:**
- Use `TestCoroutineDispatcher` for deterministic coroutine execution
- Mock time-dependent code (inject `Clock`)
- Avoid `Thread.sleep()` (use `advanceTimeBy()` in tests)

### 10.2 Test Review Checklist

- [ ] Test name clearly describes what is tested
- [ ] Given-When-Then structure used
- [ ] Single assertion per test (or logically grouped assertions)
- [ ] No hardcoded values (use constants or test data objects)
- [ ] Tests are independent (no execution order dependency)
- [ ] Fast execution (unit tests < 100ms each)

---

**End of Testing Strategy Document**
