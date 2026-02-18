package com.habitao.data.repository

import com.habitao.data.local.entity.FrequencyTypeEntity
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.data.local.entity.HabitTypeEntity
import com.habitao.data.local.entity.TargetOperatorEntity
import com.habitao.domain.model.ChecklistItem
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.model.HabitType
import com.habitao.domain.model.TargetOperator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class HabitMapperTest {
    @Test
    fun `habit entity toDomainModel maps all fields`() {
        val startDate = LocalDate.of(2025, 1, 20)
        val endDate = LocalDate.of(2025, 2, 1)
        val entity =
            HabitEntity(
                id = "habit-1",
                title = "Read",
                description = "Read 20 minutes",
                icon = "book",
                color = "blue",
                habitType = HabitTypeEntity.CHECKLIST.name,
                targetValue = 3,
                unit = "items",
                targetOperator = TargetOperatorEntity.AT_MOST.name,
                checklistJson =
                    """
                    [
                      {"id":"c1","text":"Step 1","sortOrder":0},
                      {"id":"c2","text":"Step 2","sortOrder":1}
                    ]
                    """.trimIndent(),
                frequencyType = FrequencyTypeEntity.SPECIFIC_DAYS.name,
                frequencyValue = 2,
                scheduledDaysJson = "[\"MONDAY\",\"WEDNESDAY\"]",
                startDate = toMillis(startDate),
                endDate = toMillis(endDate),
                reminderEnabled = true,
                reminderTimeMinutes = 9 * 60 + 30,
                createdAt = 10L,
                updatedAt = 20L,
                isArchived = true,
                sortOrder = 5,
            )

        val model = entity.toDomainModel()

        assertEquals("habit-1", model.id)
        assertEquals("Read", model.title)
        assertEquals("Read 20 minutes", model.description)
        assertEquals("book", model.icon)
        assertEquals("blue", model.color)
        assertEquals(HabitType.CHECKLIST, model.habitType)
        assertEquals(3, model.targetValue)
        assertEquals("items", model.unit)
        assertEquals(TargetOperator.AT_MOST, model.targetOperator)
        assertEquals(2, model.checklist.size)
        assertEquals(FrequencyType.SPECIFIC_DAYS, model.frequencyType)
        assertEquals(2, model.frequencyValue)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), model.scheduledDays)
        assertEquals(startDate, model.startDate)
        assertEquals(endDate, model.endDate)
        assertTrue(model.reminderEnabled)
        assertEquals(LocalTime.of(9, 30), model.reminderTime)
        assertEquals(10L, model.createdAt)
        assertEquals(20L, model.updatedAt)
        assertTrue(model.isArchived)
        assertEquals(5, model.sortOrder)
    }

    @Test
    fun `habit toEntity maps all fields and legacy goalCount`() {
        val habit =
            Habit(
                id = "habit-2",
                title = "Run",
                description = "Run 5k",
                icon = "run",
                color = "red",
                habitType = HabitType.MEASURABLE,
                targetValue = 5,
                unit = "km",
                targetOperator = TargetOperator.AT_LEAST,
                checklist = listOf(ChecklistItem(id = "c1", text = "Warm up", sortOrder = 0)),
                frequencyType = FrequencyType.EVERY_X_DAYS,
                frequencyValue = 3,
                scheduledDays = setOf(DayOfWeek.FRIDAY),
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 4, 1),
                reminderEnabled = true,
                reminderTime = LocalTime.of(6, 15),
                createdAt = 30L,
                updatedAt = 40L,
                isArchived = false,
                sortOrder = 2,
            )

        val entity = habit.toEntity()

        assertEquals("habit-2", entity.id)
        assertEquals("Run", entity.title)
        assertEquals("Run 5k", entity.description)
        assertEquals("run", entity.icon)
        assertEquals("red", entity.color)
        assertEquals(HabitType.MEASURABLE.name, entity.habitType)
        assertEquals(5, entity.targetValue)
        assertEquals("km", entity.unit)
        assertEquals(TargetOperator.AT_LEAST.name, entity.targetOperator)
        assertNotNull(entity.checklistJson)
        assertEquals(5, entity.goalCount)
        assertEquals(FrequencyType.EVERY_X_DAYS.name, entity.frequencyType)
        assertEquals(3, entity.frequencyValue)
        assertNotNull(entity.scheduledDaysJson)
        assertEquals(toMillis(LocalDate.of(2025, 3, 1)), entity.startDate)
        assertEquals(toMillis(LocalDate.of(2025, 4, 1)), entity.endDate)
        assertTrue(entity.reminderEnabled)
        assertEquals(6 * 60 + 15, entity.reminderTimeMinutes)
        assertEquals(30L, entity.createdAt)
        assertEquals(40L, entity.updatedAt)
        assertEquals(false, entity.isArchived)
        assertEquals(2, entity.sortOrder)
    }

    @Test
    fun `habit log entity toDomainModel maps fields`() {
        val date = LocalDate.of(2025, 1, 10)
        val entity =
            HabitLogEntity(
                id = "log-1",
                habitId = "habit-1",
                date = toMillis(date),
                currentValue = 2,
                targetValue = 5,
                isCompleted = false,
                completedChecklistItemsJson = "[\"item-1\",\"item-2\"]",
                createdAt = 11L,
                updatedAt = 22L,
                completedAt = null,
            )

        val model = entity.toDomainModel()

        assertEquals("log-1", model.id)
        assertEquals("habit-1", model.habitId)
        assertEquals(date, model.date)
        assertEquals(2, model.currentValue)
        assertEquals(5, model.targetValue)
        assertEquals(false, model.isCompleted)
        assertEquals(setOf("item-1", "item-2"), model.completedChecklistItems)
        assertEquals(11L, model.createdAt)
        assertEquals(22L, model.updatedAt)
        assertNull(model.completedAt)
    }

    @Test
    fun `habit log toEntity maps fields and legacy counts`() {
        val log =
            HabitLog(
                id = "log-2",
                habitId = "habit-2",
                date = LocalDate.of(2025, 5, 2),
                currentValue = 3,
                targetValue = 4,
                isCompleted = true,
                completedChecklistItems = setOf("item-3"),
                createdAt = 50L,
                updatedAt = 60L,
                completedAt = 70L,
            )

        val entity = log.toEntity()

        assertEquals("log-2", entity.id)
        assertEquals("habit-2", entity.habitId)
        assertEquals(toMillis(LocalDate.of(2025, 5, 2)), entity.date)
        assertEquals(3, entity.currentValue)
        assertEquals(4, entity.targetValue)
        assertEquals(true, entity.isCompleted)
        assertNotNull(entity.completedChecklistItemsJson)
        assertEquals(3, entity.currentCount)
        assertEquals(4, entity.goalCount)
        assertEquals(50L, entity.createdAt)
        assertEquals(60L, entity.updatedAt)
        assertEquals(70L, entity.completedAt)
    }

    @Test
    fun `habit entity parses legacy habit types`() {
        val countEntity = baseHabitEntity(habitType = "COUNT")
        val binaryEntity = baseHabitEntity(habitType = "BINARY")
        val durationEntity = baseHabitEntity(habitType = "DURATION")

        assertEquals(HabitType.MEASURABLE, countEntity.toDomainModel().habitType)
        assertEquals(HabitType.SIMPLE, binaryEntity.toDomainModel().habitType)
        assertEquals(HabitType.MEASURABLE, durationEntity.toDomainModel().habitType)
    }

    @Test
    fun `habit entity parses legacy frequency types`() {
        val weeklyEntity = baseHabitEntity(frequencyType = "WEEKLY")
        val customEntity = baseHabitEntity(frequencyType = "CUSTOM")

        assertEquals(FrequencyType.SPECIFIC_DAYS, weeklyEntity.toDomainModel().frequencyType)
        assertEquals(FrequencyType.EVERY_X_DAYS, customEntity.toDomainModel().frequencyType)
    }

    @Test
    fun `habit entity unknown values fallback to defaults`() {
        val entity =
            baseHabitEntity(
                habitType = "UNKNOWN_TYPE",
                targetOperator = "UNKNOWN_OPERATOR",
                frequencyType = "UNKNOWN_FREQUENCY",
            )

        val model = entity.toDomainModel()

        assertEquals(HabitType.SIMPLE, model.habitType)
        assertEquals(TargetOperator.AT_LEAST, model.targetOperator)
        assertEquals(FrequencyType.DAILY, model.frequencyType)
    }

    @Test
    fun `habit entity handles null optional fields`() {
        val entity =
            HabitEntity(
                id = "habit-null",
                title = "Nulls",
                description = null,
                icon = null,
                color = null,
                unit = null,
                checklistJson = null,
                scheduledDaysJson = null,
                endDate = null,
                reminderTimeMinutes = null,
                startDate = toMillis(LocalDate.of(2025, 1, 1)),
            )

        val model = entity.toDomainModel()

        assertNull(model.description)
        assertNull(model.icon)
        assertNull(model.color)
        assertNull(model.unit)
        assertTrue(model.checklist.isEmpty())
        assertTrue(model.scheduledDays.isEmpty())
        assertNull(model.endDate)
        assertNull(model.reminderTime)
    }

    @Test
    fun `habit entity parses json fields`() {
        val entity =
            baseHabitEntity(
                checklistJson =
                    """
                    [{"id":"a","text":"Alpha","sortOrder":0},{"id":"b","text":"Beta","sortOrder":1}]
                    """.trimIndent(),
                scheduledDaysJson = "[\"TUESDAY\",\"THURSDAY\"]",
            )

        val model = entity.toDomainModel()

        assertEquals(2, model.checklist.size)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), model.scheduledDays)
    }

    @Test
    fun `habit log entity parses completed checklist items json`() {
        val entity =
            HabitLogEntity(
                id = "log-3",
                habitId = "habit-3",
                date = toMillis(LocalDate.of(2025, 2, 2)),
                completedChecklistItemsJson = "[\"i1\",\"i2\"]",
            )

        val model = entity.toDomainModel()

        assertEquals(setOf("i1", "i2"), model.completedChecklistItems)
    }

    @Test
    fun `habit roundtrip toEntity toDomainModel preserves key fields`() {
        val habit =
            Habit(
                id = "habit-round",
                title = "Journal",
                description = "Morning",
                icon = "edit",
                color = "green",
                habitType = HabitType.CHECKLIST,
                targetValue = 2,
                unit = "items",
                targetOperator = TargetOperator.AT_LEAST,
                checklist = listOf(ChecklistItem(id = "x", text = "One", sortOrder = 0)),
                frequencyType = FrequencyType.SPECIFIC_DAYS,
                frequencyValue = 1,
                scheduledDays = setOf(DayOfWeek.SATURDAY),
                startDate = LocalDate.of(2025, 6, 1),
                endDate = LocalDate.of(2025, 7, 1),
                reminderEnabled = true,
                reminderTime = LocalTime.of(7, 0),
                createdAt = 100L,
                updatedAt = 110L,
                isArchived = true,
                sortOrder = 4,
            )

        val roundtrip = habit.toEntity().toDomainModel()

        assertEquals(habit.id, roundtrip.id)
        assertEquals(habit.title, roundtrip.title)
        assertEquals(habit.description, roundtrip.description)
        assertEquals(habit.icon, roundtrip.icon)
        assertEquals(habit.color, roundtrip.color)
        assertEquals(habit.habitType, roundtrip.habitType)
        assertEquals(habit.targetValue, roundtrip.targetValue)
        assertEquals(habit.unit, roundtrip.unit)
        assertEquals(habit.targetOperator, roundtrip.targetOperator)
        assertEquals(habit.checklist.size, roundtrip.checklist.size)
        assertEquals(habit.frequencyType, roundtrip.frequencyType)
        assertEquals(habit.frequencyValue, roundtrip.frequencyValue)
        assertEquals(habit.scheduledDays, roundtrip.scheduledDays)
        assertEquals(habit.startDate, roundtrip.startDate)
        assertEquals(habit.endDate, roundtrip.endDate)
        assertEquals(habit.reminderEnabled, roundtrip.reminderEnabled)
        assertEquals(habit.reminderTime, roundtrip.reminderTime)
        assertEquals(habit.createdAt, roundtrip.createdAt)
        assertEquals(habit.updatedAt, roundtrip.updatedAt)
        assertEquals(habit.isArchived, roundtrip.isArchived)
        assertEquals(habit.sortOrder, roundtrip.sortOrder)
    }

    private fun baseHabitEntity(
        habitType: String = HabitTypeEntity.SIMPLE.name,
        targetOperator: String = TargetOperatorEntity.AT_LEAST.name,
        frequencyType: String = FrequencyTypeEntity.DAILY.name,
        checklistJson: String? = null,
        scheduledDaysJson: String? = null,
    ): HabitEntity {
        return HabitEntity(
            id = "habit-base",
            title = "Base",
            habitType = habitType,
            targetOperator = targetOperator,
            frequencyType = frequencyType,
            checklistJson = checklistJson,
            scheduledDaysJson = scheduledDaysJson,
            startDate = toMillis(LocalDate.of(2025, 1, 1)),
        )
    }

    private fun toMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
