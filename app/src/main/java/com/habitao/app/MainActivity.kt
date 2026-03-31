package com.habitao.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.habitao.data.backup.BackupManager
import com.habitao.system.notifications.HabitReminderScheduler
import com.habitao.system.notifications.TaskReminderScheduler
import org.koin.android.ext.android.inject
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val backupManager: BackupManager by inject()
    private val habitReminderScheduler: HabitReminderScheduler by inject()
    private val taskReminderScheduler: TaskReminderScheduler by inject()
    companion object {
        private var quickActionRouteState by mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        quickActionRouteState = QuickActionIntentParser.toRoute(intent?.action)

        // Request highest available refresh rate
        @Suppress("DEPRECATION")
        val modes =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display?.supportedModes?.toList()
            } else {
                windowManager.defaultDisplay.supportedModes?.toList()
            }
        modes?.maxByOrNull { it.refreshRate }?.let { mode ->
            window.attributes =
                window.attributes.also { params ->
                    params.preferredDisplayModeId = mode.modeId
                }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()

            val exportLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE),
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch {
                            backupManager.exportToUri(uri).fold(
                                onSuccess = { count ->
                                    Toast.makeText(this@MainActivity, "Exported $count records successfully", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(this@MainActivity, "Export failed: ${error.message}", Toast.LENGTH_LONG).show()
                                },
                            )
                        }
                    }
                }

            val importLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch {
                            backupManager.importFromUri(uri).fold(
                                onSuccess = { count ->
                                    habitReminderScheduler.rescheduleAllReminders()
                                    taskReminderScheduler.rescheduleAllReminders()
                                    Toast.makeText(this@MainActivity, "Restored $count records successfully", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(this@MainActivity, "Import failed: ${error.message}", Toast.LENGTH_LONG).show()
                                },
                            )
                        }
                    }
                }

            // Request Android-specific permissions (notifications, exact alarms)
            RequestAppPermissions()

            App(
                onExportBackup = { exportLauncher.launch(BackupManager.DEFAULT_FILENAME) },
                onImportBackup = { importLauncher.launch(arrayOf(BackupManager.MIME_TYPE)) },
                quickActionRoute = quickActionRouteState,
                onQuickActionConsumed = { quickActionRouteState = null },
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        quickActionRouteState = QuickActionIntentParser.toRoute(intent.action)
    }
}
