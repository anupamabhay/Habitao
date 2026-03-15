package com.habitao.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.ChecklistItem
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitType
import com.habitao.domain.repository.HabitRepository
import com.habitao.system.notifications.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

// State for Create Habit form
data class CreateHabitState(
    // Edit mode
    val editingHabitId: String? = null,
    val isLoadingHabit: Boolean = false,
    // Form fields
    val title: String = "",
    val description: String = "",
    val habitType: HabitType = HabitType.SIMPLE,
    val targetValue: String = "1",
    val unit: String = "",
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val frequencyValue: String = "3",
    val scheduledDays: Set<DayOfWeek> = emptySet(),
    // Checklist items (for CHECKLIST habit type)
    val checklistItems: List<String> = emptyList(),
    val newChecklistItem: String = "",
    // Reminder settings
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    // Validation errors
    val titleError: String? = null,
    val targetValueError: String? = null,
    val checklistError: String? = null,
    // UI state
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isEditMode: Boolean get() = editingHabitId != null
}

// Intents for Create Habit screen
sealed class CreateHabitIntent {
    data class UpdateTitle(val title: String) : CreateHabitIntent()

    data class UpdateDescription(val description: String) : CreateHabitIntent()

    data class UpdateHabitType(val habitType: HabitType) : CreateHabitIntent()

    data class UpdateTargetValue(val targetValue: String) : CreateHabitIntent()

    data class UpdateUnit(val unit: String) : CreateHabitIntent()

    data class UpdateFrequencyType(val frequencyType: FrequencyType) : CreateHabitIntent()

    data class UpdateFrequencyValue(val frequencyValue: String) : CreateHabitIntent()

    data class ToggleScheduledDay(val day: DayOfWeek) : CreateHabitIntent()

    data class UpdateNewChecklistItem(val text: String) : CreateHabitIntent()

    data object AddChecklistItem : CreateHabitIntent()

    data class RemoveChecklistItem(val index: Int) : CreateHabitIntent()

    data class UpdateChecklistItemText(val index: Int, val newText: String) : CreateHabitIntent()

    data class UpdateReminderEnabled(val enabled: Boolean) : CreateHabitIntent()

    data class UpdateReminderTime(val time: LocalTime) : CreateHabitIntent()

    data object SaveHabit : CreateHabitIntent()

    data object ClearError : CreateHabitIntent()

    data object ResetForm : CreateHabitIntent()

    data class LoadHabitForEdit(val habitId: String) : CreateHabitIntent()
}

