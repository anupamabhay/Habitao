# Contributing to Habitao

Thank you for your interest in contributing to Habitao!

---

## 🔀 Branching Strategy

### Branch Structure

```
main (protected, production-ready releases only)
  │
  └── dev (primary development branch)
        ├── feature/habit-crud
        ├── feature/task-management
        ├── feature/pomodoro-timer
        ├── bugfix/notification-crash
        └── refactor/repository-pattern
```

### Branch Types

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New features | `feature/habit-streak-calculation` |
| `bugfix/` | Bug fixes | `bugfix/notification-not-firing` |
| `refactor/` | Code refactoring | `refactor/clean-architecture` |
| `docs/` | Documentation updates | `docs/architecture-update` |
| `test/` | Test additions/fixes | `test/habit-repository-integration` |
| `hotfix/` | Emergency production fixes | `hotfix/critical-data-loss` |

### Workflow

1. **Create Feature Branch from `dev`:**
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feature/your-feature-name
   ```

2. **Work on Your Feature:**
   ```bash
   # Make changes
   git add .
   git commit -m "feat: add habit streak calculation"
   
   # Keep up to date with dev
   git fetch origin dev
   git rebase origin/dev
   ```

3. **Push to Remote:**
   ```bash
   git push -u origin feature/your-feature-name
   ```

4. **Create Pull Request:**
   - Target: `dev` (NOT `main`)
   - Title: Clear, descriptive (e.g., "Add habit streak calculation")
   - Description: What changed, why, testing done
   - Request review

5. **After PR Approval:**
   - Squash and merge to `dev`
   - Delete feature branch

### Rules

- ✅ **Always** branch from `dev`
- ✅ **Always** merge back to `dev` via Pull Request
- ✅ **Never** commit directly to `dev` or `main`
- ✅ **Never** merge without code review
- ✅ Merge to `main` only for releases (with approval)
- ❌ **No force pushes** to `dev` or `main`

---

## 📝 Commit Messages

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding or updating tests
- `docs`: Documentation changes
- `style`: Formatting, missing semicolons, etc. (no code change)
- `perf`: Performance improvement
- `chore`: Build process, dependencies, tooling

### Examples

```
feat(habits): add streak calculation use case

Implement CalculateStreakUseCase to compute current and longest
streaks from HabitLog records. Handles edge cases like missing
days and incomplete logs.

Closes #42
```

```
fix(notifications): handle Doze mode for exact alarms

Request SCHEDULE_EXACT_ALARM permission on Android 12+ and fall
back to inexact alarms if permission denied. Show user education
dialog explaining why exact timing matters.

Fixes #89
```

---

## 🧪 Testing Requirements

### Before Creating PR

- [ ] All unit tests pass: `./gradlew testDebugUnitTest`
- [ ] All integration tests pass: `./gradlew connectedDebugAndroidTest`
- [ ] Code follows Detekt rules: `./gradlew detekt`
- [ ] Code formatted with Ktlint: `./gradlew ktlintFormat`
- [ ] New code has tests (minimum 80% coverage for new logic)
- [ ] Manual testing completed on emulator/device

### Coverage Targets

- Domain layer (Use Cases): **90%+**
- Data layer (Repositories, DAOs): **80%+**
- Presentation layer (ViewModels): **70%+**

---

## 🏗️ Code Style

### Kotlin Style Guide

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these project-specific rules:

**Naming:**
```kotlin
// Classes: PascalCase
class HabitRepository

// Functions: camelCase
fun calculateStreak()

// Constants: SCREAMING_SNAKE_CASE
const val MAX_HABIT_COUNT = 100

// Private properties: camelCase with underscore prefix
private val _state = MutableStateFlow()
val state: StateFlow = _state.asStateFlow()
```

**Architecture:**
- Domain layer: Pure Kotlin (no Android imports)
- Data layer: Repository pattern
- Presentation: MVI (Model-View-Intent)

**Compose:**
```kotlin
// Composables: PascalCase
@Composable
fun HabitCard(habit: Habit) {
    // ...
}

// Preview functions
@Preview
@Composable
private fun HabitCardPreview() {
    // ...
}
```

### Automatic Formatting

Run before committing:
```bash
./gradlew ktlintFormat
```

---

## 📚 Documentation

### Code Documentation

**Public APIs:**
```kotlin
/**
 * Calculates the current streak for a habit based on consecutive completed days.
 *
 * @param habitId The unique identifier of the habit
 * @return Result containing StreakInfo or error
 */
suspend fun calculateStreak(habitId: String): Result<StreakInfo>
```

**Complex Logic:**
```kotlin
// Calculate streak by walking backwards from today until we find
// a gap (missing day or incomplete log)
var currentDate = LocalDate.now()
while (/* condition */) {
    // ...
}
```

### Architecture Decision Records (ADRs)

For significant architectural decisions, create ADR in `docs/adr/`:

```markdown
# ADR-001: Use MVI Instead of MVVM

## Status
Accepted

## Context
Need predictable state management for Jetpack Compose UI.

## Decision
Use MVI (Model-View-Intent) pattern with StateFlow.

## Consequences
- Single immutable state prevents state desync bugs
- Easier debugging (all state changes via explicit intents)
- Better testability
```

---

## 🔍 Code Review Guidelines

### As a Reviewer

- Check for:
  - [ ] Correct architecture layer (no Android in domain)
  - [ ] Proper error handling (no naked try-catch)
  - [ ] Tests included for new logic
  - [ ] No hardcoded strings (use resources)
  - [ ] No sensitive data in logs
  - [ ] Performance considerations (lazy loading, pagination)

- Be constructive and kind
- Ask questions, don't command
- Approve PRs promptly (within 24 hours if possible)

### As an Author

- Keep PRs small (< 400 lines changed)
- Self-review before requesting review
- Respond to feedback promptly
- Don't take feedback personally
- Update PR based on feedback, don't argue

---

## 🚀 Release Process

### Version Numbering

Semantic Versioning: `MAJOR.MINOR.PATCH`
- **MAJOR:** Breaking changes
- **MINOR:** New features (backward compatible)
- **PATCH:** Bug fixes

### Release Workflow

1. **Create Release Branch:**
   ```bash
   git checkout dev
   git checkout -b release/1.0.0
   ```

2. **Update Version:**
   - `app/build.gradle.kts`: Update `versionCode` and `versionName`
   - Update `CHANGELOG.md`

3. **QA Testing:**
   - Run full test suite
   - Manual testing on multiple devices
   - Beta testing (internal track)

4. **Merge to Main:**
   ```bash
   git checkout main
   git merge release/1.0.0
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin main --tags
   ```

5. **Merge Back to Dev:**
   ```bash
   git checkout dev
   git merge main
   git push origin dev
   ```

6. **Deploy:**
   - CI/CD pipeline builds and signs APK/AAB
   - Uploads to Google Play (internal → beta → production)

---

## 📞 Getting Help

- **Questions:** Open a GitHub Discussion
- **Bugs:** Create an issue with `bug` label
- **Feature Requests:** Create an issue with `enhancement` label

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Happy Coding! 🎉**
