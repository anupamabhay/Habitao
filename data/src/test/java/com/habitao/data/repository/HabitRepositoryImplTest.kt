package com.habitao.data.repository

import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.entity.FrequencyTypeEntity
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.data.local.entity.HabitTypeEntity
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitType
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
import java.time.ZoneId

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

    @Test
    fun `updateHabit should call updateHabit on DAO`() =
        runTest {
            val habit = createTestHabit()
            coEvery { habitDao.updateHabit(any()) } returns Unit

            val result = repository.updateHabit(habit)

            assertTrue(result.isSuccess)
            coVerify { habitDao.updateHabit(any()) }
        }

    @Test
    fun `deleteHabit should call deleteHabitById on DAO`() =
        runTest {
            val habitId = "delete-id"
            coEvery { habitDao.deleteHabitById(habitId) } returns Unit

            val result = repository.deleteHabit(habitId)

            assertTrue(result.isSuccess)
            coVerify { habitDao.deleteHabitById(habitId) }
        }

    @Test
    fun `createHabit should return failure on DAO exception`() =
        runTest {
            val habit = createTestHabit()
            coEvery { habitDao.insertHabit(any()) } throws IllegalStateException("insert failed")

            val result = repository.createHabit(habit)

            assertTrue(result.isFailure)
        }

    @Test
    fun `updateHabit should return failure on DAO exception`() =
        runTest {
            val habit = createTestHabit()
            coEvery { habitDao.updateHabit(any()) } throws IllegalStateException("update failed")

            val result = repository.updateHabit(habit)

            assertTrue(result.isFailure)
        }

    @Test
    fun `deleteHabit should return failure on DAO exception`() =
        runTest {
            val habitId = "delete-fail"
            coEvery { habitDao.deleteHabitById(habitId) } throws IllegalStateException("delete failed")

            val result = repository.deleteHabit(habitId)

            assertTrue(result.isFailure)
        }

    @Test
    fun `archiveHabit should call setHabitArchived on DAO`() =
        runTest {
            val habitId = "archive-id"
            coEvery { habitDao.setHabitArchived(habitId, true) } returns Unit

            val result = repository.archiveHabit(habitId, true)

            assertTrue(result.isSuccess)
            coVerify { habitDao.setHabitArchived(habitId, true) }
        }

    @Test
    fun `createOrUpdateLog should create new log when none exists`() =
        runTest {
            val habitId = "habit-log"
            val date = LocalDate.of(2025, 1, 2)
            val habit = createTestHabit().copy(id = habitId, targetValue = 3)
            val dateMillis = dateToMillis(date)
            coEvery { habitDao.getHabitById(habitId) } returns createTestHabitEntity(habitId).copy(targetValue = 3)
            coEvery { habitLogDao.getLogForHabitAndDate(habitId, dateMillis) } returns null
            coEvery { habitLogDao.insertLog(any()) } returns 1L

            val result = repository.createOrUpdateLog(habitId, date, 2)

            assertTrue(result.isSuccess)
            coVerify { habitLogDao.insertLog(match { it.habitId == habitId && it.date == dateMillis }) }
        }

    @Test
    fun `createOrUpdateLog should update existing log`() =
        runTest {
            val habitId = "habit-log-update"
            val date = LocalDate.of(2025, 1, 3)
            val dateMillis = dateToMillis(date)
            val existing =
                HabitLogEntity(
                    id = "log-1",
                    habitId = habitId,
                    date = dateMillis,
                    currentValue = 1,
                    targetValue = 3,
                    isCompleted = false,
                )
            coEvery { habitDao.getHabitById(habitId) } returns createTestHabitEntity(habitId).copy(targetValue = 3)
            coEvery { habitLogDao.getLogForHabitAndDate(habitId, dateMillis) } returns existing
            coEvery { habitLogDao.insertLog(any()) } returns 1L

            val result = repository.createOrUpdateLog(habitId, date, 2)

            assertTrue(result.isSuccess)
            coVerify { habitLogDao.insertLog(match { it.id == "log-1" && it.currentValue == 2 }) }
        }

    @Test
    fun `createOrUpdateLog should set isCompleted when count reaches target`() =
        runTest {
            val habitId = "habit-log-complete"
            val date = LocalDate.of(2025, 1, 4)
            val dateMillis = dateToMillis(date)
            coEvery { habitDao.getHabitById(habitId) } returns createTestHabitEntity(habitId).copy(targetValue = 2)
            coEvery { habitLogDao.getLogForHabitAndDate(habitId, dateMillis) } returns null
            coEvery { habitLogDao.insertLog(any()) } returns 1L

            val result = repository.createOrUpdateLog(habitId, date, 2)

            assertTrue(result.isSuccess)
            coVerify { habitLogDao.insertLog(match { it.isCompleted && it.currentValue == 2 && it.targetValue == 2 }) }
        }

    @Test
    fun `createOrUpdateLog should return failure when habit not found`() =
        runTest {
            val habitId = "missing-habit"
            val date = LocalDate.of(2025, 1, 5)
            coEvery { habitDao.getHabitById(habitId) } returns null

            val result = repository.createOrUpdateLog(habitId, date, 1)

            assertTrue(result.isFailure)
        }

    @Test
    fun `getLogForHabitAndDate should return log when found`() =
        runTest {
            val habitId = "habit-log-found"
            val date = LocalDate.of(2025, 1, 6)
            val dateMillis = dateToMillis(date)
            val log =
                HabitLogEntity(
                    id = "log-found",
                    habitId = habitId,
                    date = dateMillis,
                    currentValue = 1,
                    targetValue = 2,
                    isCompleted = false,
                )
            coEvery { habitLogDao.getLogForHabitAndDate(habitId, dateMillis) } returns log

            val result = repository.getLogForHabitAndDate(habitId, date)

            assertTrue(result.isSuccess)
            assertEquals("log-found", result.getOrNull()?.id)
        }

    @Test
    fun `getLogForHabitAndDate should return null result when no log exists`() =
        runTest {
            val habitId = "habit-log-none"
            val date = LocalDate.of(2025, 1, 7)
            val dateMillis = dateToMillis(date)
            coEvery { habitLogDao.getLogForHabitAndDate(habitId, dateMillis) } returns null

            val result = repository.getLogForHabitAndDate(habitId, date)

            assertTrue(result.isSuccess)
            assertEquals(null, result.getOrNull())
        }

    private fun createTestHabit(): Habit {
        return Habit(
            id = "test-id",
            title = "Test Habit",
            targetValue = 5,
            habitType = HabitType.MEASURABLE,
            frequencyType = FrequencyType.DAILY,
            startDate = LocalDate.now(),
        )
    }

    private fun createTestHabitEntity(id: String): HabitEntity {
        return HabitEntity(
            id = id,
            title = "Test Habit",
            targetValue = 5,
            habitType = HabitTypeEntity.MEASURABLE.name,
            frequencyType = FrequencyTypeEntity.DAILY.name,
            startDate = System.currentTimeMillis(),
        )
    }

    private fun dateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
