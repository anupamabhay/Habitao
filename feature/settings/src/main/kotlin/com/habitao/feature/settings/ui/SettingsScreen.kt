package com.habitao.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private const val BOTTOM_TAB_COUNT = 4

data class SettingsTabOption(
    val id: String,
    val label: String,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    selectedBottomTabIds: List<String>,
    allTabs: List<SettingsTabOption>,
    defaultLaunchTabId: String,
    onBottomTabsChanged: (List<String>) -> Unit,
    onDefaultLaunchTabChanged: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderedTabs = remember(selectedBottomTabIds, allTabs) {
        val idToTab = allTabs.associateBy { it.id }
        val ordered = selectedBottomTabIds.mapNotNull { idToTab[it] }
        val remaining = allTabs.filter { it.id !in selectedBottomTabIds }
        (ordered + remaining).distinctBy { it.id }
    }

    val enabledTabs = orderedTabs.take(BOTTOM_TAB_COUNT)
    val disabledTabs = orderedTabs.drop(BOTTOM_TAB_COUNT)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tab Bar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column {
                        enabledTabs.forEachIndexed { index, tab ->
                            TabItemRow(
                                tab = tab,
                                isEnabled = true,
                                canMoveUp = index > 0,
                                canMoveDown = index < enabledTabs.size - 1,
                                onToggle = {
                                    val newList = orderedTabs.toMutableList()
                                    newList.remove(tab)
                                    newList.add(tab)
                                    onBottomTabsChanged(newList.map { it.id })
                                },
                                onMoveUp = {
                                    val newList = orderedTabs.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    onBottomTabsChanged(newList.map { it.id })
                                },
                                onMoveDown = {
                                    val newList = orderedTabs.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    onBottomTabsChanged(newList.map { it.id })
                                }
                            )
                            if (index < enabledTabs.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            if (disabledTabs.isNotEmpty()) {
                item {
                    Text(
                        text = "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column {
                            disabledTabs.forEachIndexed { index, tab ->
                                TabItemRow(
                                    tab = tab,
                                    isEnabled = false,
                                    canMoveUp = false,
                                    canMoveDown = false,
                                    onToggle = {
                                        val newList = orderedTabs.toMutableList()
                                        newList.remove(tab)
                                        val insertIndex = minOf(BOTTOM_TAB_COUNT - 1, newList.size)
                                        newList.add(insertIndex, tab)
                                        onBottomTabsChanged(newList.map { it.id })
                                    },
                                    onMoveUp = {},
                                    onMoveDown = {}
                                )
                                if (index < disabledTabs.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 56.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max number of tabs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$BOTTOM_TAB_COUNT >",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Over-limited tabs will be shown in More.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TabItemRow(
    tab: SettingsTabOption,
    isEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.RemoveCircle else Icons.Default.AddCircle,
                contentDescription = if (isEnabled) "Disable" else "Enable",
                tint = if (isEnabled) Color(0xFFE57373) else Color(0xFF81C784)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForTab(tab.label),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = getSubtitleForTab(tab.label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (isEnabled) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = canMoveUp, onClick = onMoveUp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = canMoveDown, onClick = onMoveDown)
                )
            }
        }
    }
}

private fun getIconForTab(label: String): ImageVector {
    return when(label.lowercase()) {
        "tasks" -> Icons.Default.CheckCircle
        "habits" -> Icons.Default.Loop
        "routines" -> Icons.Default.GridView
        "pomodoro" -> Icons.Default.Timer
        "stats" -> Icons.Default.CalendarToday
        else -> Icons.Default.Folder
    }
}

private fun getSubtitleForTab(label: String): String {
    return when(label.lowercase()) {
        "tasks" -> "Manage your task with lists and filters."
        "habits" -> "Develop a habit and keep track of it."
        "routines" -> "Build daily routines step by step."
        "pomodoro" -> "Use the Pomo timer or stopwatch to keep focus."
        "stats" -> "View your progress and statistics."
        else -> "Tab description."
    }
}

