package com.habitao.feature.habits.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.HabitType
import com.habitao.feature.habits.viewmodel.CreateHabitIntent
import com.habitao.feature.habits.viewmodel.CreateHabitState
import com.habitao.feature.habits.viewmodel.CreateHabitViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitScreen(
    onNavigateBack: () -> Unit,
    onHabitCreated: () -> Unit,
    habitId: String? = null,
    viewModel: CreateHabitViewModel = hiltViewModel(key = habitId ?: "create"),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Reset form or load habit when entering the screen
    LaunchedEffect(habitId) {
        if (habitId != null) {
            viewModel.processIntent(CreateHabitIntent.LoadHabitForEdit(habitId))
        } else {
            viewModel.processIntent(CreateHabitIntent.ResetForm)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            onHabitCreated()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.processIntent(CreateHabitIntent.ClearError)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Habit" else "New Habit",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            Button(
                onClick = { viewModel.processIntent(CreateHabitIntent.SaveHabit) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = Dimensions.screenPaddingHorizontal,
                            vertical = Dimensions.elementSpacingLarge,
                        ),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(
                    text =
                        when {
                            state.isSaving -> "Saving..."
                            state.isEditMode -> "Save Changes"
                            else -> "Create Habit"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { paddingValues ->
        CreateHabitForm(
            state = state,
            onIntent = viewModel::processIntent,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun CreateHabitForm(
    state: CreateHabitState,
    onIntent: (CreateHabitIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputShape = RoundedCornerShape(12.dp)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Basic Info Section
        FormSection(title = "Basics") {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onIntent(CreateHabitIntent.UpdateTitle(it)) },
                label = { Text("Habit name") },
                placeholder = { Text("e.g., Morning run, Read 30 min") },
                singleLine = true,
                isError = state.titleError != null,
                supportingText = state.titleError?.let { { Text(it) } },
                shape = inputShape,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { onIntent(CreateHabitIntent.UpdateDescription(it)) },
                label = { Text("Description (optional)") },
                placeholder = { Text("Why is this habit important to you?") },
                minLines = 2,
                maxLines = 3,
                shape = inputShape,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Habit Type Section
        FormSection(title = "Tracking style") {
            HabitTypeSelector(
                selectedType = state.habitType,
                onTypeSelected = { onIntent(CreateHabitIntent.UpdateHabitType(it)) },
            )

            // Conditional fields for MEASURABLE type
            AnimatedVisibility(
                visible = state.habitType == HabitType.MEASURABLE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = state.targetValue,
                            onValueChange = { onIntent(CreateHabitIntent.UpdateTargetValue(it)) },
                            label = { Text("Target") },
                            placeholder = { Text("10") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = state.targetValueError != null,
                            supportingText = state.targetValueError?.let { { Text(it) } },
                            shape = inputShape,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                            modifier = Modifier.weight(1f),
                        )

                        OutlinedTextField(
                            value = state.unit,
                            onValueChange = { onIntent(CreateHabitIntent.UpdateUnit(it)) },
                            label = { Text("Unit") },
                            placeholder = { Text("glasses, pages") },
                            singleLine = true,
                            shape = inputShape,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Conditional fields for CHECKLIST type
            AnimatedVisibility(
                visible = state.habitType == HabitType.CHECKLIST,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val checklistFocusRequester = remember { FocusRequester() }
                val addChecklistItem = {
                    onIntent(CreateHabitIntent.AddChecklistItem)
                    checklistFocusRequester.requestFocus()
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Existing checklist items
                    state.checklistItems.forEachIndexed { index, item ->
                        ChecklistItemRow(
                            text = item,
                            onTextChange = { newText ->
                                onIntent(CreateHabitIntent.UpdateChecklistItemText(index, newText))
                            },
                            onRemove = { onIntent(CreateHabitIntent.RemoveChecklistItem(index)) },
                        )
                    }

                    // Add new item input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.newChecklistItem,
                            onValueChange = { onIntent(CreateHabitIntent.UpdateNewChecklistItem(it)) },
                            label = { Text("Add item") },
                            placeholder = { Text("e.g., Brush teeth") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addChecklistItem() }),
                            isError = state.checklistError != null,
                            supportingText = state.checklistError?.let { { Text(it) } },
                            shape = inputShape,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                            modifier = Modifier.weight(1f).focusRequester(checklistFocusRequester),
                        )

                        FilledTonalIconButton(
                            onClick = { addChecklistItem() },
                            enabled = state.newChecklistItem.isNotBlank(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add item",
                            )
                        }
                    }

                    if (state.checklistItems.isNotEmpty()) {
                        Text(
                            text =
                                "${state.checklistItems.size} item" +
                                    "${if (state.checklistItems.size != 1) "s" else ""} added",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Frequency Section
        FormSection(title = "Schedule") {
            FrequencyTypeSelector(
                selectedType = state.frequencyType,
                onTypeSelected = { onIntent(CreateHabitIntent.UpdateFrequencyType(it)) },
            )

            // Conditional: Day picker for SPECIFIC_DAYS
            AnimatedVisibility(
                visible = state.frequencyType == FrequencyType.SPECIFIC_DAYS,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    DaySelector(
                        selectedDays = state.scheduledDays,
                        onDayToggled = { onIntent(CreateHabitIntent.ToggleScheduledDay(it)) },
                    )
                }
            }

            // Conditional: Frequency value for TIMES_PER_WEEK
            AnimatedVisibility(
                visible = state.frequencyType == FrequencyType.TIMES_PER_WEEK,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    FrequencyValueInput(
                        value = state.frequencyValue,
                        onValueChange = { onIntent(CreateHabitIntent.UpdateFrequencyValue(it)) },
                        label = "Times per week",
                        placeholder = "3",
                        shape = inputShape,
                    )
                }
            }

            // Conditional: Frequency value for EVERY_X_DAYS
            AnimatedVisibility(
                visible = state.frequencyType == FrequencyType.EVERY_X_DAYS,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    FrequencyValueInput(
                        value = state.frequencyValue,
                        onValueChange = { onIntent(CreateHabitIntent.UpdateFrequencyValue(it)) },
                        label = "Every X days",
                        placeholder = "2",
                        shape = inputShape,
                    )
                }
            }
        }

        // Reminders Section
        FormSection(title = "Reminders") {
            ReminderSection(
                reminderEnabled = state.reminderEnabled,
                reminderTime = state.reminderTime,
                onReminderEnabledChange = { onIntent(CreateHabitIntent.UpdateReminderEnabled(it)) },
                onReminderTimeChange = { onIntent(CreateHabitIntent.UpdateReminderTime(it)) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitTypeSelector(
    selectedType: HabitType,
    onTypeSelected: (HabitType) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HabitType.entries.forEachIndexed { index, habitType ->
            SegmentedButton(
                selected = selectedType == habitType,
                onClick = { onTypeSelected(habitType) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = HabitType.entries.size,
                        baseShape = RoundedCornerShape(12.dp),
                    ),
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                icon = {
                    SegmentedButtonDefaults.Icon(
                        active = selectedType == habitType,
                        activeContent = {
                            Icon(
                                imageVector = habitType.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        inactiveContent = {
                            Icon(
                                imageVector = habitType.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                },
            ) {
                Text(
                    text = habitType.displayName(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyTypeSelector(
    selectedType: FrequencyType,
    onTypeSelected: (FrequencyType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // First row: DAILY and SPECIFIC_DAYS
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val firstRowTypes = listOf(FrequencyType.DAILY, FrequencyType.SPECIFIC_DAYS)
            firstRowTypes.forEachIndexed { index, frequencyType ->
                SegmentedButton(
                    selected = selectedType == frequencyType,
                    onClick = { onTypeSelected(frequencyType) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = firstRowTypes.size,
                            baseShape = RoundedCornerShape(12.dp),
                        ),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Text(
                        text = frequencyType.displayName(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Second row: TIMES_PER_WEEK and EVERY_X_DAYS
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val secondRowTypes = listOf(FrequencyType.TIMES_PER_WEEK, FrequencyType.EVERY_X_DAYS)
            secondRowTypes.forEachIndexed { index, frequencyType ->
                SegmentedButton(
                    selected = selectedType == frequencyType,
                    onClick = { onTypeSelected(frequencyType) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = secondRowTypes.size,
                            baseShape = RoundedCornerShape(12.dp),
                        ),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Text(
                        text = frequencyType.displayName(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = selectedDays.contains(day)
                FilterChip(
                    selected = isSelected,
                    onClick = { onDayToggled(day) },
                    label = {
                        Text(
                            text = day.shortName.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FrequencyValueInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    shape: RoundedCornerShape,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = shape,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun HabitType.displayName(): String =
    when (this) {
        HabitType.SIMPLE -> "Yes/No"
        HabitType.MEASURABLE -> "Number"
        HabitType.CHECKLIST -> "Checklist"
    }

private fun HabitType.icon(): ImageVector =
    when (this) {
        HabitType.SIMPLE -> Icons.Outlined.CheckCircleOutline
        HabitType.MEASURABLE -> Icons.Outlined.Numbers
        HabitType.CHECKLIST -> Icons.Outlined.Checklist
    }

@Composable
private fun ChecklistItemRow(
    text: String,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp),
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove item",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun FrequencyType.displayName(): String =
    when (this) {
        FrequencyType.DAILY -> "Daily"
        FrequencyType.SPECIFIC_DAYS -> "Specific days"
        FrequencyType.TIMES_PER_WEEK -> "X per week"
        FrequencyType.EVERY_X_DAYS -> "Every X days"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection(
    reminderEnabled: Boolean,
    reminderTime: LocalTime?,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Enable/disable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Remind me",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Get notified based on schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onReminderEnabledChange,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                )
            }

            // Time picker (visible when enabled)
            AnimatedVisibility(
                visible = reminderEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Surface(
                    onClick = { showTimePicker = true },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Reminder time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Text(
                            text = reminderTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "9:00 AM",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = reminderTime ?: LocalTime.of(9, 0),
            onTimeSelected = {
                onReminderTimeChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false,
        )

    var showKeyboardInput by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Select time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { showKeyboardInput = !showKeyboardInput }) {
                        Icon(
                            imageVector =
                                if (showKeyboardInput) {
                                    Icons.Outlined.Schedule
                                } else {
                                    Icons.Outlined.Keyboard
                                },
                            contentDescription =
                                if (showKeyboardInput) {
                                    "Switch to dial input"
                                } else {
                                    "Switch to keyboard input"
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val timePickerColors =
                    TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )

                if (showKeyboardInput) {
                    TimeInput(
                        state = timePickerState,
                        colors = timePickerColors,
                    )
                } else {
                    TimePicker(
                        state = timePickerState,
                        colors = timePickerColors,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        },
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
