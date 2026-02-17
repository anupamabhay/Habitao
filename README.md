# Habitao

Unified Productivity App for Android - Habits, Routines, Tasks, and Pomodoro Timer in one app.

## Status

**Current:** MVP Development Phase - Core habits functionality implemented  
**Platform:** Android 8.0+ (API 26+)  
**Architecture:** Clean Architecture + MVI with Jetpack Compose

## Quick Start

```bash
# Clone and open in Android Studio
git clone <repo-url>
cd Habitao

# Build debug APK
./gradlew assembleDebug

# Run on connected device
./gradlew installDebug
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | Clean Architecture + MVI |
| Database | Room 2.7 |
| DI | Hilt 2.51 |
| Navigation | Voyager 1.1 |
| Async | Kotlin Coroutines + Flow |

## Project Structure

```
habitao/
├── app/                  # Application entry point
├── feature/              # Feature modules
│   ├── habits/           # Habit tracking
│   ├── routines/         # Routine management
│   ├── tasks/            # Task management
│   └── pomodoro/         # Focus timer
├── domain/               # Business logic (pure Kotlin)
├── data/                 # Data layer (Room, repositories)
├── core/                 # Shared utilities
│   ├── common/           # Common utilities
│   ├── ui/               # Design system, theme
│   └── testing/          # Test utilities
└── system/               # Android system integrations
    ├── notifications/
    ├── alarms/
    ├── work/
    └── widget/
```

## Architecture

```
Presentation (Compose + ViewModel)
        |
        v
Domain (Use Cases + Repository Interfaces)
        |
        v
Data (Room + Repository Implementations)
```

- **MVI Pattern**: Unidirectional data flow with State, Intent, and ViewModel
- **Clean Architecture**: Separation of concerns across layers
- **Repository Pattern**: Abstract data sources from business logic

## Features

### Implemented
- Habit list view with Material Design 3 theming
- Create habit form with support for three habit types:
  - Simple (Yes/No completion)
  - Measurable (numeric targets with progress tracking)
  - Checklist (sub-tasks that can be checked off)
- Edit existing habits
- Habit completion tracking with daily logs
- Flexible scheduling (daily, specific days, interval-based)
- Dynamic color support (Material You)
- Empty state handling
- Database schema for habits and logs

### Planned
- Routines with ordered steps
- Tasks with subtasks and priorities
- Pomodoro timer with notifications
- Home screen widgets
- Statistics and streaks
- Habit reminders and notifications

## Development

### Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

### Building
```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build (requires signing)
./gradlew test             # Run unit tests
```

### Git Workflow
- `main` - Production releases only
- `dev` - Primary development branch
- `feature/*` - Feature branches from dev

## Documentation

Detailed documentation in `docs/`:

- [Project Overview](./docs/00-PROJECT-OVERVIEW.md)
- [Product Requirements](./docs/01-PRODUCT-REQUIREMENTS.md)
- [Data Model](./docs/02-DATA-MODEL-SCHEMA.md)
- [Technical Architecture](./docs/03-TECHNICAL-ARCHITECTURE.md)
- [Security and Privacy](./docs/04-SECURITY-PRIVACY.md)
- [Testing Strategy](./docs/05-TESTING-STRATEGY.md)

## License

TBD
