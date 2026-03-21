package com.habitao.feature.tasks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.icons.outlined.FormatIndentDecrease
import androidx.compose.material.icons.outlined.FormatIndentIncrease
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Undo
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.koin.compose.viewmodel.koinViewModel
import com.habitao.core.ui.components.MarkdownVisualTransformation
import com.habitao.core.ui.components.findInlineRegions
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.TaskPriority
import com.habitao.feature.tasks.viewmodel.CreateTaskIntent
import com.habitao.feature.tasks.viewmodel.CreateTaskState
import com.habitao.feature.tasks.viewmodel.CreateTaskViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Tracks text states for undo/redo with debounced checkpointing. */
private class UndoRedoManager(private val maxHistory: Int = 50) {
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastCheckpoint = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Push a checkpoint. Debounces rapid keystrokes (>400ms gap or structural change). */
    fun checkpoint(
        state: TextFieldValue,
        force: Boolean = false,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - lastCheckpoint
        if (!force && elapsed < 400 && undoStack.isNotEmpty()) return
        if (undoStack.lastOrNull()?.text == state.text) return
        undoStack.add(state)
        if (undoStack.size > maxHistory) undoStack.removeAt(0)
        redoStack.clear()
        lastCheckpoint = now
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        redoStack.add(current)
        return undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        undoStack.add(current)
        return redoStack.removeAt(redoStack.lastIndex)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onNavigateBack: () -> Unit,
    onTaskCreated: () -> Unit,
    taskId: String? = null,
    viewModel: CreateTaskViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    // Hoisted description state so bottom action row can edit markdown.
    var descriptionFocused by remember { mutableStateOf(false) }
    var descriptionTfv by remember {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }
    val undoRedoManager = remember { UndoRedoManager() }

    // IME-based toolbar visibility: tracks keyboard state independently of focus
    // so the toolbar doesn't vanish during scroll-induced focus loss.
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var lastFocusedField by remember { mutableStateOf<String?>(null) }
    val showToolbar = descriptionFocused || (isImeVisible && lastFocusedField == "description")

    // Sync ViewModel → local TextFieldValue (on load/reset only)
    LaunchedEffect(state.description) {
        if (descriptionTfv.text != state.description) {
            val safeCursor = descriptionTfv.selection.start.coerceAtMost(state.description.length)
            descriptionTfv =
                TextFieldValue(text = state.description, selection = TextRange(safeCursor))
        }
    }

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
        contentWindowInsets = WindowInsets(0),
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
                actions = {
                    if (showToolbar) {
                        TextButton(
                            onClick = { viewModel.processIntent(CreateTaskIntent.SaveTask) },
                            enabled = !state.isSaving,
                        ) {
                            Text(
                                text =
                                    when {
                                        state.isSaving -> "Saving..."
                                        state.isEditMode -> "Save"
                                        else -> "Create"
                                    },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            CreateTaskForm(
                state = state,
                onIntent = viewModel::processIntent,
                descriptionTfv = descriptionTfv,
                onDescriptionTfvChange = { newTfv ->
                    undoRedoManager.checkpoint(descriptionTfv)
                    descriptionTfv = newTfv
                    viewModel.processIntent(CreateTaskIntent.SetDescription(newTfv.text))
                },
                onDescriptionFocusChange = { focused ->
                    descriptionFocused = focused
                    if (focused) {
                        lastFocusedField = "description"
                    }
                },
                onTitleFocusChange = { focused ->
                    if (focused) {
                        descriptionFocused = false
                        lastFocusedField = "title"
                    }
                },
                onSubtaskFocusChange = { focused ->
                    if (focused) {
                        descriptionFocused = false
                        lastFocusedField = "subtask"
                    }
                },
                modifier = Modifier.weight(1f),
            )

            if (showToolbar) {
                MarkdownToolbar(
                    textFieldValue = descriptionTfv,
                    onTextFieldValueChange = { newTfv ->
                        descriptionTfv = newTfv
                        viewModel.processIntent(CreateTaskIntent.SetDescription(newTfv.text))
                    },
                    undoRedoManager = undoRedoManager,
                    onDismiss = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
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
            }
        }
    }
}

@Composable
private fun CreateTaskForm(
    state: CreateTaskState,
    onIntent: (CreateTaskIntent) -> Unit,
    descriptionTfv: TextFieldValue,
    onDescriptionTfvChange: (TextFieldValue) -> Unit,
    onDescriptionFocusChange: (Boolean) -> Unit,
    onTitleFocusChange: (Boolean) -> Unit,
    onSubtaskFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputShape = RoundedCornerShape(16.dp)
    val scrollState = rememberScrollState()

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
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            onTitleFocusChange(focusState.isFocused)
                        },
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
                textFieldValue = descriptionTfv,
                onTextFieldValueChange = onDescriptionTfvChange,
                onFocusChange = onDescriptionFocusChange,
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
                        onFocusChange = onSubtaskFocusChange,
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

        Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))
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

/** Formatting toolbar that sits above the keyboard. */
@Composable
private fun MarkdownToolbar(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    undoRedoManager: UndoRedoManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val applyFormat: (String, String) -> Unit = { prefix, suffix ->
        val sel = textFieldValue.selection
        val text = textFieldValue.text
        val safeStart = sel.start.coerceIn(0, text.length)
        val safeEnd = sel.end.coerceIn(0, text.length)
        undoRedoManager.checkpoint(textFieldValue, force = true)
        if (safeStart != safeEnd) {
            val before = text.substring(0, safeStart)
            val selected = text.substring(safeStart, safeEnd)
            val after = text.substring(safeEnd)
            val newText = before + prefix + selected + suffix + after
            val newCursor = safeEnd + prefix.length + suffix.length
            onTextFieldValueChange(
                TextFieldValue(text = newText, selection = TextRange(newCursor)),
            )
        } else {
            val before = text.substring(0, safeStart)
            val after = text.substring(safeStart)
            val newText = before + prefix + suffix + after
            val newCursor = safeStart + prefix.length
            onTextFieldValueChange(
                TextFieldValue(text = newText, selection = TextRange(newCursor)),
            )
        }
    }

    // Indent current line: if list marker present, nest it; otherwise insert spaces
    val indentCurrentLine: () -> Unit = {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        undoRedoManager.checkpoint(textFieldValue, force = true)

        val lineStart =
            text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
                if (it == -1) 0 else it + 1
            }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val trimmedLine = line.trimStart()

        // Check if line has a list marker that should be indented as a whole
        val isListLine =
            trimmedLine.startsWith("- ") ||
                trimmedLine.startsWith("[ ] ") ||
                trimmedLine.startsWith("[x] ") ||
                trimmedLine.startsWith("[X] ") ||
                trimmedLine.matches(Regex("^\\d+\\.\\s.*"))

        if (isListLine) {
            // Indent the entire line (marker + content) by prepending 2 spaces
            val newLine = "  $line"
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            onTextFieldValueChange(
                TextFieldValue(
                    text = newText,
                    selection = TextRange((cursor + 2).coerceAtMost(newText.length)),
                ),
            )
        } else {
            // Plain text: insert 4 spaces at cursor
            applyFormat("    ", "")
        }
    }

    // Outdent current line: remove up to 2 leading spaces
    val outdentCurrentLine: () -> Unit = {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        undoRedoManager.checkpoint(textFieldValue, force = true)

        val lineStart =
            text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
                if (it == -1) 0 else it + 1
            }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        // Remove up to 2 leading spaces
        val spacesToRemove =
            when {
                line.startsWith("  ") -> 2
                line.startsWith(" ") -> 1
                else -> 0
            }

        if (spacesToRemove > 0) {
            val newLine = line.substring(spacesToRemove)
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            onTextFieldValueChange(
                TextFieldValue(
                    text = newText,
                    selection = TextRange((cursor - spacesToRemove).coerceAtLeast(lineStart)),
                ),
            )
        }
    }