@HiltViewModel
class CreateHabitViewModel
    @Inject
    constructor(
        private val habitRepository: HabitRepository,
        private val reminderScheduler: HabitReminderScheduler,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CreateHabitState())
        val state: StateFlow<CreateHabitState> = _state.asStateFlow()
        private val _savedEvent = Channel<Unit>(Channel.BUFFERED)
        val savedEvent = _savedEvent.receiveAsFlow()

        fun processIntent(intent: CreateHabitIntent) {
            when (intent) {
                is CreateHabitIntent.UpdateTitle -> updateTitle(intent.title)
                is CreateHabitIntent.UpdateDescription -> updateDescription(intent.description)
                is CreateHabitIntent.UpdateHabitType -> updateHabitType(intent.habitType)
                is CreateHabitIntent.UpdateTargetValue -> updateTargetValue(intent.targetValue)
                is CreateHabitIntent.UpdateUnit -> updateUnit(intent.unit)
                is CreateHabitIntent.UpdateFrequencyType -> updateFrequencyType(intent.frequencyType)
                is CreateHabitIntent.UpdateFrequencyValue -> updateFrequencyValue(intent.frequencyValue)
                is CreateHabitIntent.ToggleScheduledDay -> toggleScheduledDay(intent.day)
                is CreateHabitIntent.UpdateNewChecklistItem -> updateNewChecklistItem(intent.text)
                is CreateHabitIntent.AddChecklistItem -> addChecklistItem()
                is CreateHabitIntent.RemoveChecklistItem -> removeChecklistItem(intent.index)
                is CreateHabitIntent.UpdateChecklistItemText ->
                    updateChecklistItemText(intent.index, intent.newText)
                is CreateHabitIntent.UpdateReminderEnabled -> updateReminderEnabled(intent.enabled)
                is CreateHabitIntent.UpdateReminderTime -> updateReminderTime(intent.time)
                is CreateHabitIntent.SaveHabit -> saveHabit()
                is CreateHabitIntent.ClearError -> clearError()
                is CreateHabitIntent.ResetForm -> resetForm()
                is CreateHabitIntent.LoadHabitForEdit -> loadHabitForEdit(intent.habitId)
            }
        }

        // Reset form to prevent stale state from previous navigation
        private fun resetForm() {
            _state.value = CreateHabitState()
        }

        // Load an existing habit for editing
        private fun loadHabitForEdit(habitId: String) {
            _state.update { it.copy(isLoadingHabit = true, editingHabitId = habitId) }
            viewModelScope.launch {
                habitRepository.getHabitById(habitId)
                    .onSuccess { habit ->
                        _state.update {
                            it.copy(
                                isLoadingHabit = false,
                                title = habit.title,
                                description = habit.description ?: "",
                                habitType = habit.habitType,
                                targetValue = habit.targetValue.toString(),
                                unit = habit.unit ?: "",
                                frequencyType = habit.frequencyType,
                                frequencyValue = habit.frequencyValue.toString(),
                                scheduledDays = habit.scheduledDays,
                                checklistItems = habit.checklist.map { item -> item.text },
                                reminderEnabled = habit.reminderEnabled,
                                reminderTime = habit.reminderTime,
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isLoadingHabit = false,
                                error = error.message ?: "Failed to load habit",
                            )
                        }
                    }
            }
        }

        private fun updateTitle(title: String) {
            _state.update { it.copy(title = title, titleError = null) }
        }

        private fun updateDescription(description: String) {
            _state.update { it.copy(description = description) }
        }

        private fun updateHabitType(habitType: HabitType) {
            _state.update {
                it.copy(
                    habitType = habitType,
                    targetValue = if (habitType == HabitType.SIMPLE) "1" else it.targetValue,
                    targetValueError = null,
                )
            }
        }

        private fun updateTargetValue(targetValue: String) {
            if (targetValue.isEmpty() || targetValue.all { it.isDigit() }) {
                _state.update { it.copy(targetValue = targetValue, targetValueError = null) }
            }
        }

        private fun updateUnit(unit: String) {
            _state.update { it.copy(unit = unit) }
        }

        private fun updateFrequencyType(frequencyType: FrequencyType) {
            _state.update { it.copy(frequencyType = frequencyType) }
        }

        private fun updateFrequencyValue(frequencyValue: String) {
            if (frequencyValue.isEmpty() || frequencyValue.all { it.isDigit() }) {
                _state.update { it.copy(frequencyValue = frequencyValue) }
            }
        }

        private fun toggleScheduledDay(day: DayOfWeek) {
            _state.update {
                val newDays =
                    if (it.scheduledDays.contains(day)) {
                        it.scheduledDays - day
                    } else {
                        it.scheduledDays + day
                    }
                it.copy(scheduledDays = newDays)
            }
        }

        private fun updateNewChecklistItem(text: String) {
            _state.update { it.copy(newChecklistItem = text) }
        }

        private fun addChecklistItem() {
            val currentState = _state.value
            val itemText = currentState.newChecklistItem.trim()

            if (itemText.isBlank()) return
            if (currentState.checklistItems.size >= 20) {
                _state.update { it.copy(checklistError = "Maximum 20 items allowed") }
                return
            }

            _state.update {
                it.copy(
                    checklistItems = it.checklistItems + itemText,
                    newChecklistItem = "",
                    checklistError = null,
                )
            }
        }

        private fun removeChecklistItem(index: Int) {
            _state.update {
                it.copy(
                    checklistItems = it.checklistItems.filterIndexed { i, _ -> i != index },
                    checklistError = null,
                )
            }
        }

        private fun updateChecklistItemText(
            index: Int,
            newText: String,
        ) {
            _state.update {
                val updated = it.checklistItems.toMutableList()
                if (index in updated.indices) {
                    updated[index] = newText
                }
                it.copy(checklistItems = updated)
            }
        }

        private fun updateReminderEnabled(enabled: Boolean) {
            _state.update {
                it.copy(
                    reminderEnabled = enabled,
                    // Set default time to 9:00 AM when enabling for the first time
                    reminderTime =
                        if (enabled && it.reminderTime == null) {
                            LocalTime.of(9, 0)
                        } else {
                            it.reminderTime
                        },
                )
            }
        }

        private fun updateReminderTime(time: LocalTime) {
            _state.update { it.copy(reminderTime = time) }
        }

        private fun saveHabit() {
            val currentState = _state.value

            val titleError = validateTitle(currentState.title)
            val targetValueError = validateTargetValue(currentState.targetValue, currentState.habitType)
            val checklistError = validateChecklist(currentState.checklistItems, currentState.habitType)

            if (titleError != null || targetValueError != null || checklistError != null) {
                _state.update {
                    it.copy(
                        titleError = titleError,
                        targetValueError = targetValueError,
                        checklistError = checklistError,
                    )
                }
                return
            }

            viewModelScope.launch {
                _state.update { it.copy(isSaving = true) }

                // Build checklist items with proper domain model
                val checklist =
                    currentState.checklistItems.mapIndexed { index, text ->
                        ChecklistItem(text = text, sortOrder = index)
                    }

                // For checklist habits, target is number of items
                val targetValue =
                    when (currentState.habitType) {
                        HabitType.CHECKLIST -> checklist.size
                        else -> currentState.targetValue.toIntOrNull() ?: 1
                    }

                // Use existing ID if editing, otherwise generate new
                val habitId = currentState.editingHabitId ?: java.util.UUID.randomUUID().toString()

                val habit =
                    Habit(
                        id = habitId,
                        title = currentState.title.trim(),
                        description = currentState.description.trim().takeIf { it.isNotEmpty() },
                        habitType = currentState.habitType,
                        targetValue = targetValue,
                        unit = currentState.unit.trim().takeIf { it.isNotEmpty() },
                        frequencyType = currentState.frequencyType,
                        frequencyValue = currentState.frequencyValue.toIntOrNull() ?: 1,
                        scheduledDays = currentState.scheduledDays,
                        checklist = checklist,
                        reminderEnabled = currentState.reminderEnabled,
                        reminderTime = currentState.reminderTime,
                    )

                val result =
                    if (currentState.isEditMode) {
                        habitRepository.updateHabit(habit)
                    } else {
                        habitRepository.createHabit(habit)
                    }

                result
                    .onSuccess {
                        val reminderTime = habit.reminderTime
                        if (habit.reminderEnabled && reminderTime != null) {
                            reminderScheduler.scheduleReminder(
                                habitId = habit.id,
                                habitTitle = habit.title,
                                time = reminderTime,
                                frequencyType = habit.frequencyType,
                                scheduledDays = habit.scheduledDays,
                            )
                        } else {
                            reminderScheduler.cancelReminder(habit.id)
                        }
                        _state.update { it.copy(isSaving = false) }
                        _savedEvent.send(Unit)
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = error.message ?: "Failed to save habit",
                            )
                        }
                    }
            }
        }

        private fun validateChecklist(
            items: List<String>,
            habitType: HabitType,
        ): String? {
            if (habitType != HabitType.CHECKLIST) return null
            return when {
                items.isEmpty() -> "Add at least one checklist item"
                else -> null
            }
        }

        private fun clearError() {
            _state.update { it.copy(error = null) }
        }

        private fun validateTitle(title: String): String? {
            return when {
                title.isBlank() -> "Habit name is required"
                title.length < 2 -> "Name must be at least 2 characters"
                title.length > 100 -> "Name must be less than 100 characters"
                else -> null
            }
        }

        private fun validateTargetValue(
            targetValue: String,
            habitType: HabitType,
        ): String? {
            if (habitType == HabitType.SIMPLE) {
                return null
            }

            val value = targetValue.toIntOrNull()
            return when {
                targetValue.isBlank() -> "Target is required"
                value == null -> "Must be a number"
                value < 1 -> "Target must be at least 1"
                value > 10000 -> "Target is too large"
                else -> null
            }
        }
    }
