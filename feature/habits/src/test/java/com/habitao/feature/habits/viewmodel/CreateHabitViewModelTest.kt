package com.habitao.feature.habits.viewmodel

import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitType
import com.habitao.domain.repository.HabitRepository
import com.habitao.system.notifications.HabitReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
@DisplayName("CreateHabitViewModel")
class CreateHabitViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var habitRepository: HabitRepository
    private lateinit var reminderScheduler: HabitReminderScheduler
    private lateinit var viewModel: CreateHabitViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = mockk()
        reminderScheduler = mockk(relaxed = true)
        viewModel = CreateHabitViewModel(habitRepository, reminderScheduler)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Title validation")
    inner class TitleValidationTests {
        @Test
        fun `blank title sets error`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle(""))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertNotNull(viewModel.state.value.titleError)
            }

        @Test
        fun `short title sets error`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("X"))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertEquals("Name must be at least 2 characters", viewModel.state.value.titleError)
            }

        @Test
        fun `long title sets error`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("A".repeat(101)))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertEquals("Name must be less than 100 characters", viewModel.state.value.titleError)
            }

        @Test
        fun `valid title clears error`() {
            viewModel.processIntent(CreateHabitIntent.UpdateTitle("Valid Name"))

            assertNull(viewModel.state.value.titleError)
            assertEquals("Valid Name", viewModel.state.value.title)
        }
    }

    @Nested
    @DisplayName("Target value validation")
    inner class TargetValueValidationTests {
        @Test
        fun `simple type skips target validation`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Test Habit"))
                viewModel.processIntent(CreateHabitIntent.UpdateHabitType(HabitType.SIMPLE))
                viewModel.processIntent(CreateHabitIntent.UpdateTargetValue(""))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertNull(viewModel.state.value.targetValueError)
            }

        @Test
        fun `measurable with blank target sets error`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Test Habit"))
                viewModel.processIntent(CreateHabitIntent.UpdateHabitType(HabitType.MEASURABLE))
                viewModel.processIntent(CreateHabitIntent.UpdateTargetValue(""))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertNotNull(viewModel.state.value.targetValueError)
            }

        @Test
        fun `rejects non-digit input`() {
            viewModel.processIntent(CreateHabitIntent.UpdateTargetValue("abc"))

            // Non-digit input is rejected, value stays at default
            assertEquals("1", viewModel.state.value.targetValue)
        }
    }

    @Nested
    @DisplayName("Checklist validation")
    inner class ChecklistValidationTests {
        @Test
        fun `checklist type with no items sets error`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Test Habit"))
                viewModel.processIntent(CreateHabitIntent.UpdateHabitType(HabitType.CHECKLIST))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertEquals("Add at least one checklist item", viewModel.state.value.checklistError)
            }

        @Test
        fun `non-checklist type skips checklist validation`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Test Habit"))
                viewModel.processIntent(CreateHabitIntent.UpdateHabitType(HabitType.SIMPLE))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertNull(viewModel.state.value.checklistError)
            }
    }

    @Nested
    @DisplayName("Intent processing")
    inner class IntentProcessingTests {
        @Test
        fun `update habit type to SIMPLE resets target value to 1`() {
            viewModel.processIntent(CreateHabitIntent.UpdateTargetValue("10"))
            viewModel.processIntent(CreateHabitIntent.UpdateHabitType(HabitType.SIMPLE))

            assertEquals("1", viewModel.state.value.targetValue)
        }

        @Test
        fun `toggle scheduled day adds and removes`() {
            viewModel.processIntent(CreateHabitIntent.ToggleScheduledDay(DayOfWeek.MONDAY))
            assertTrue(viewModel.state.value.scheduledDays.contains(DayOfWeek.MONDAY))

            viewModel.processIntent(CreateHabitIntent.ToggleScheduledDay(DayOfWeek.MONDAY))
            assertFalse(viewModel.state.value.scheduledDays.contains(DayOfWeek.MONDAY))
        }

        @Test
        fun `add checklist item trims and appends`() {
            viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("  Buy milk  "))
            viewModel.processIntent(CreateHabitIntent.AddChecklistItem)

            assertEquals(listOf("Buy milk"), viewModel.state.value.checklistItems)
            assertEquals("", viewModel.state.value.newChecklistItem)
        }

        @Test
        fun `add blank checklist item does nothing`() {
            viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("   "))
            viewModel.processIntent(CreateHabitIntent.AddChecklistItem)

            assertTrue(viewModel.state.value.checklistItems.isEmpty())
        }

        @Test
        fun `add checklist item at max 20 shows error`() {
            repeat(20) { i ->
                viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("Item $i"))
                viewModel.processIntent(CreateHabitIntent.AddChecklistItem)
            }

            viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("Item 21"))
            viewModel.processIntent(CreateHabitIntent.AddChecklistItem)

            assertEquals(20, viewModel.state.value.checklistItems.size)
            assertEquals("Maximum 20 items allowed", viewModel.state.value.checklistError)
        }

        @Test
        fun `remove checklist item by index`() {
            viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("A"))
            viewModel.processIntent(CreateHabitIntent.AddChecklistItem)
            viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("B"))
            viewModel.processIntent(CreateHabitIntent.AddChecklistItem)

            viewModel.processIntent(CreateHabitIntent.RemoveChecklistItem(0))

            assertEquals(listOf("B"), viewModel.state.value.checklistItems)
        }

        @Test
        fun `enable reminder sets default time when null`() {
            viewModel.processIntent(CreateHabitIntent.UpdateReminderEnabled(true))

            assertTrue(viewModel.state.value.reminderEnabled)
            assertEquals(LocalTime.of(9, 0), viewModel.state.value.reminderTime)
        }

        @Test
        fun `enable reminder preserves existing time`() {
            viewModel.processIntent(CreateHabitIntent.UpdateReminderTime(LocalTime.of(14, 30)))
            viewModel.processIntent(CreateHabitIntent.UpdateReminderEnabled(true))

            assertEquals(LocalTime.of(14, 30), viewModel.state.value.reminderTime)
        }

        @Test
        fun `reset form returns to initial state`() {
            viewModel.processIntent(CreateHabitIntent.UpdateTitle("Modified"))
            viewModel.processIntent(CreateHabitIntent.UpdateDescription("Desc"))
            viewModel.processIntent(CreateHabitIntent.ResetForm)

            val state = viewModel.state.value
            assertEquals("", state.title)
            assertEquals("", state.description)
            assertNull(state.editingHabitId)
        }

        @Test
        fun `update frequency value rejects non-digit`() {
            viewModel.processIntent(CreateHabitIntent.UpdateFrequencyValue("abc"))

            // Default value preserved
            assertEquals("3", viewModel.state.value.frequencyValue)
        }
    }

    @Nested
    @DisplayName("Save flow")
    inner class SaveFlowTests {
        @Test
        fun `save with validation errors does not call repository`() =
            runTest {
                // Title is blank - validation should fail
                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                coVerify(exactly = 0) { habitRepository.createHabit(any()) }
                coVerify(exactly = 0) { habitRepository.updateHabit(any()) }
            }

        @Test
        fun `save success creates habit via repository`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("New Habit"))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                coVerify { habitRepository.createHabit(any()) }
                assertFalse(viewModel.state.value.isSaving)
            }

        @Test
        fun `save failure sets error in state`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("New Habit"))
                coEvery { habitRepository.createHabit(any()) } returns
                    Result.failure(Exception("DB error"))

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                assertEquals("DB error", viewModel.state.value.error)
                assertFalse(viewModel.state.value.isSaving)
            }

        @Test
        fun `save with reminder enabled schedules reminder`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Reminder Habit"))
                viewModel.processIntent(CreateHabitIntent.UpdateReminderEnabled(true))
                viewModel.processIntent(CreateHabitIntent.UpdateReminderTime(LocalTime.of(8, 0)))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                coVerify {
                    reminderScheduler.scheduleReminder(any(), eq("Reminder Habit"), eq(LocalTime.of(8, 0)))
                }
            }

        @Test
        fun `save with reminder disabled cancels reminder`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("No Reminder"))
                viewModel.processIntent(CreateHabitIntent.UpdateReminderEnabled(false))
                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                coVerify { reminderScheduler.cancelReminder(any()) }
            }

        @Test
        fun `save checklist habit sets target to checklist size`() =
            runTest {
                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Checklist Habit"))
                viewModel.processIntent(CreateHabitIntent.UpdateHabitType(HabitType.CHECKLIST))
                viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("Step 1"))
                viewModel.processIntent(CreateHabitIntent.AddChecklistItem)
                viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("Step 2"))
                viewModel.processIntent(CreateHabitIntent.AddChecklistItem)
                viewModel.processIntent(CreateHabitIntent.UpdateNewChecklistItem("Step 3"))
                viewModel.processIntent(CreateHabitIntent.AddChecklistItem)

                coEvery { habitRepository.createHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                coVerify {
                    habitRepository.createHabit(
                        match { it.targetValue == 3 && it.checklist.size == 3 },
                    )
                }
            }
    }

    @Nested
    @DisplayName("Edit mode")
    inner class EditModeTests {
        @Test
        fun `load habit for edit populates form from repository`() =
            runTest {
                val habit =
                    Habit(
                        id = "edit-id",
                        title = "Existing Habit",
                        description = "A description",
                        habitType = HabitType.MEASURABLE,
                        targetValue = 10,
                        unit = "minutes",
                        frequencyType = FrequencyType.SPECIFIC_DAYS,
                        scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                        checklist = emptyList(),
                        reminderEnabled = true,
                        reminderTime = LocalTime.of(7, 30),
                        startDate = LocalDate.now(),
                    )
                coEvery { habitRepository.getHabitById("edit-id") } returns Result.success(habit)

                viewModel.processIntent(CreateHabitIntent.LoadHabitForEdit("edit-id"))
                advanceUntilIdle()

                val state = viewModel.state.value
                assertEquals("edit-id", state.editingHabitId)
                assertEquals("Existing Habit", state.title)
                assertEquals("A description", state.description)
                assertEquals(HabitType.MEASURABLE, state.habitType)
                assertEquals("10", state.targetValue)
                assertEquals("minutes", state.unit)
                assertTrue(state.reminderEnabled)
                assertEquals(LocalTime.of(7, 30), state.reminderTime)
                assertFalse(state.isLoadingHabit)
            }

        @Test
        fun `load habit for edit failure sets error`() =
            runTest {
                coEvery { habitRepository.getHabitById("bad-id") } returns
                    Result.failure(Exception("Not found"))

                viewModel.processIntent(CreateHabitIntent.LoadHabitForEdit("bad-id"))
                advanceUntilIdle()

                assertEquals("Not found", viewModel.state.value.error)
                assertFalse(viewModel.state.value.isLoadingHabit)
            }

        @Test
        fun `save in edit mode calls updateHabit`() =
            runTest {
                coEvery { habitRepository.getHabitById("edit-id") } returns
                    Result.success(
                        Habit(
                            id = "edit-id",
                            title = "Old Title",
                            startDate = LocalDate.now(),
                        ),
                    )
                coEvery { habitRepository.updateHabit(any()) } returns Result.success(Unit)

                viewModel.processIntent(CreateHabitIntent.LoadHabitForEdit("edit-id"))
                advanceUntilIdle()

                viewModel.processIntent(CreateHabitIntent.UpdateTitle("Updated Title"))
                viewModel.processIntent(CreateHabitIntent.SaveHabit)
                advanceUntilIdle()

                coVerify { habitRepository.updateHabit(match { it.id == "edit-id" }) }
                coVerify(exactly = 0) { habitRepository.createHabit(any()) }
            }
    }

    @Nested
    @DisplayName("Clear error")
    inner class ClearErrorTests {
        @Test
        fun `clear error removes error from state`() {
            viewModel.processIntent(CreateHabitIntent.ClearError)

            assertNull(viewModel.state.value.error)
        }
    }
}
