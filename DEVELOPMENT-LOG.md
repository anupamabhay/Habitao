# Habitao - Development Log

**Purpose:** Track implementation progress, document decisions, record solutions, and maintain context across development sessions.

**Last Updated:** March 4, 2026 - UI/UX Overhaul, Nested Subtasks, Live Markdown, Security Audit

---

## Current Phase: Production-Quality UI/UX and Feature Completeness

**Goal:** Achieve pixel-perfect Material Design 3 consistency, fix nested data hierarchies, implement live WYSIWYG markdown editing, and pass security audit.

**Branch:** `feature/improvements`

**Status:** All tasks1 through tasks6 completed. PR #6 open to merge into `dev`.

---

## Completed

### Phase 0: Planning (Complete)
- Comprehensive planning documentation (6 documents, 30,000+ words)
- Research completed: Material Design 3, tech stack comparison, security practices
- Architecture decisions documented (Clean Architecture + MVI)
- Data model designed (8 entities with relationships)
- Git repository initialized with `dev` as primary branch
- Multi-module Gradle structure (14 modules)
- Version catalog with all dependencies configured
- ProGuard rules templates created
- Detekt configuration set up

### Phase 1: Foundation (Complete)
- AndroidManifest.xml with permissions and notification channels
- Application class with Hilt DI
- Material Design 3 theme (light/dark, dynamic colors)
- MainActivity with Jetpack Compose and Jetpack Navigation
- Room Database with Habit and HabitLog entities
- DataStore for user preferences

### Phase 2: Core Habits Feature (Complete)
- Habit list view with Material Design 3 theming
- Create/edit habit form with all frequency types:
  - Daily habits
  - Specific days of week
  - X times per week
  - Every X days (interval-based)
- Three habit types:
  - Simple (Yes/No completion)
  - Measurable (numeric targets with progress tracking)
  - Checklist (sub-tasks that can be checked off)
- Habit completion tracking with daily logs
- Week calendar strip for date navigation
- Streak calculation and display
- Stats page with habit statistics
- Empty state handling
- Reminder/notification settings per habit

### Phase 3: Bug Fixing (In Progress)
**Session: Feb 17-20, 2026**

#### Round 1 Bugs (All Fixed - Commit `900c5d7`)
| Bug | Fix | Status |
|-----|-----|--------|
| Scheduling filter missing | Re-added `isScheduledFor(date)` filter to repository | Fixed |
| Progress resets daily | Added weekly progress aggregation for TIMES_PER_WEEK | Fixed |
| Horizontal alignment issues | Fixed empty state padding to match LazyColumn | Fixed |
| EVERY_X_DAYS visibility | Changed to always visible with cycle tracking | Fixed |
| Flow error handling | Added `.catch` to observeHabitsForDate | Fixed |
| DAO optimization | Added `getLogsForHabitBetweenDates` query | Fixed |

#### Round 2 Bugs (All Fixed - Commit `4127023`)
| Bug | Fix | Status |
|-----|-----|--------|
| Empty state not centered | Added `verticalArrangement = Arrangement.Center`, removed redundant button | Fixed |
| Stats page shows 0 streak | Changed to reactive flow with `observeAllHabits()` | Fixed |
| "Daily reminder" misleading | Changed to "Remind me" with schedule-based description | Fixed |
| TIMES_PER_WEEK not hiding | Added filter in HabitsViewModel to hide after target met | Fixed |

### Phase 4: UI/UX Overhaul (feature/improvements branch)
**Session: Mar 3-4, 2026**

#### tasks1-tasks4 (All Complete)
| Feature | Description | Status |
|---------|-------------|--------|
| Priority Color Standardization | LOW=Blue, MEDIUM=Amber, HIGH=Red (TickTick convention) | Complete |
| Specific Days for Routines | Added SPECIFIC_DATES repeat pattern to routines schedule | Complete |
| Stats Page Layout | Reordered card hierarchy: Title+Stat top, Ring bottom | Complete |
| Task Spacing | Adjusted spacing constants across task list items | Complete |
| Markdown Support | Added MarkdownText composable with inline rendering | Complete |
| Copilot PR Fixes (Round 2) | 13 findings: ANR fixes, alarm rationale, abs(hashCode), stale date, etc. | Complete |

