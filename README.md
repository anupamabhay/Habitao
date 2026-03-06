# Habitao

A unified productivity app for Android combining habit tracking, a Pomodoro focus timer, routines, and task management.

## Status

**Current:** Active development (Habits, Pomodoro, Routines, Tasks, and Settings complete)
**Platform:** Android 8.0+ (API 26)
**Architecture:** Clean Architecture + MVI with Jetpack Compose

## Features

### Habits
- Three habit types: simple (yes/no), measurable (numeric targets), and checklist (sub-tasks)
- Flexible scheduling: daily, specific days, X times per week, every X days
- Streak tracking and weekly progress aggregation
- Infinite-scrolling week calendar with snap-to-week navigation
- Stats dashboard with completion rates and streak data
- Per-habit reminders with sound and vibration

### Pomodoro Timer
- Foreground service timer with pause, resume, stop, and skip controls
- Configurable work/break durations and session counts
- Auto-start next session or break
- System ringtone picker for completion sounds
- Vibration feedback with configurable duration
- Daily focus time tracking (sessions, rounds, total duration)
- Completion notifications with alarm-priority sound
- Full-screen focus mode with animations

### Routines
- Create and manage daily routines with ordered steps
- Flexible scheduling: daily, specific days, every X days
- Step duration tracking displayed on routine cards
- Completion history and progress indicators
- Per-routine reminders

### Tasks
- Create and manage tasks with priorities (Low, Medium, High) and due dates
- Nested subtasks up to 3 levels deep
- Inline markdown editing with formatting toolbar
- Auto-sorted sections: Overdue, Today, Tomorrow, Upcoming
- Task reminders with alarm scheduling

### Settings
- Theme selection (System, Light, Dark)
- Backup and restore (JSON export/import of all data)
- Configurable tab bar with reordering
- Default launch tab selection
- Pomodoro timer configuration

### Planned
- Home screen widgets

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | Clean Architecture + MVI |
| Database | Room |
| DI | Hilt |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| CI/CD | GitHub Actions |

## Project Structure

```
habitao/
├── app/                  # Application entry point, DI, navigation
├── domain/               # Business logic (pure Kotlin, no Android deps)
├── data/                 # Room database, repository implementations
├── core/
│   ├── common/           # Shared utilities
│   ├── datastore/        # Preferences DataStore
│   ├── ui/               # Design system, theme, shared components
│   └── testing/          # Test utilities and fakes
├── feature/
│   ├── habits/           # Habit tracking UI and ViewModels
│   ├── pomodoro/         # Focus timer UI, service, preferences
│   ├── routines/         # Routine management UI and ViewModels
│   ├── tasks/            # Task management UI and ViewModels
│   └── settings/         # App settings UI and ViewModels
└── system/
    ├── notifications/    # Notification channels, reminders, receivers
    ├── alarms/           # Exact alarm scheduling
    ├── work/             # Background work (WorkManager)
    └── widget/           # Home screen widgets (planned)
```

## Building

### Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

### Commands
```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Release build (requires signing config)
./gradlew testDebugUnitTest    # Unit tests
./gradlew detekt               # Static analysis
./gradlew ktlintFormat         # Code formatting
```

## Git Workflow

- `main` - Production releases only
- `dev` - Primary development branch
- `feature/*` - Feature branches (merge to `dev` via PR)

See [CONTRIBUTING.md](./CONTRIBUTING.md) for full guidelines.

## Documentation

- [Project Overview](./docs/00-PROJECT-OVERVIEW.md)
- [Product Requirements](./docs/01-PRODUCT-REQUIREMENTS.md)
- [Data Model](./docs/02-DATA-MODEL-SCHEMA.md)
- [Technical Architecture](./docs/03-TECHNICAL-ARCHITECTURE.md)
- [Security and Privacy](./docs/04-SECURITY-PRIVACY.md)
- [Testing Strategy](./docs/05-TESTING-STRATEGY.md)
- [CI/CD Setup](./docs/06-CICD-SETUP.md)

## License

TBD
