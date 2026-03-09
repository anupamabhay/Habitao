package com.habitao.feature.tasks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.components.MarkdownVisualTransformation
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.TaskPriority
import com.habitao.feature.tasks.viewmodel.CreateTaskIntent
import com.habitao.feature.tasks.viewmodel.CreateTaskState
import com.habitao.feature.tasks.viewmodel.CreateTaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onNavigateBack: () -> Unit,
    onTaskCreated: () -> Unit,
    taskId: String? = null,
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(taskId) {
        viewModel.processIntent(CreateTaskIntent.ResetForm)
        if (taskId != null) {
            viewModel.processIntent(CreateTaskIntent.LoadTask(taskId))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            onTaskCreated()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.processIntent(CreateTaskIntent.ClearError)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Task" else "New Task",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = { viewModel.processIntent(CreateTaskIntent.SaveTask) },
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
                            else -> "Create Task"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { paddingValues ->
        CreateTaskForm(
            state = state,
            onIntent = viewModel::processIntent,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun CreateTaskForm(
    state: CreateTaskState,
    onIntent: (CreateTaskIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputShape = RoundedCornerShape(16.dp)

    var subtaskIdToFocus by remember { mutableStateOf<String?>(null) }
    var previousSubtaskIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentSubtaskIds = state.subtasks.map { it.id }

    LaunchedEffect(currentSubtaskIds) {
        val addedIds = currentSubtaskIds.filterNot { it in previousSubtaskIds }
        if (addedIds.isNotEmpty()) {
            subtaskIdToFocus = addedIds.last()
        }
        previousSubtaskIds = currentSubtaskIds
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.screenPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(Dimensions.sectionSpacing),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Basics Section (Borderless)
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingLarge)) {
            BasicTextField(
                value = state.title,
                onValueChange = { onIntent(CreateTaskIntent.SetTitle(it)) },
                textStyle =
                    MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (state.title.isEmpty()) {
                        Text(
                            text = "What would you like to do?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                },
            )

            MarkdownDescriptionField(
                value = state.description,
                onValueChange = { onIntent(CreateTaskIntent.SetDescription(it)) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Schedule Section
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)) {
            SectionHeader("Schedule")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingLarge),
            ) {
                DatePickerField(
                    date = state.dueDate,
                    onDateSelected = { onIntent(CreateTaskIntent.SetDueDate(it)) },
                    modifier = Modifier.weight(1f),
                )

                TimePickerField(
                    time = state.dueTime,
                    onTimeSelected = { onIntent(CreateTaskIntent.SetDueTime(it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Priority Section
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)) {
            SectionHeader("Priority")
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
            ) {
                TaskPriority.entries.forEach { priority ->
                    PriorityChip(
                        priority = priority,
                        isSelected = state.priority == priority,
                        onClick = { onIntent(CreateTaskIntent.SetPriority(priority)) },
                    )
                }
            }
        }

        // Reminder Section
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)) {
            SectionHeader("Reminders")
            Surface(
                shape = inputShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            ) {
                Column {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Enable Reminder",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = { onIntent(CreateTaskIntent.SetReminderEnabled(it)) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }

                    AnimatedVisibility(visible = state.reminderEnabled) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val options =
                                listOf(
                                    0 to "At time",
                                    10 to "10 min",
                                    30 to "30 min",
                                    60 to "1 hour",
                                )
                            options.forEach { (minutes, label) ->
                                FilterChip(
                                    selected = state.reminderMinutesBefore == minutes,
                                    onClick = { onIntent(CreateTaskIntent.SetReminderMinutesBefore(minutes)) },
                                    label = { Text(label) },
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            selectedContainerColor =
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.1f,
                                                ),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    border =
                                        FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = state.reminderMinutesBefore == minutes,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtasks Section
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)) {
            SectionHeader("Subtasks")

            state.subtasks.forEach { subtask ->
                key(subtask.id) {
                    SubtaskRow(
                        text = subtask.text,
                        priority = subtask.priority,
                        onTextChange = { onIntent(CreateTaskIntent.UpdateSubtaskText(subtask.id, it)) },
                        onPriorityChange = { onIntent(CreateTaskIntent.UpdateSubtaskPriority(subtask.id, it)) },
                        onRemove = { onIntent(CreateTaskIntent.RemoveSubtask(subtask.id)) },
                        onSubmit = { onIntent(CreateTaskIntent.AddSubtask) },
                        shouldRequestFocus = subtask.id == subtaskIdToFocus,
                        onFocusRequested = {
                            if (subtaskIdToFocus == subtask.id) {
                                subtaskIdToFocus = null
                            }
                        },
                    )
                }
            }

            Surface(
                onClick = { onIntent(CreateTaskIntent.AddSubtask) },
                shape = inputShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add subtask",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.fabClearance + 32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun MarkdownFormattingToolbar(
    onFormat: (prefix: String, suffix: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FormatIconButton(Icons.Outlined.FormatBold, "Bold") { onFormat("**", "**") }
        FormatIconButton(Icons.Outlined.FormatItalic, "Italic") { onFormat("*", "*") }
        FormatIconButton(Icons.Outlined.FormatStrikethrough, "Strikethrough") { onFormat("~~", "~~") }
        FormatIconButton(Icons.Outlined.Code, "Inline code") { onFormat("`", "`") }
        FormatIconButton(Icons.Outlined.Title, "Heading") { onFormat("# ", "") }
        FormatIconButton(Icons.Outlined.FormatListBulleted, "Bullet list") { onFormat("- ", "") }
        FormatIconButton(Icons.Outlined.CheckBoxOutlineBlank, "Checkbox") { onFormat("[ ] ", "") }
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(Dimensions.actionButtonSecondary),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimensions.iconSizeSmall),
            )
        }
    }
}

@Composable
private fun MarkdownDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markdownTransformation =
        remember(baseColor) {
            MarkdownVisualTransformation(baseColor)
        }

    // No key on remember — preserves cursor position across recompositions triggered by
    // the parent emitting a new state after each keystroke.
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
        )
    }

    // Sync only when an external source changes the text (e.g., field reset from the ViewModel).
    // During normal typing the internal text already matches `value`, so this is a no-op.
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            val safeCursor = textFieldValue.selection.start.coerceAtMost(value.length)
            textFieldValue = TextFieldValue(text = value, selection = TextRange(safeCursor))
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newTfv ->
            val oldText = textFieldValue.text
            val result = handleMarkdownAutoFormat(oldText, newTfv.text, newTfv.selection)
            textFieldValue = result
            if (result.text != value) {
                onValueChange(result.text)
            }
        },
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color = baseColor,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        visualTransformation = markdownTransformation,
        decorationBox = { innerTextField ->
            Column {
                if (value.isEmpty()) {
                    Text(
                        text = "Add description... (supports markdown)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
                Spacer(modifier = Modifier.height(8.dp))
                MarkdownFormattingToolbar(
                    onFormat = { prefix, suffix ->
                        val sel = textFieldValue.selection
                        val text = textFieldValue.text
                        if (sel.start != sel.end) {
                            // Wrap selected text
                            val before = text.substring(0, sel.start)
                            val selected = text.substring(sel.start, sel.end)
                            val after = text.substring(sel.end)
                            val newText = before + prefix + selected + suffix + after
                            val newCursor = sel.end + prefix.length + suffix.length
                            textFieldValue =
                                TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursor),
                                )
                            onValueChange(newText)
                        } else {
                            // Insert at cursor for line-level markers (like - , [ ] )
                            val before = text.substring(0, sel.start)
                            val after = text.substring(sel.start)
                            val newText = before + prefix + suffix + after
                            val newCursor = sel.start + prefix.length
                            textFieldValue =
                                TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursor),
                                )
                            onValueChange(newText)
                        }
                    },
                )
            }
        },
    )
}