#### tasks5 (All Complete)
| Feature | Description | Status |
|---------|-------------|--------|
| Routine Schedule Selector | 2-row segmented grid (Daily/Specific days, Every X days) | Complete |
| Stats Card Hierarchy | Title+stat at top, progress ring pushed to bottom | Complete |
| Create Button Consistency | All 3 create screens use identical bottomBar Button with navigationBarsPadding | Complete |
| Chart Bars | Thinner bars (0.55f width), scrollable Day view, per-point X-axis labels | Complete |
| Live Markdown VT | MarkdownVisualTransformation with identity offset mapping, no separate preview | Complete |

#### tasks6 (All Complete)
| Feature | Description | Status |
|---------|-------------|--------|
| Day Selector Full Width | Replaced FlowRow with weighted Row so chips span full container width | Complete |
| Stats Cards Polish | Left-aligned text, taller cards, redesigned routine card with icon badge | Complete |
| Markdown Headers | Added #1-6 header rendering with scaled font sizes, markers dimmed | Complete |
| Markdown Auto-Format | Auto-continue lists/numbered lists/checkboxes on Enter, remove empty markers | Complete |
| Nested Subtasks | Recursive subtask saving, recursive ViewModel tree, depth-based UI rendering | Complete |
| Security Audit | No sensitive files tracked, added nul to .gitignore | Complete |

#### Key Architecture Decisions:
- MarkdownVisualTransformation uses identity offset mapping to avoid cursor desync.
- Subtasks use flat `parentTaskId` references in Room, with recursive tree-building in ViewModel.
- Auto-formatting uses character diff detection to identify Enter key presses.
- Cross-feature dependency (settings -> pomodoro) documented as tech debt with TODO.
- BroadcastReceivers use goAsync() + coroutine pattern instead of runBlocking.

---

## In Progress

### Cross-Feature UX and Analytics (feature/routines-and-tasks branch)
**Status:** Merged into dev.

#### Completed Work:
| Feature | Description | Status |
|---------|-------------|--------|
| Stats Graph Data Flow | Fixed activity graph binding to real habit/routine/task completion data | Complete |
| Stats Graph Modes | Added bar/line graph switching with persistent user preference | Complete |
| Stats Time Filters | Day/Week/Month filtering now drives chart range and labels correctly | Complete |
| Routine Screen Redesign | Added overview card, icon badges, and cleaner progress hierarchy | Complete |
| Routine Step UX | Added visual state backgrounds and refined completion progress styling | Complete |
| Settings Main View | Added sectioned settings UI with dedicated General/About groups | Complete |
| Theme Settings | Added theme mode selection (System/Light/Dark) with persistence | Complete |
| Tab Bar Settings | Added configurable visible tab count (3-5) and tab list controls | Complete |
| Settings Persistence | Extended AppSettingsManager with max tabs, theme mode, graph type | Complete |

#### Key Decisions:
- Persist visualization preferences (graph type) in app settings to keep analytics behavior consistent across sessions.
- Keep routine cards information-dense but scannable using overview metrics and compact per-item progress.
- Split settings into dedicated sub-views to reduce cognitive load and support future growth.

---

## Next Up (Prioritized)

### After This Session
1. **PR #6 Review and Merge** (Priority: High)
   - Merge feature/improvements into dev after Copilot review passes
   - Run full CI pipeline validation

2. **Test Coverage Improvement** (Priority: Medium)
   - Unit tests for StatsViewModel aggregation paths
   - Unit tests for nested subtask tree building in TasksViewModel
   - Unit tests for markdown auto-formatting logic

3. **Widget Implementation** (Priority: Medium)
   - Home screen widget for habit/task quick-check

---

## Issues & Blockers

### Active Issues
None - all known bugs have been fixed, awaiting user verification.

### Resolved Issues

#### Issue: Habits not showing after creation
**Date:** Feb 17, 2026  
**Cause:** Missing `isScheduledFor(date)` filter in repository after refactor  
**Solution:** Re-added the filter to `observeHabitsForDate` in HabitRepositoryImpl  
**Commit:** `557aa13`

#### Issue: Empty state not centered
**Date:** Feb 19, 2026  
**Cause:** Missing `verticalArrangement` in Column and parent Box needed `fillMaxWidth()`  
**Solution:** Added `Arrangement.Center` and proper width modifiers  
**Commit:** `4127023`

