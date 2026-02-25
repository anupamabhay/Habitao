package com.habitao.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    val tabMap = remember(allTabs) { allTabs.associateBy(SettingsTabOption::id) }
    val selectedTabOptions = remember(selectedBottomTabIds, tabMap) {
        selectedBottomTabIds.mapNotNull(tabMap::get)
    }
    val hiddenTabOption = remember(selectedBottomTabIds, allTabs) {
        val selectedSet = selectedBottomTabIds.toSet()
        allTabs.firstOrNull { tab -> tab.id !in selectedSet }
    }

    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var dailyRemindersEnabled by rememberSaveable { mutableStateOf(true) }
    var hiddenTabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeaderCard()
            }

            item {
                SettingsSection(title = "App Preferences") {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        title = "Theme",
                        subtitle = "System Default",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                            )
                        },
                        onClick = {},
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { checked -> notificationsEnabled = checked },
                            )
                        },
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Default.Schedule,
                        title = "Daily Reminders",
                        subtitle = if (dailyRemindersEnabled) "09:00 AM" else "Disabled",
                        trailingContent = {
                            Switch(
                                checked = dailyRemindersEnabled,
                                onCheckedChange = { checked -> dailyRemindersEnabled = checked },
                            )
                        },
                    )
                }
            }

            item {
                SettingsSection(title = "Navigation") {
                    Text(
                        text = "Visible bottom tabs",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    selectedTabOptions.forEachIndexed { index, tab ->
                        TabReorderRow(
                            tab = tab,
                            canMoveUp = index > 0,
                            canMoveDown = index < selectedTabOptions.lastIndex,
                            onMoveUp = {
                                onBottomTabsChanged(
                                    moveTab(
                                        tabIds = selectedTabOptions.map(SettingsTabOption::id),
                                        fromIndex = index,
                                        toIndex = index - 1,
                                    ),
                                )
                            },
                            onMoveDown = {
                                onBottomTabsChanged(
                                    moveTab(
                                        tabIds = selectedTabOptions.map(SettingsTabOption::id),
                                        fromIndex = index,
                                        toIndex = index + 1,
                                    ),
                                )
                            },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                    SettingsRow(
                        icon = Icons.Default.SwapHoriz,
                        title = "Hidden tab",
                        subtitle = hiddenTabOption?.label ?: "None",
                        trailingContent = {
                            Box {
                                IconButton(onClick = { hiddenTabMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Choose hidden tab",
                                    )
                                }

                                DropdownMenu(
                                    expanded = hiddenTabMenuExpanded,
                                    onDismissRequest = { hiddenTabMenuExpanded = false },
                                ) {
                                    allTabs.forEach { tab ->
                                        DropdownMenuItem(
                                            text = { Text(tab.label) },
                                            onClick = {
                                                hiddenTabMenuExpanded = false
                                                onBottomTabsChanged(
                                                    visibleTabsForHiddenSelection(
                                                        hiddenTabId = tab.id,
                                                        currentVisibleTabIds =
                                                            selectedTabOptions.map(SettingsTabOption::id),
                                                        allTabIds = allTabs.map(SettingsTabOption::id),
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )

                    HorizontalDivider()

                    Text(
                        text = "Default launch tab",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    allTabs.forEach { tab ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onDefaultLaunchTabChanged(tab.id) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = defaultLaunchTabId == tab.id,
                                onClick = { onDefaultLaunchTabChanged(tab.id) },
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Data & Security") {
                    SettingsRow(
                        icon = Icons.Default.Cloud,
                        title = "Cloud Sync",
                        subtitle = "Last synced 2m ago",
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Default.Download,
                        title = "Export Data",
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.2",
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Default.Settings,
                        title = "Privacy Policy",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }

            item {
                Text(
                    text = "Habitao © 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AD",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
            ) {
                Text(
                    text = "Alex Doe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "alex@example.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit profile",
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = trailingContent,
        modifier = rowModifier,
    )
}

@Composable
private fun TabReorderRow(
    tab: SettingsTabOption,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tab.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Move ${tab.label} up",
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Move ${tab.label} down",
            )
        }
    }
}

private fun moveTab(
    tabIds: List<String>,
    fromIndex: Int,
    toIndex: Int,
): List<String> {
    if (fromIndex !in tabIds.indices || toIndex !in tabIds.indices) {
        return tabIds
    }

    val mutableTabIds = tabIds.toMutableList()
    val movedTab = mutableTabIds.removeAt(fromIndex)
    mutableTabIds.add(toIndex, movedTab)
    return mutableTabIds
}

private fun visibleTabsForHiddenSelection(
    hiddenTabId: String,
    currentVisibleTabIds: List<String>,
    allTabIds: List<String>,
): List<String> {
    val targetVisibleTabs = allTabIds.filterNot { it == hiddenTabId }
    val orderedVisibleTabs = currentVisibleTabIds.filter { it in targetVisibleTabs }
    val missingVisibleTabs = targetVisibleTabs.filterNot { it in orderedVisibleTabs }
    return (orderedVisibleTabs + missingVisibleTabs).take(BOTTOM_TAB_COUNT)
}
