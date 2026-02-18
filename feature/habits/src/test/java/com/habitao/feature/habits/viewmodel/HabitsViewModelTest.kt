package com.habitao.feature.habits.viewmodel

import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.HabitType
import com.habitao.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for HabitsViewModel.
 * The ViewModel uses reactive Flows to automatically load habits when the selected date changes,
 * so we test by observing state changes after setting up mock Flow emissions.
 */
@ExperimentalCoroutinesApi
class HabitsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var habitRepository: HabitRepository
    private lateinit var viewModel: HabitsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = mockk()

        // Setup default mock responses for Flow observations
        every { habitRepository.observeHabitsForDate(any()) } returns flowOf(Result.success(emptyList()))
        every { habitRepository.observeLogsForDate(any()) } returns flowOf(Result.success(emptyMap()))
    }

    @Test
    fun `initial state should have empty habits and be loading`() =
        runTest {
            // Given
            viewModel = HabitsViewModel(habitRepository)

            // Then - initial state before flows emit
            val initialState = viewModel.state.value
            assertTrue(initialState.habits.isEmpty())
            assertTrue(initialState.isLoading)
            assertEquals(LocalDate.now(), initialState.selectedDate)
        }

    @Test
    fun `state should update when habits flow emits`() =
        runTest {
            // Given
            val testHabits = listOf(createTestHabit())
            every { habitRepository.observeHabitsForDate(any()) } returns flowOf(Result.success(testHabits))

            // When
            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // Then
            val state = viewModel.state.value
            assertEquals(1, state.habits.size)
            assertEquals("Test Habit", state.habits[0].title)
            assertFalse(state.isLoading)
        }

    @Test
    fun `selectDate should trigger new habit load`() =
        runTest {
            // Given
            val todayHabits = listOf(createTestHabit("today-id", "Today Habit"))
            val tomorrowHabits = listOf(createTestHabit("tomorrow-id", "Tomorrow Habit"))

            every { habitRepository.observeHabitsForDate(LocalDate.now()) } returns
                flowOf(Result.success(todayHabits))
            every { habitRepository.observeHabitsForDate(LocalDate.now().plusDays(1)) } returns
                flowOf(Result.success(tomorrowHabits))

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // Verify initial state
            assertEquals("Today Habit", viewModel.state.value.habits.firstOrNull()?.title)

            // When - select a different date
            viewModel.processIntent(HabitsIntent.SelectDate(LocalDate.now().plusDays(1)))
            advanceUntilIdle()

            // Then
            val state = viewModel.state.value
            assertEquals(LocalDate.now().plusDays(1), state.selectedDate)
            assertEquals("Tomorrow Habit", state.habits.firstOrNull()?.title)
        }

    @Test
    fun `clearError should remove error from state`() =
        runTest {
            // Given
            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.clearError()

            // Then
            assertEquals(null, viewModel.state.value.error)
        }

    @Test
    fun `logs should be associated with habits`() =
        runTest {
            // Given
            val habit = createTestHabit()
            val log =
                HabitLog(
                    id = "log-1",
                    habitId = habit.id,
                    date = LocalDate.now(),
                    currentValue = 3,
                    targetValue = 5,
                    isCompleted = false,
                )

            every { habitRepository.observeHabitsForDate(any()) } returns flowOf(Result.success(listOf(habit)))
            every { habitRepository.observeLogsForDate(any()) } returns flowOf(Result.success(mapOf(habit.id to log)))

            // When
            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // Then
            val state = viewModel.state.value
            assertEquals(1, state.logs.size)
            assertEquals(3, state.logs[habit.id]?.currentValue)
        }

    @Test
    fun `incrementHabitProgress should increase current value by 1`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val date = LocalDate.now()
            val log =
                HabitLog(
                    id = "log-id",
                    habitId = habitId,
                    date = date,
                    currentValue = 3,
                    targetValue = 5,
                    isCompleted = false,
                )

            coEvery { habitRepository.getLogForHabitAndDate(habitId, date) } returns Result.success(log)
            coEvery { habitRepository.createOrUpdateLog(any(), any(), any()) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.IncrementHabitProgress(habitId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.createOrUpdateLog(habitId, date, 4) }
        }

    @Test
    fun `incrementHabitProgress should start from 0 when no log exists`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val date = LocalDate.now()

            coEvery { habitRepository.getLogForHabitAndDate(habitId, date) } returns Result.success(null)
            coEvery { habitRepository.createOrUpdateLog(any(), any(), any()) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.IncrementHabitProgress(habitId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.createOrUpdateLog(habitId, date, 1) }
        }

    @Test
    fun `decrementHabitProgress should decrease current value by 1`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val date = LocalDate.now()
            val log =
                HabitLog(
                    id = "log-id",
                    habitId = habitId,
                    date = date,
                    currentValue = 3,
                    targetValue = 5,
                    isCompleted = false,
                )

            coEvery { habitRepository.getLogForHabitAndDate(habitId, date) } returns Result.success(log)
            coEvery { habitRepository.createOrUpdateLog(any(), any(), any()) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.DecrementHabitProgress(habitId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.createOrUpdateLog(habitId, date, 2) }
        }

    @Test
    fun `decrementHabitProgress should not go below 0`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val date = LocalDate.now()
            val log =
                HabitLog(
                    id = "log-id",
                    habitId = habitId,
                    date = date,
                    currentValue = 0,
                    targetValue = 5,
                    isCompleted = false,
                )

            coEvery { habitRepository.getLogForHabitAndDate(habitId, date) } returns Result.success(log)
            coEvery { habitRepository.createOrUpdateLog(any(), any(), any()) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.DecrementHabitProgress(habitId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.createOrUpdateLog(habitId, date, 0) }
        }

    @Test
    fun `markHabitComplete should call createOrUpdateLog with correct params`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val date = LocalDate.now()

            coEvery { habitRepository.createOrUpdateLog(any(), any(), any()) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.MarkHabitComplete(habitId, 4))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.createOrUpdateLog(habitId, date, 4) }
        }

    @Test
    fun `markHabitComplete failure should set error in state`() =
        runTest {
            // Given
            val habitId = "habit-id"

            coEvery { habitRepository.createOrUpdateLog(any(), any(), any()) } returns
                Result.failure(Exception("error"))

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.MarkHabitComplete(habitId, 4))
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.state.value.error != null)
        }

    @Test
    fun `deleteHabit should call repository deleteHabit`() =
        runTest {
            // Given
            val habitId = "habit-id"

            coEvery { habitRepository.deleteHabit(habitId) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.DeleteHabit(habitId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.deleteHabit(habitId) }
        }

    @Test
    fun `deleteHabit failure should set error in state`() =
        runTest {
            // Given
            val habitId = "habit-id"

            coEvery { habitRepository.deleteHabit(habitId) } returns Result.failure(Exception("error"))

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.DeleteHabit(habitId))
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.state.value.error != null)
        }

    @Test
    fun `archiveHabit should call repository archiveHabit`() =
        runTest {
            // Given
            val habitId = "habit-id"

            coEvery { habitRepository.archiveHabit(habitId) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.ArchiveHabit(habitId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.archiveHabit(habitId) }
        }

    @Test
    fun `toggleChecklistItem should call repository toggleChecklistItem`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val itemId = "item-id"
            val date = LocalDate.now()

            coEvery { habitRepository.toggleChecklistItem(habitId, date, itemId) } returns Result.success(Unit)

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.ToggleChecklistItem(habitId, itemId))
            advanceUntilIdle()

            // Then
            coVerify { habitRepository.toggleChecklistItem(habitId, date, itemId) }
        }

    @Test
    fun `toggleChecklistItem failure should set error in state`() =
        runTest {
            // Given
            val habitId = "habit-id"
            val itemId = "item-id"

            coEvery { habitRepository.toggleChecklistItem(any(), any(), any()) } returns
                Result.failure(Exception("error"))

            viewModel = HabitsViewModel(habitRepository)
            advanceUntilIdle()

            // When
            viewModel.processIntent(HabitsIntent.ToggleChecklistItem(habitId, itemId))
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.state.value.error != null)
        }

    private fun createTestHabit(
        id: String = "test-id",
        title: String = "Test Habit",
    ): Habit {
        return Habit(
            id = id,
            title = title,
            habitType = HabitType.MEASURABLE,
            targetValue = 5,
            unit = "times",
            frequencyType = FrequencyType.DAILY,
            startDate = LocalDate.now(),
        )
    }
}
