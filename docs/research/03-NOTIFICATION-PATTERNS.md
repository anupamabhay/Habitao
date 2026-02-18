# Android Notification Scheduling Patterns
## Research Date: February 18, 2026

---

## 1. Architecture Overview

```
User sets reminder time (CreateHabitScreen)
    |
    v
CreateHabitViewModel.saveHabit()
    |
    v
HabitReminderScheduler.scheduleReminder(habit)
    |
    v
AlarmManager.setExactAndAllowWhileIdle()
    |
    v (at scheduled time)
HabitReminderReceiver.onReceive()
    |
    v
NotificationHelper.showHabitReminder()
```

---

## 2. Required Permissions

```xml
<!-- AndroidManifest.xml -->

<!-- Exact alarms (Android 12+, API 31+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Notifications (Android 13+, API 33+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Reschedule alarms after reboot -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Vibration for notifications -->
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## 3. Key Components

### 3.1 NotificationHelper
- Create NotificationChannel on app startup (required API 26+)
- Channel ID: "habit_reminders"
- Channel Name: "Habit Reminders"
- Importance: NotificationManager.IMPORTANCE_HIGH (heads-up notification)
- Build notification with: title, text, action button ("Mark Complete")

### 3.2 HabitReminderScheduler
- Schedule exact alarm at the habit's reminder time
- Use PendingIntent with habit ID as request code (habit.id.hashCode())
- FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
- Cancel alarm when: habit deleted, reminder disabled, habit archived
- Reschedule when: reminder time changed, habit updated

### 3.3 HabitReminderReceiver (BroadcastReceiver)
- Receives alarm broadcast
- Extracts habit ID and title from intent extras
- Calls NotificationHelper to show notification
- Optionally reschedules for next occurrence (daily habits)

### 3.4 BootReceiver (BroadcastReceiver)
- Listens for BOOT_COMPLETED
- Queries all habits with reminders enabled
- Reschedules all alarms via HabitReminderScheduler

---

## 4. Android Version Considerations

### Android 12 (API 31): SCHEDULE_EXACT_ALARM
- Must check `alarmManager.canScheduleExactAlarms()` before scheduling
- If denied, fall back to `setAndAllowWhileIdle()` (inexact)
- Can direct user to Settings to grant permission

### Android 13 (API 33): POST_NOTIFICATIONS
- Must request runtime permission before showing notifications
- Request on first reminder enable, not on app launch
- Show rationale dialog explaining why notifications are needed

### Doze Mode
- Use `setExactAndAllowWhileIdle()` instead of `setExact()`
- This ensures alarm fires even in Doze mode
- Note: Android may still defer by a few minutes in deep Doze

---

## 5. Notification Action Button

```kotlin
// "Mark Complete" action on notification
val completeIntent = Intent(context, HabitCompleteReceiver::class.java).apply {
    putExtra("HABIT_ID", habitId)
    action = "ACTION_COMPLETE_HABIT"
}
val completePendingIntent = PendingIntent.getBroadcast(
    context,
    habitId.hashCode() + 1,
    completeIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("Habit Reminder")
    .setContentText("Time to: $habitTitle")
    .addAction(R.drawable.ic_check, "Mark Complete", completePendingIntent)
    .setAutoCancel(true)
    .build()
```

---

## 6. Testing Notifications

### Emulator
- Set system time to just before reminder time
- Or use `adb shell am broadcast` to simulate alarm

### Physical Device
- Battery optimization may block alarms
- Test with "Don't optimize" setting for the app
- Test after device reboot to verify BootReceiver
