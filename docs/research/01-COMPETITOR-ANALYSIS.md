# Competitor Analysis - Habit Tracker Apps
## Research Date: February 18, 2026

---

## 1. TickTick

### Home Screen
- Shows "Today" with date, greeting based on time of day
- Daily progress bar at top: "4 of 7 completed"
- Habits listed as cards with inline progress
- Completed items move to bottom of list with strikethrough and muted color
- Swipe actions: swipe left to delete, NO swipe-right-to-complete (tap only)

### Habit Card Design
- Clean card with habit name, frequency text ("Every day"), and circular progress ring
- Color-coded by category (user-assigned colors)
- Tap the circle/checkbox to mark complete
- Counter habits show "+/-" buttons inline
- Completed cards: muted background, checkmark overlay

### Undo Pattern
- Snackbar at bottom: "Habit completed. Undo" with 5-second timeout
- Swipe-to-delete shows snackbar: "Deleted. Undo"
- No swipe-to-complete gesture

### Notifications
- Per-habit reminder with time picker
- "Smart reminder" option that adapts to user behavior
- Notification shows habit name + action button to mark complete from notification

---

## 2. Todoist

### Home Screen
- "Today" view with date and task count
- Sections: "Overdue", "Today", grouped by project
- Circular karma/productivity score in header
- Pull-to-refresh gesture

### Task/Habit Card
- Minimal: checkbox + title + optional labels/tags
- Priority indicated by checkbox color (red=P1, orange=P2, blue=P3, gray=P4)
- Subtasks shown as "0/3" with expandable arrow
- Swipe right = complete, swipe left = reschedule (with undo snackbar)

### Undo Pattern
- Bottom snackbar: "1 task completed. Undo" (always shown on complete)
- Delete: confirmation dialog, then snackbar undo
- Critical: undo snackbar persists for ~5 seconds

---

## 3. Loop Habit Tracker (Open Source)

### Home Screen
- Calendar heatmap header (shows last 30 days as colored dots)
- Each habit row shows: name + mini frequency chart + today's status
- Long press to edit, tap to toggle complete
- No swipe gestures

### Habit Card Design  
- Minimal rows (not cards) - habit name left, toggle right
- Color per habit (user-chosen)
- Streak counter shown as small badge
- Progress shown as fill percentage in mini bar

### Statistics
- Per-habit: streak count, best streak, completion rate %, frequency histogram
- Calendar view: color-coded heatmap (green=done, gray=skipped, red=missed)

---

## 4. Streaks (iOS, for reference)

### Home Screen
- Grid layout with circular progress rings
- Each habit = large circle with icon + progress ring
- Completed habits: ring fills + checkmark animation
- Daily streak number shown below each habit

### Completion Animation
- Ring fills with smooth animation (0.3s)
- Haptic feedback on completion
- Confetti/sparkle on streak milestones (7-day, 30-day, etc.)

---

## 5. Key UX Patterns to Adopt

### Completion
- Primary: tap action button to toggle (already implemented)
- Swipe-to-complete is OPTIONAL and risky (TickTick doesn't use it)
- Always show undo snackbar on completion/deletion

### Home Screen Header
- Time-of-day greeting: "Good morning", "Good afternoon", "Good evening"
- Daily progress summary: "3 of 7 habits done" with progress indicator
- Date in secondary style below greeting

### Card Design
- Completed cards: muted colors + subtle visual change (lower elevation, opacity)
- Strikethrough on title when completed (Todoist, TickTick pattern)
- Category/type color coding

### Swipe Actions
- Swipe LEFT = delete (destructive, with undo snackbar)
- Swipe RIGHT = optional (complete for non-simple, disabled for now)
- Always show Snackbar with "Undo" action (5-second timeout)

### Notifications
- Per-habit reminder at user-set time
- Notification shows habit name + "Mark Complete" action button
- Reschedule alarms on boot (RECEIVE_BOOT_COMPLETED)
