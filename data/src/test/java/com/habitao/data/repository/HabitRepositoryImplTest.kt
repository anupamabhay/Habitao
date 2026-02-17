package com.habitao.data.repository

import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.entity.HabitEntity
import com.habitao.domain.model.Habit
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.TrackingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class HabitRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var habitDao: HabitDao
    private lateinit var habitLogDao: HabitLogDao
    private lateinit var repository: HabitRepositoryImpl

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        habitDao = mockk()
        habitLogDao = mockk()
        repository = HabitRepositoryImpl(habitDao, habitLogDao, testDispatcher)
    }

    @Test
    fun `createHabit should insert habit into database`() =
        runTest {
            // Given
            val habit = createTestHabit()
            coEvery { habitDao.insertHabit(any()) } returns 1L

            // When
            val result = repository.createHabit(habit)

            // Then
            assertTrue(result.isSuccess)
            coVerify { habitDao.insertHabit(any()) }
        }

    @Test
    fun `getHabitById should return habit when found`() =
        runTest {
            // Given
            val habitId = "test-id"
            val entity = createTestHabitEntity(habitId)
            coEvery { habitDao.getHabitById(habitId) } returns entity

            // When
            val result = repository.getHabitById(habitId)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(habitId, result.getOrNull()?.id)
        }

    @Test
    fun `getHabitById should return failure when not found`() =
        runTest {
            // Given
            val habitId = "non-existent"
            coEvery { habitDao.getHabitById(habitId) } returns null

            // When
            val result = repository.getHabitById(habitId)

            // Then
            assertTrue(result.isFailure)
        }

    private fun createTestHabit(): Habit {
        return Habit(
            id = "test-id",
            title = "Test Habit",
            goalCount = 5,
            trackingType = TrackingType.COUNT,
            repeatPattern = RepeatPattern.DAILY,
            startDate = LocalDate.now(),
            nextScheduledDate = LocalDate.now(),
        )
    }

    private fun createTestHabitEntity(id: String): HabitEntity {
        return HabitEntity(
            id = id,
            title = "Test Habit",
            goalCount = 5,
            trackingType = TrackingType.COUNT.name,
            repeatPattern = RepeatPattern.DAILY.name,
            startDate = System.currentTimeMillis(),
            nextScheduledDate = System.currentTimeMillis(),
        )
    }
}
