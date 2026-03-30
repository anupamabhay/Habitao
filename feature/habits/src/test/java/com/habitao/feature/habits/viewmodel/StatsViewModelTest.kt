package com.habitao.feature.habits.viewmodel

import com.habitao.core.datastore.AppSettings
import com.habitao.core.datastore.AppSettingsRepository
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.HabitType
import com.habitao.domain.model.PomodoroSession
import com.habitao.domain.model.StreakInfo
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.domain.repository.RoutineRepository
import com.habitao.domain.repository.TaskRepository
import com.habitao.feature.pomodoro.preferences.PomodoroPreferencesSource
import com.habitao.feature.pomodoro.service.TimerStateHolder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var habitRepository: HabitRepository
    private lateinit var pomodoroRepository: PomodoroRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var routineRepository: RoutineRepository
    private lateinit var pomodoroPreferences: PomodoroPreferencesSource
    private lateinit var appSettingsRepository: AppSettingsRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = mockk()
        pomodoroRepository = mockk()
        taskRepository = mockk()
        routineRepository = mockk()
        pomodoroPreferences = mockk()
        appSettingsRepository = mockk()

        every { pomodoroRepository.observeSessionsForDateRange(any(), any()) } returns flowOf(Result.success(emptyList<PomodoroSession>()))
        every { taskRepository.observeTasksForDateRange(any(), any()) } returns flowOf(Result.success(emptyList()))
        every { routineRepository.observeAllRoutines() } returns flowOf(Result.success(emptyList()))
        every { routineRepository.observeRoutineLogsForDateRange(any(), any()) } returns flowOf(Result.success(emptyList()))
        every { pomodoroPreferences.getTodaysRounds() } returns 0
        every { appSettingsRepository.settings } returns flowOf(AppSettings())
    }

    @Test
    fun `one day streak should appear immediately when logs update`() =
        runTest {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val habit = createTestHabit()
            val habitsFlow = MutableStateFlow(Result.success(listOf(habit)))
            val logsFlow = MutableStateFlow(Result.success(emptyList<HabitLog>()))
            var streakInfo = StreakInfo(0, 0, 0)

            every { habitRepository.observeAllHabits() } returns habitsFlow
            every { habitRepository.observeLogsForDateRange(any(), any()) } returns logsFlow
            coEvery { habitRepository.calculateStreak(habit.id) } answers { Result.success(streakInfo) }

            val viewModel =
                StatsViewModel(
                    habitRepository = habitRepository,
                    pomodoroRepository = pomodoroRepository,
                    taskRepository = taskRepository,
                    routineRepository = routineRepository,
                    timerStateHolder = TimerStateHolder(),
                    pomodoroPreferences = pomodoroPreferences,
                    appSettingsManager = appSettingsRepository,
                )

            advanceUntilIdle()

            assertTrue(viewModel.state.value.habitStats.isEmpty())
            assertEquals(0, viewModel.state.value.currentBestStreak)

            streakInfo = StreakInfo(currentStreak = 1, longestStreak = 1, totalCompletions = 1)
            logsFlow.value =
                Result.success(
                    listOf(
                        HabitLog(
                            id = "log-1",
                            habitId = habit.id,
                            date = today,
                            currentValue = 1,
                            targetValue = 1,
                            isCompleted = true,
                            completedAt = Clock.System.now().toEpochMilliseconds(),
                        ),
                    ),
                )

            advanceUntilIdle()

            val updatedState = viewModel.state.value
            assertEquals(1, updatedState.currentBestStreak)
            assertEquals(1, updatedState.habitStats.size)
            assertEquals(1, updatedState.habitStats.first().currentStreak)
            assertTrue(updatedState.habitStats.first().isCompletedToday)
            assertFalse(updatedState.isLoading)
        }

    private fun createTestHabit(): Habit =
        Habit(
            id = "habit-1",
            title = "Read",
            description = null,
            habitType = HabitType.SIMPLE,
            targetValue = 1,
            unit = null,
            frequencyType = FrequencyType.DAILY,
            frequencyValue = 1,
            scheduledDays = DayOfWeek.entries.toSet(),
            reminderEnabled = false,
            reminderTime = null,
            color = "#4CAF50",
            icon = "book",
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            sortOrder = 0,
            isArchived = false,
        )
}