#### Issue: Stats page showing 0 streak
**Date:** Feb 20, 2026  
**Cause:** `loadHabitStats()` used one-time `getAllHabits()` which may have had issues  
**Solution:** Switched to reactive `observeAllHabits()` flow with proper combine  
**Commit:** `4127023`

#### Issue: TIMES_PER_WEEK habits showing after target met
**Date:** Feb 20, 2026  
**Cause:** No filtering logic for weekly progress  
**Solution:** Added filter in HabitsViewModel combine block to hide habits after weekly target met (unless completed today)  
**Commit:** `4127023`

---

## Decisions & Solutions

### Architecture Decisions

#### Decision: Switch from Voyager to Jetpack Navigation
**Date:** 2026-02-17  
**Rationale:** 
- Voyager had edge cases with screen lifecycle
- Jetpack Navigation is more stable and widely supported
- Will evaluate navigation for iOS separately when the time comes
- Better tooling and debugging support

**Original Decision (2026-02-13):** Voyager was initially chosen for KMP-readiness, but practical issues led to switch.

#### Decision: Proto DataStore + Tink instead of EncryptedSharedPreferences
**Date:** 2026-02-13  
**Rationale:**
- EncryptedSharedPreferences deprecated in 2026
- Proto DataStore is type-safe and reactive
- Tink provides hardware-backed encryption via Android Keystore

**Documented In:** `docs/04-SECURITY-PRIVACY.md`

### Implementation Solutions

None yet - will document as we encounter and solve issues.

---

## Metrics & Progress

### Code Statistics
```
Modules Implemented: 11/14 (app, domain, data, core/common, core/ui, feature/habits, feature/pomodoro, feature/routines, feature/tasks, feature/settings, system/*)
Features Complete: 4/4 core modules implemented (Habits, Pomodoro, Routines, Tasks) with ongoing refinement
Test Coverage: TBD (next phase)
```

### Recent Commits (feature/improvements branch)
- `f967af8` - feat: tasks5 - routine selector grid, stats card hierarchy, consistent create buttons, thinner chart bars, live markdown VisualTransformation
- `189f500` - fix: Stats card padding, chart bar gaps, segmented button wrapping, bottom nav padding
- `2c7d6d2` - fix: Address Copilot PR review - ANR fixes, alarm rationale, markdown parsing, hashCode safety, stale date
- `8830e63` - style: Fix ktlint violations - line length, trailing commas, import ordering
- `beac80b` - feat: implement tasks4.md - fix priority colors, add specific days, improve stats layout, task spacing, markdown support
- `d8ad133` - fix: address Copilot PR review + implement tasks3.md improvements

---

## Code Review Notes

### Patterns to Follow
1. **MVI State Management:** All ViewModels use StateFlow with immutable state
2. **Error Handling:** Use `Result<T>` wrapper, no naked try-catch
3. **Naming:** PascalCase for classes, camelCase for functions
4. **Tests:** Write tests BEFORE implementation (TDD where possible)

### Anti-Patterns to Avoid
- Avoid Android imports in domain layer
- Avoid type suppression (`as any`, `@ts-ignore`)
- Avoid empty catch blocks
- Avoid hardcoded strings (use resources)
- Avoid mutable state in ViewModels

---

## Notes & Learnings

### Session Notes

