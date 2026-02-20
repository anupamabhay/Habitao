package com.habitao.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Habit domain model")
class HabitTest {
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate

    @BeforeEach
    fun setup() {
        startDate = LocalDate.of(2024, 1, 1)
        endDate = LocalDate.of(2024, 1, 10)
    }

    @Nested
    @DisplayName("isScheduledFor")
    inner class IsScheduledForTests {
        @Test
        fun `returns false before start and after end dates`() {
            // Given
            val habit = createBaseHabit().copy(endDate = endDate)

            // When
            val beforeStart = habit.isScheduledFor(startDate.minusDays(1))
            val afterEnd = habit.isScheduledFor(endDate.plusDays(1))

            // Then
            assertFalse(beforeStart)
            assertFalse(afterEnd)
        }

        @Test
        fun `returns true on start and end dates for daily frequency`() {
            // Given
            val habit = createBaseHabit().copy(endDate = endDate, frequencyType = FrequencyType.DAILY)

            // When
            val onStart = habit.isScheduledFor(startDate)
            val onEnd = habit.isScheduledFor(endDate)

            // Then
            assertTrue(onStart)
            assertTrue(onEnd)
        }

        @Test
        fun `returns true for daily frequency within range`() {
            // Given
            val habit = createBaseHabit().copy(frequencyType = FrequencyType.DAILY)

            // When
            val scheduled = habit.isScheduledFor(startDate.plusDays(3))

            // Then
            assertTrue(scheduled)
        }

        @Test
        fun `returns true only for specific scheduled days`() {
            // Given
            val habit =
                createBaseHabit().copy(
                    frequencyType = FrequencyType.SPECIFIC_DAYS,
                    scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                )

            // When
            val monday = habit.isScheduledFor(LocalDate.of(2024, 1, 1))
            val tuesday = habit.isScheduledFor(LocalDate.of(2024, 1, 2))

            // Then
            assertTrue(monday)
            assertFalse(tuesday)
        }

        @Test
        fun `returns true for times per week within range`() {
            // Given
            val habit = createBaseHabit().copy(frequencyType = FrequencyType.TIMES_PER_WEEK)

            // When
            val scheduled = habit.isScheduledFor(startDate.plusDays(4))

            // Then
            assertTrue(scheduled)
        }

        @Test
        fun `returns true for every x days regardless of interval position`() {
            // Given: EVERY_X_DAYS habits are always shown (user tracks progress manually)
            val habit =
                createBaseHabit().copy(
                    frequencyType = FrequencyType.EVERY_X_DAYS,
                    frequencyValue = 2,
                )

            // When
            val onInterval = habit.isScheduledFor(startDate.plusDays(2))
            val offInterval = habit.isScheduledFor(startDate.plusDays(1))

            // Then: Both should return true since EVERY_X_DAYS always shows
            assertTrue(onInterval)
            assertTrue(offInterval)
        }
    }

    @Nested
    @DisplayName("getFrequencyDescription")
    inner class GetFrequencyDescriptionTests {
        @Test
        fun `returns every day for daily frequency`() {
            // Given
            val habit = createBaseHabit().copy(frequencyType = FrequencyType.DAILY)

            // When
            val description = habit.getFrequencyDescription()

            // Then
            assertEquals("Every day", description)
        }

        @Test
        fun `returns short names for specific days`() {
            // Given
            val habit =
                createBaseHabit().copy(
                    frequencyType = FrequencyType.SPECIFIC_DAYS,
                    scheduledDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY),
                )

            // When
            val description = habit.getFrequencyDescription()

            // Then
            assertEquals("Mon, Wed", description)
        }

        @Test
        fun `returns every day when all specific days selected`() {
            // Given
            val habit =
                createBaseHabit().copy(
                    frequencyType = FrequencyType.SPECIFIC_DAYS,
                    scheduledDays = DayOfWeek.values().toSet(),
                )

            // When
            val description = habit.getFrequencyDescription()

            // Then
            assertEquals("Every day", description)
        }

        @Test
        fun `returns times per week description`() {
            // Given
            val habit =
                createBaseHabit().copy(
                    frequencyType = FrequencyType.TIMES_PER_WEEK,
                    frequencyValue = 3,
                )

            // When
            val description = habit.getFrequencyDescription()

            // Then
            assertEquals("3 times per week", description)
        }

