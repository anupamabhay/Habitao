package com.habitao.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.RepeatPattern
import com.habitao.data.local.entity.TrackingType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitaoDatabaseTest {
    private lateinit var database: HabitaoDatabase
    private lateinit var habitDao: com.habitao.data.local.dao.HabitDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(
                context,
                HabitaoDatabase::class.java,
            ).allowMainThreadQueries().build()
        habitDao = database.habitDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveHabit() =
        runBlocking {
            // Given
            val habit =
                HabitEntity(
                    id = "test-id",
                    title = "Test Habit",
                    goalCount = 5,
                    trackingType = TrackingType.COUNT.name,
                    repeatPattern = RepeatPattern.DAILY.name,
                    startDate = System.currentTimeMillis(),
                    nextScheduledDate = System.currentTimeMillis(),
                )

            // When
            habitDao.insertHabit(habit)
            val retrieved = habitDao.getHabitById("test-id")

            // Then
            assertNotNull(retrieved)
            assertEquals("Test Habit", retrieved?.title)
            assertEquals(5, retrieved?.goalCount)
        }

    @Test
    fun observeAllHabitsEmitsList() =
        runBlocking {
            // Given
            val habit1 = createHabitEntity("1", "Habit 1")
            val habit2 = createHabitEntity("2", "Habit 2")

            // When
            habitDao.insertHabit(habit1)
            habitDao.insertHabit(habit2)
            val habits = habitDao.observeAllHabits().first()

            // Then
            assertEquals(2, habits.size)
        }

    private fun createHabitEntity(
        id: String,
        title: String,
    ): HabitEntity {
        return HabitEntity(
            id = id,
            title = title,
            goalCount = 1,
            trackingType = TrackingType.COUNT.name,
            repeatPattern = RepeatPattern.DAILY.name,
            startDate = System.currentTimeMillis(),
            nextScheduledDate = System.currentTimeMillis(),
        )
    }
}