    // Toggle checkbox on current line
    val toggleCheckbox: () -> Unit = {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        undoRedoManager.checkpoint(textFieldValue, force = true)

        val lineStart =
            text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
                if (it == -1) 0 else it + 1
            }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val trimmedLine = line.trimStart()
        val lineIndent = line.length - trimmedLine.length
        val linePrefix = line.substring(0, lineIndent)

        // 3-state cycle: none -> [ ] -> [x] -> none (remove)
        val (newLine, cursorDelta) =
            when {
                trimmedLine.startsWith("[x] ") || trimmedLine.startsWith("[X] ") -> {
                    // Checked -> remove checkbox entirely
                    linePrefix + trimmedLine.substring(4) to -4
                }
                trimmedLine.startsWith("[ ] ") -> {
                    // Unchecked -> check
                    linePrefix + "[x] " + trimmedLine.substring(4) to 0
                }
                else -> {
                    // No checkbox -> add unchecked
                    linePrefix + "[ ] " + trimmedLine to 4
                }
            }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange((cursor + cursorDelta).coerceIn(0, newText.length)),
            ),
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimensions.toolbarHeight)
                    .padding(horizontal = Dimensions.elementSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Scrollable formatting buttons
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarIcon(Icons.Outlined.FormatSize, "Heading") { applyFormat("# ", "") }
                ToolbarIcon(Icons.Outlined.FormatBold, "Bold") { applyFormat("**", "**") }
                ToolbarIcon(Icons.Outlined.FormatItalic, "Italic") { applyFormat("*", "*") }
                ToolbarIcon(Icons.Outlined.FormatStrikethrough, "Strike") { applyFormat("~~", "~~") }
                ToolbarIcon(Icons.Outlined.Code, "Code") { applyFormat("`", "`") }
                ToolbarIcon(Icons.Outlined.FormatListBulleted, "List") { applyFormat("- ", "") }
                ToolbarIcon(Icons.Outlined.CheckBoxOutlineBlank, "Checkbox", onClick = toggleCheckbox)
                ToolbarIcon(Icons.Outlined.FormatIndentIncrease, "Indent", onClick = indentCurrentLine)
                ToolbarIcon(Icons.Outlined.FormatIndentDecrease, "Outdent", onClick = outdentCurrentLine)
            }

            Spacer(modifier = Modifier.width(Dimensions.elementSpacing))

            // Fixed undo/redo/dismiss section
            ToolbarIcon(
                Icons.Outlined.Undo,
                "Undo",
                enabled = undoRedoManager.canUndo,
            ) {
                undoRedoManager.undo(textFieldValue)?.let { onTextFieldValueChange(it) }
            }
            ToolbarIcon(
                Icons.Outlined.Redo,
                "Redo",
                enabled = undoRedoManager.canRedo,
            ) {
                undoRedoManager.redo(textFieldValue)?.let { onTextFieldValueChange(it) }
            }
            ToolbarIcon(Icons.Outlined.KeyboardArrowDown, "Dismiss", onClick = onDismiss)
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(Dimensions.actionButtonSecondary),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
            modifier = Modifier.size(Dimensions.iconSizeSmall),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkdownDescriptionField(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cursorPos = textFieldValue.selection.start
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    // Cache region parsing on text content -- avoids re-parsing on cursor-only changes
    val cachedRegions =
        remember(textFieldValue.text, baseColor) {
            findInlineRegions(textFieldValue.text, baseColor)
        }

    // Single instance: update cursor via mutable property instead of recreating
    val markdownTransformation =
        remember(baseColor, cachedRegions) {
            MarkdownVisualTransformation(baseColor, cachedRegions)
        }
    markdownTransformation.cursorPosition = cursorPos

    // Keep editor visible when focus enters description field.
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(textFieldValue.text, textFieldValue.selection) {
        if (imeBottom <= 0) return@LaunchedEffect
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (layout.layoutInput.text.isEmpty()) return@LaunchedEffect

        val transformedOffset =
            markdownTransformation.lastOffsetMapping
                .originalToTransformed(cursorPos)
                .coerceIn(0, layout.layoutInput.text.length)
        val cursorRect = layout.getCursorRect(transformedOffset)
        val paddedRect = Rect(cursorRect.left, cursorRect.top, cursorRect.right, cursorRect.bottom + 120f)
        bringIntoViewRequester.bringIntoView(paddedRect)
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newTfv ->
            val oldText = textFieldValue.text
            val result = handleMarkdownAutoFormat(oldText, newTfv.text, newTfv.selection)
            onTextFieldValueChange(result)
        },
        onTextLayout = { layout ->
            textLayoutResult = layout
        },
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color = baseColor,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier =
            modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    onFocusChange(focusState.isFocused)
                    if (focusState.isFocused) {
                        scope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
        visualTransformation = markdownTransformation,
        decorationBox = { innerTextField ->
            Column {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "Add description... (supports markdown)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            }
        },
    )
}

private fun handleMarkdownAutoFormat(
    oldText: String,
    newText: String,
    currentSelection: TextRange,
): TextFieldValue {
    // Only trigger on single-character insertion
    if (newText.length != oldText.length + 1) {
        return TextFieldValue(text = newText, selection = currentSelection)
    }

    // Use cursor position to find the newline — more reliable than char-by-char diff
    // which can fail with IME predictive text.
    val cursor = currentSelection.start
    val newlineIdx = (cursor - 1).coerceAtLeast(0)

    if (newlineIdx >= newText.length || newText[newlineIdx] != '\n') {
        return TextFieldValue(text = newText, selection = currentSelection)
    }

    // Get the line BEFORE the newline
    val beforeNewline = newText.substring(0, newlineIdx)
    val prevLineStart = beforeNewline.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
    val prevLine = beforeNewline.substring(prevLineStart)
    val trimmedPrev = prevLine.trimStart()
    val indent = prevLine.substring(0, prevLine.length - trimmedPrev.length)
    val after = newText.substring(newlineIdx + 1)

    // Helper to remove empty marker and the line
    fun removeMarkerLine(): TextFieldValue {
        val beforePrevLine = newText.substring(0, prevLineStart)
        val result = beforePrevLine + after
        return TextFieldValue(text = result, selection = TextRange(beforePrevLine.length))
    }

    // Helper to continue list on next line
    fun continueLine(marker: String): TextFieldValue {
        val result = beforeNewline + "\n" + indent + marker + after
        val newCursor = newlineIdx + 1 + indent.length + marker.length
        return TextFieldValue(text = result, selection = TextRange(newCursor))
    }

    // Unordered list continuation: "- text" -> "- " on next line
    if (trimmedPrev.startsWith("- ") && trimmedPrev.length > 2) {
        // Checkbox within list: "- [ ] text" or "- [x] text"
        return if (trimmedPrev.startsWith("- [ ] ") && trimmedPrev.length > 6) {
            continueLine("- [ ] ")
        } else if (
            (trimmedPrev.startsWith("- [x] ") || trimmedPrev.startsWith("- [X] ")) &&
            trimmedPrev.length > 6
        ) {
            continueLine("- [ ] ")
        } else {
            continueLine("- ")
        }
    }

    // Empty unordered list marker -> remove
    if (trimmedPrev == "-" || trimmedPrev == "- " ||
        trimmedPrev == "- [ ]" || trimmedPrev == "- [ ] " ||
        trimmedPrev == "- [x]" || trimmedPrev == "- [x] " ||
        trimmedPrev == "- [X]" || trimmedPrev == "- [X] "
    ) {
        return removeMarkerLine()
    }

    // Numbered list: "1. text" -> "2. " on next line
    val numberedMatch = Regex("^(\\d+)\\.\\s(.+)$").find(trimmedPrev)
    if (numberedMatch != null) {
        val nextNum = (numberedMatch.groupValues[1].toIntOrNull() ?: 0) + 1
        return continueLine("$nextNum. ")
    }

    // Empty numbered list marker -> remove
    if (Regex("^\\d+\\.\\s?$").matches(trimmedPrev)) {
        return removeMarkerLine()
    }

    // Checkbox: "[ ] text" -> "[ ] " on next line
    if (trimmedPrev.startsWith("[ ] ") && trimmedPrev.length > 4) {
        return continueLine("[ ] ")
    }

    // Checked checkbox: "[x] text" -> "[ ] " on next line
    if ((trimmedPrev.startsWith("[x] ") || trimmedPrev.startsWith("[X] ")) &&
        trimmedPrev.length > 4
    ) {
        return continueLine("[ ] ")
    }

    // Empty checkbox -> remove
    if (trimmedPrev == "[ ]" || trimmedPrev == "[ ] " ||
        trimmedPrev == "[x]" || trimmedPrev == "[x] "
    ) {
        return removeMarkerLine()
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
                text = date?.let { d ->
                    "${d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}, ${d.year}"
                } ?: "Date",
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
                initialSelectedDateMillis = date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
            )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val selectedDate =
                            Instant.fromEpochMilliseconds(selectedMillis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
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
                text = time?.let { t ->
                    val h = if (t.hour == 0 || t.hour == 12) 12 else t.hour % 12
                    val amPm = if (t.hour < 12) "AM" else "PM"
                    "${h}:${t.minute.toString().padStart(2, '0')} $amPm"
                } ?: "Time",
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
                                onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute))
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
    onFocusChange: (Boolean) -> Unit,
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
                    .onFocusChanged { focusState ->
                        onFocusChange(focusState.isFocused)
                    }
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