        @Test
        fun `returns every x days description`() {
            // Given
            val habit =
                createBaseHabit().copy(
                    frequencyType = FrequencyType.EVERY_X_DAYS,
                    frequencyValue = 2,
                )

            // When
            val description = habit.getFrequencyDescription()

            // Then
            assertEquals("Every 2 days", description)
        }
    }

    @Nested
    @DisplayName("getTargetDescription")
    inner class GetTargetDescriptionTests {
        @Test
        fun `returns descriptions for each habit type`() {
            // Given
            val simple = createBaseHabit().copy(habitType = HabitType.SIMPLE)
            val measurable =
                createBaseHabit().copy(
                    habitType = HabitType.MEASURABLE,
                    targetValue = 5,
                    unit = null,
                )
            val checklist =
                createBaseHabit().copy(
                    habitType = HabitType.CHECKLIST,
                    checklist =
                        listOf(
                            ChecklistItem(text = "One"),
                            ChecklistItem(text = "Two"),
                        ),
                )

            // When
            val simpleDescription = simple.getTargetDescription()
            val measurableDescription = measurable.getTargetDescription()
            val checklistDescription = checklist.getTargetDescription()

            // Then
            assertEquals("", simpleDescription)
            assertEquals("5 times", measurableDescription)
            assertEquals("2 items", checklistDescription)
        }

        @Test
        fun `includes at most operator for measurable habits`() {
            // Given
            val habit =
                createBaseHabit().copy(
                    habitType = HabitType.MEASURABLE,
                    targetValue = 3,
                    unit = "cups",
                    targetOperator = TargetOperator.AT_MOST,
                )

            // When
            val description = habit.getTargetDescription()

            // Then
            assertEquals("at most 3 cups", description)
        }
    }

    @Nested
    @DisplayName("HabitLog progress")
    inner class HabitLogProgressTests {
        @Test
        fun `returns fraction of current value over target`() {
            // Given
            val log =
                HabitLog(
                    habitId = "habit-id",
                    date = startDate,
                    currentValue = 2,
                    targetValue = 5,
                )

            // When
            val progress = log.progress

            // Then
            assertEquals(0.4f, progress)
        }

        @Test
        fun `handles zero target values and clamps over completion`() {
            // Given
            val completedZeroTarget =
                HabitLog(
                    habitId = "habit-id",
                    date = startDate,
                    currentValue = 0,
                    targetValue = 0,
                    isCompleted = true,
                )
            val incompleteZeroTarget =
                HabitLog(
                    habitId = "habit-id",
                    date = startDate,
                    currentValue = 0,
                    targetValue = 0,
                    isCompleted = false,
                )
            val overTarget =
                HabitLog(
                    habitId = "habit-id",
                    date = startDate,
                    currentValue = 6,
                    targetValue = 5,
                )

            // When
            val completedProgress = completedZeroTarget.progress
            val incompleteProgress = incompleteZeroTarget.progress
            val overTargetProgress = overTarget.progress

            // Then
            assertEquals(1f, completedProgress)
            assertEquals(0f, incompleteProgress)
            assertEquals(1f, overTargetProgress)
        }
    }

    @Nested
    @DisplayName("Legacy conversions")
    inner class LegacyConversionTests {
        @Test
        fun `tracking type converts to habit type`() {
            // Given
            val count = TrackingType.COUNT
            val duration = TrackingType.DURATION
            val binary = TrackingType.BINARY

            // When
            val countType = count.toHabitType()
            val durationType = duration.toHabitType()
            val binaryType = binary.toHabitType()

            // Then
            assertEquals(HabitType.MEASURABLE, countType)
            assertEquals(HabitType.MEASURABLE, durationType)
            assertEquals(HabitType.SIMPLE, binaryType)
        }

        @Test
        fun `repeat pattern converts to frequency type`() {
            // Given
            val daily = RepeatPattern.DAILY
            val weekly = RepeatPattern.WEEKLY
            val custom = RepeatPattern.CUSTOM
            val specificDates = RepeatPattern.SPECIFIC_DATES

            // When
            val dailyType = daily.toFrequencyType()
            val weeklyType = weekly.toFrequencyType()
            val customType = custom.toFrequencyType()
            val specificDatesType = specificDates.toFrequencyType()

            // Then
            assertEquals(FrequencyType.DAILY, dailyType)
            assertEquals(FrequencyType.SPECIFIC_DAYS, weeklyType)
            assertEquals(FrequencyType.EVERY_X_DAYS, customType)
            assertEquals(FrequencyType.SPECIFIC_DAYS, specificDatesType)
        }
    }

    @Nested
    @DisplayName("Habit presets")
    inner class HabitPresetsTests {
        @Test
        fun `getByCategory returns presets matching category`() {
            // Given
            val category = PresetCategory.HEALTH

            // When
            val presets = HabitPresets.getByCategory(category)

            // Then
            assertTrue(presets.isNotEmpty())
            assertTrue(presets.all { it.category == category })
        }
    }

    private fun createBaseHabit(): Habit {
        return Habit(
            id = "habit-id",
            title = "Test Habit",
            habitType = HabitType.SIMPLE,
            frequencyType = FrequencyType.DAILY,
            startDate = startDate,
            endDate = null,
        )
    }
}