#### 2026-03-04: UI/UX Overhaul, Nested Subtasks, Live Markdown (tasks6)
- Replaced FlowRow day selectors with weighted Row layout so day chips span full container width.
- Left-aligned stat card text (Tasks, Daily Goal), increased card padding to 24dp for better proportions.
- Redesigned Routine stats card with icon badge in rounded container and thicker 8dp progress bar.
- Added header rendering (#1 through #6) to MarkdownVisualTransformation with scaled font sizes and dimmed markers.
- Implemented auto-formatting: list continuation on Enter (bullets, numbered, checkboxes), empty marker removal.
- Fixed nested subtasks: removed parentTaskId guard in saveTask(), built recursive subtask tree in TasksViewModel, added depth-based indentation in TasksScreen.
- Security audit: no sensitive files tracked. Added `nul` to .gitignore (Windows git add artifact).
- Build verified: ktlintCheck + assembleDebug pass with 0 errors.

**Key Insight:** VisualTransformation with identity OffsetMapping is the correct approach for live inline markdown; it avoids cursor position desync that occurs with text replacement approaches.

#### 2026-03-03: UI/UX Overhaul (tasks5)
- Routine schedule selector changed to 2-row segmented grid matching Habits layout.
- Stats cards reordered: Title+Stat text at top, progress ring at bottom (SpaceBetween arrangement).
- Create buttons standardized across all 3 screens: bottomBar Button with navigationBarsPadding(), no Surface wrapper.
- Chart bars thinned (0.55f group width), Day view made scrollable, per-data-point X-axis labels.
- Live markdown VisualTransformation implemented with identity offset mapping, markers dimmed in-place.
- Copilot PR review (13 items) addressed: goAsync() for receivers, abs(hashCode), alarm rationale dialog, stale date fix.

#### 2026-02-27: Stats, Routines, and Settings Updates
- Fixed Stats activity graph to use real aggregated data from habits, routines, and tasks.
- Added graph mode switching (bar/line) and persisted selection in app settings.
- Connected Day/Week/Month filter state directly to chart range generation and labels.
- Redesigned Routines screen with a summary overview card and refined per-routine progress presentation.
- Updated routine rows with icon badges and clearer completion-state visuals.
- Expanded Settings with dedicated General and About sections.
- Added configurable tab visibility limit (3-5 tabs) and persisted theme mode.

**Key Insight:** Analytics and configuration flows should be persisted and shared across screens to keep the user experience predictable.

#### 2026-02-23: Bug Fixes, Security Audit & Cleanup
- Fixed calendar week-scrolling direction (minusWeeks -> plusWeeks)
- Added snapshotFlow auto-selection on page scroll to update month header text
- Fixed Sunday truncation by switching DateChip from fixed 52.dp width to Modifier.weight(1f)
- Fixed Pomodoro crash: manifest had FOREGROUND_SERVICE_SHORT_SERVICE but service uses specialUse type
- Changed to FOREGROUND_SERVICE_SPECIAL_USE permission and startForegroundService() for timer start
- Security audit: no API keys, secrets, or sensitive data found in codebase
- Updated .gitignore to cover %TEMP%/, logs/, and docs/ui designs/
- Removed 6 placeholder stub files from modules with real code
- Converted all verbose KDoc comments to brief inline comments across data, domain, and feature layers
- Removed all emojis from CONTRIBUTING.md and DEVELOPMENT-LOG.md
- Rewrote README.md to reflect current feature state and professional standards
- Build verified: assembleDebug passes with 0 errors

#### 2026-02-23: PR Review & Fixes
- Addressed Copilot PR review comments for Pomodoro feature
- Fixed sound playing for the *next* session by capturing session type before advancing
- Ensured TimerService stops foreground execution when idle to clear sticky notifications
- Updated foreground service type to `specialUse` (Android 14+ compliance) instead of `shortService`
- Wired `todaysCompletedRounds` in StatsViewModel to actual preferences data
- Merged planned features into existing documentation (`00-PROJECT-OVERVIEW.md`, `01-PRODUCT-REQUIREMENTS.md`)

#### 2026-02-23: Planning & Research Session
- Created PR #3 to merge feature/pomodoro into dev
- Researched Pomodoro UX enhancements from Forest, TickTick, Focus To-Do, Be Focused
- Researched Compose performance best practices (recomposition, 120Hz, baseline profiles)
- Researched calendar widget optimization (HorizontalPager vs LazyRow)
- Removed redundant "auto-start limit" setting (no competitor app has this)
- Created comprehensive feature planning doc (docs/07-PLANNED-FEATURES.md)
- Fixed completion sound infinite loop (MediaPlayer + 10s timeout)
- Made Stats page the default navigation tab
- Added real-time focus time updates to Stats screen

**Key Findings:**
- Calendar should use HorizontalPager with snap-to-week, not LazyRow with thousands of items
- Compose state reads during scroll should use lambda modifiers to skip recomposition
- Forest/TickTick use narrative animations (growing trees, shrinking tomatoes) for engagement
- Auto-start should cycle through all sessions until totalSessions goal, no separate limit needed

#### 2026-02-22: Pomodoro Feature - Polish & Settings
- Redesigned PomodoroSettingsSheet with M3 grouped cards (Duration, Cycles, Sound & Vibration)
- Updated Today's Focus to show exact duration (e.g., "1h 12m 33s")
- Added daily "Rounds" counter (1 round = completing all configured sessions)
- Fixed sound picker to show "Default" instead of "Silent", with "SILENT" marker for explicit silent choice
- Renamed confusing "Auto pomodoro cycle" label to "Auto-start limit"
- Added spacing and alignment fixes throughout settings UI
- Handled "SILENT" marker in TimerService sound playback
- Research: Analyzed TickTick, Forest, Pomofocus, Be Focused for UX patterns

#### 2026-02-21: Pomodoro Feature - Core Implementation
- Implemented full Pomodoro timer with foreground service
- Fixed ANR bug (cached PendingIntents, timer on background dispatcher)
- Fixed session counter logic (reset after long break)
- Added confirmation dialogs for Stop/Skip
- Added completion feedback (sound, haptic, notification)
- Implemented customizable settings (durations, session counts)
- Added auto-start next pomodoro / auto-start break
- Added system ringtone picker for ending sounds
- Added vibration toggle and duration settings
- Added total sessions goal with auto-completion

#### 2026-02-20: Bug Fixing Session 2
- Resumed bug fixing from previous session
- Fixed 4 remaining bugs:
  - Empty state centering (Issue 1)
  - Stats page 0 streak (Issue 2)
  - Reminder label wording (Issue 3)
  - TIMES_PER_WEEK hiding logic (Issue 4)
- Committed and pushed all fixes (commit `4127023`)
- Updated DEVELOPMENT-LOG.md with full project history

**Key Insight:** Use reactive flows (`observeAllHabits()`) instead of one-time fetches for data that needs to update in real-time.

#### 2026-02-19: Bug Fixing Session 1
- User reported multiple bugs during APK testing
- Fixed scheduling filter and empty state alignment
- Fixed EVERY_X_DAYS visibility logic
- Added weekly progress aggregation for TIMES_PER_WEEK
- Committed fixes (commits `557aa13`, `900c5d7`)

**Key Insight:** Complex scheduling logic (interval habits, weekly targets) needs thorough testing with different date scenarios.

#### 2026-02-17: User Testing Started
- Core habits feature complete
- User began APK testing
- Multiple bugs discovered in scheduling and display logic

#### 2026-02-13: Project Kickoff
- Completed comprehensive planning phase
- Set up multi-module Gradle structure
- Decision to create development log for context tracking

**Key Insight:** Having detailed documentation upfront prevents architectural mistakes.

---

## Future Enhancements (Post-MVP)

### Deferred Features
- Month calendar view (v1.1)
- Cloud sync (v2.0)
- Advanced statistics with heatmaps (v1.1)
- Habit templates library (v1.1)
- Data import from other apps (v1.2)
- Wear OS support (v2.0+)

### Technical Debt to Address
- Cross-feature dependency: `feature/settings` depends on `feature/pomodoro` for `PomodoroSettingsSheet` (TODO in settings/build.gradle.kts)
- No unit tests for markdown auto-formatting or nested subtask tree building
- Hardcoded dp values remain in some older composables (pre-tasks1 code)
- `SPECIFIC_DATES` enum value is functionally identical to `WEEKLY` for routines (kept for Habits compatibility)

---

## Reference Quick Links

### Documentation
- [Product Requirements](./docs/01-PRODUCT-REQUIREMENTS.md)
- [Technical Architecture](./docs/03-TECHNICAL-ARCHITECTURE.md)
- [Data Model](./docs/02-DATA-MODEL-SCHEMA.md)
- [Security](./docs/04-SECURITY-PRIVACY.md)
- [Testing Strategy](./docs/05-TESTING-STRATEGY.md)

### External Resources
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Voyager Navigation](https://voyager.adriel.cafe/)

---

## Update Convention

**When to Update:**
- Start of each work session
- After completing a task
- When encountering/resolving issues
- End of each sprint

**Format:**
```markdown
### [Date] - [Session Title]
**Tasks Completed:**
- Task 1
- Task 2

**Issues Encountered:**
- Issue description

**Solutions Applied:**
- Solution description

**Next Session:**
- Planned tasks
```

---

**End of Development Log**