private fun handleMarkdownAutoFormat(
    oldText: String,
    newText: String,
    currentSelection: TextRange,
): TextFieldValue {
    // Only trigger on newline insertion (user pressed Enter)
    if (newText.length <= oldText.length) {
        return TextFieldValue(text = newText, selection = currentSelection)
    }
    val insertedCount = newText.length - oldText.length
    if (insertedCount != 1) {
        return TextFieldValue(text = newText, selection = currentSelection)
    }

    // Find where the newline was inserted
    val diffIndex =
        newText.indices.firstOrNull { i ->
            i >= oldText.length || newText[i] != oldText[i]
        } ?: return TextFieldValue(text = newText, selection = currentSelection)

    if (newText[diffIndex] != '\n') {
        return TextFieldValue(text = newText, selection = currentSelection)
    }

    // Get the line BEFORE the newline
    val beforeNewline = newText.substring(0, diffIndex)
    val prevLineStart = beforeNewline.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
    val prevLine = beforeNewline.substring(prevLineStart)
    val trimmedPrev = prevLine.trimStart()

    // Unordered list: "- text" -> auto-add "- " on next line
    if (trimmedPrev.startsWith("- ") && trimmedPrev.length > 2) {
        val after = newText.substring(diffIndex + 1)
        val result = beforeNewline + "\n- " + after
        return TextFieldValue(text = result, selection = TextRange(diffIndex + 3))
    }

    // Empty unordered list marker: just "- " with no text -> remove it
    if (trimmedPrev == "-" || trimmedPrev == "- ") {
        val beforePrevLine = newText.substring(0, prevLineStart)
        val after = newText.substring(diffIndex + 1)
        val result = beforePrevLine + after
        return TextFieldValue(text = result, selection = TextRange(beforePrevLine.length))
    }

    // Numbered list: "1. text" -> auto-add "2. " on next line
    val numberedMatch = Regex("^(\\d+)\\.\\s(.+)$").find(trimmedPrev)
    if (numberedMatch != null) {
        val nextNumber = (numberedMatch.groupValues[1].toIntOrNull() ?: 0) + 1
        val prefix = "$nextNumber. "
        val after = newText.substring(diffIndex + 1)
        val result = beforeNewline + "\n" + prefix + after
        return TextFieldValue(
            text = result,
            selection = TextRange(diffIndex + 1 + prefix.length),
        )
    }

    // Empty numbered list marker: "1. " with no text -> remove it
    val emptyNumberedMatch = Regex("^(\\d+)\\.\\s?$").find(trimmedPrev)
    if (emptyNumberedMatch != null) {
        val beforePrevLine = newText.substring(0, prevLineStart)
        val after = newText.substring(diffIndex + 1)
        val result = beforePrevLine + after
        return TextFieldValue(text = result, selection = TextRange(beforePrevLine.length))
    }

    // Checkbox: "[ ] text" -> auto-add "[ ] " on next line
    if (trimmedPrev.startsWith("[ ] ") && trimmedPrev.length > 4) {
        val after = newText.substring(diffIndex + 1)
        val result = beforeNewline + "\n[ ] " + after
        return TextFieldValue(text = result, selection = TextRange(diffIndex + 5))
    }

    // Checked checkbox: "[x] text" -> auto-add "[ ] " on next line
    if ((trimmedPrev.startsWith("[x] ") || trimmedPrev.startsWith("[X] ")) && trimmedPrev.length > 4) {
        val after = newText.substring(diffIndex + 1)
        val result = beforeNewline + "\n[ ] " + after
        return TextFieldValue(text = result, selection = TextRange(diffIndex + 5))
    }

    // Empty checkbox: "[ ] " -> remove
    if (trimmedPrev == "[ ]" || trimmedPrev == "[ ] ") {
        val beforePrevLine = newText.substring(0, prevLineStart)
        val after = newText.substring(diffIndex + 1)
        val result = beforePrevLine + after
        return TextFieldValue(text = result, selection = TextRange(beforePrevLine.length))
    }

    return TextFieldValue(text = newText, selection = currentSelection)
}

