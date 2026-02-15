package com.habitao.feature.habits.viewmodel

import com.habitao.domain.model.Habit
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.TrackingType
import com.habitao.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@ExperimentalCoroutinesApi
class HabitsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var habitRepository: HabitRepository
    private lateinit var viewModel: HabitsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = mockk()
        viewModel = HabitsViewModel(habitRepository)
    }

    @Test
    fun `initial state should have empty habits and not loading`() = runTest {
        // Then
        val initialState = viewModel.state.value
        assertTrue(initialState.habits.isEmpty())
        assertFalse(initialState.isLoading)
        assertEquals(LocalDate.now(), initialState.selectedDate)
    }

    @Test
    fun `loadHabits should update state with habits`() = runTest {
        // Given
        val habits = listOf(createTestHabit())
        coEvery { habitRepository.getHabitsForDate(any()) } returns Result.success(habits)

        // When
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(1, state.habits.size)
        assertEquals("Test Habit", state.habits[0].title)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadHabits should handle error`() = runTest {
        // Given
        coEvery { habitRepository.getHabitsForDate(any()) } returns 
            Result.failure(Exception("Network error"))

        // When
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        // Given
        coEvery { habitRepository.getHabitsForDate(any()) } returns 
            Result.failure(Exception("Error"))
        viewModel.processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
        advanceUntilIdle()
        
        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.state.value.error)
    }

    private fun createTestHabit(): Habit {
        return Habit(
            id = "test-id",
            title = "Test Habit",
            goalCount = 5,
            trackingType = TrackingType.COUNT,
            repeatPattern = RepeatPattern.DAILY,
            startDate = LocalDate.now(),
            nextScheduledDate = LocalDate.now()
        )
    }
}
