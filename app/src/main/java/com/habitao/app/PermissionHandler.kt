package com.habitao.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun RequestAppPermissions() {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var showAlarmRationale by remember { mutableStateOf(false) }
    var permissionsRequested by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val allGranted = results.values.all { it }
            if (!allGranted) {
                showRationale = true
            }
            // Also check exact alarm permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    showAlarmRationale = true
                }
            }
        }

    // Request notification permission on first composition
    LaunchedEffect(Unit) {
        if (!permissionsRequested) {
            permissionsRequested = true
            val permissionsToRequest = mutableListOf<String>()

            // POST_NOTIFICATIONS (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                // Permissions already granted, check exact alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        showAlarmRationale = true
                    }
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permissions Required") },
            text = {
                Text(
                    "Habitao needs notification permission to send you reminders " +
                        "for habits, tasks, and Pomodoro sessions. " +
                        "You can grant this in Settings.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                    },
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Later")
                }
            },
        )
    }

    if (showAlarmRationale) {
        AlertDialog(
            onDismissRequest = { showAlarmRationale = false },
            title = { Text("Exact Alarms Permission") },
            text = {
                Text(
                    "Habitao needs exact alarm permission to deliver " +
                        "precise reminders for your habits and tasks.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlarmRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent =
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Some devices don't support this intent
                            }
                        }
                    },
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmRationale = false }) {
                    Text("Later")
                }
            },
        )
    }
}