@Composable
private fun PriorityChip(
    priority: TaskPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priorityColor = priorityColor(priority)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100),
        color =
            if (isSelected) {
                priorityColor.copy(
                    alpha = 0.15f,
                )
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            },
        border =
            BorderStroke(
                1.dp,
                if (isSelected) {
                    priorityColor.copy(
                        alpha = 0.5f,
                    )
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                },
            ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(priorityColor),
            )
            Text(
                text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) priorityColor else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun priorityColor(priority: TaskPriority): Color {
    return when (priority) {
        TaskPriority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskPriority.LOW -> Color(0xFF1E88E5)
        TaskPriority.MEDIUM -> Color(0xFFFFB300)
        TaskPriority.HIGH -> Color(0xFFE53935)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    date: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog = true },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint =
                    if (date != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                text = date?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Date",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (date != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }

    if (showDialog) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val selectedDate =
                            Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        onDateSelected(selectedDate)
                    } else {
                        onDateSelected(null)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    time: LocalTime?,
    onTimeSelected: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog = true },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint =
                    if (time != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                text = time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "Time",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (time != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }

    if (showDialog) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = time?.hour ?: 9,
                initialMinute = time?.minute ?: 0,
                is24Hour = false,
            )
        var showKeyboardInput by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showDialog = false }) {
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
                                contentDescription = "Toggle input mode",
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
                        TextButton(onClick = {
                            onTimeSelected(null)
                            showDialog = false
                        }) {
                            Text("Clear")
                        }
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                                showDialog = false
                            },
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    text: String,
    priority: TaskPriority,
    onTextChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onRemove: () -> Unit,
    onSubmit: () -> Unit,
    shouldRequestFocus: Boolean,
    onFocusRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = false,
            onCheckedChange = null,
            colors =
                CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            modifier = Modifier.size(24.dp),
        )

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onSubmit() }),
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyUp &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            onSubmit()
                            true
                        } else {
                            false
                        }
                    },
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = "New subtask",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            },
        )

        SubtaskPrioritySelector(
            selectedPriority = priority,
            onPrioritySelected = onPriorityChange,
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove subtask",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubtaskPrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskPriority.entries.forEach { priority ->
            val isSelected = selectedPriority == priority
            val dotColor = priorityColor(priority)

            Surface(
                onClick = { onPrioritySelected(priority) },
                shape = CircleShape,
                color = dotColor.copy(alpha = if (isSelected) 1f else 0.35f),
                border =
                    BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color =
                            if (isSelected) {
                                dotColor
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            },
                    ),
                modifier = Modifier.size(if (isSelected) 16.dp else 14.dp),
            ) {}
        }
    }
}
