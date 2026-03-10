# Habitao - Development Log

**Purpose:** Track implementation progress, document decisions, record solutions, and maintain context across development sessions.

**Last Updated:** March 10, 2026 - Editor Overhaul, Stats Compaction, Pomodoro Polish, APK Signing Fix

---

## Current Phase: Production-Quality UI/UX and Feature Completeness

**Goal:** Achieve pixel-perfect Material Design 3 consistency, fix nested data hierarchies, implement live WYSIWYG markdown editing, and pass security audit.

**Branch:** `feature/improvements`

**Status:** Editor overhaul complete, CTO directives round 3 implemented. PR #12 (`feature/improvements` -> `dev`) active.

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

#### tasks7 (All Complete)
| Feature | Description | Status |
|---------|-------------|--------|
| Stats Card Gaps | Added defaultMinSize(220.dp) for proper spacing between text and ring | Complete |
| Month X-axis Labels | Fixed label logic: per-data-point labels, every 5th shown for month view | Complete |
| Day Chip Text | Single-letter abbreviations (M,T,W,T,F,S,S), maxLines=1, centered text | Complete |
| Markdown Cursor Fix | Switched to TextFieldValue for cursor control after auto-format | Complete |
| Checkbox Auto-Continue | Added [x] checkbox continuation on Enter | Complete |
| Formatting Toolbar | Added inline toolbar (Bold, Italic, Strikethrough, Code, H1, List, Checkbox) | Complete |
| Repo Audit | Source dirs mixed (java/kotlin) noted as cosmetic; no sensitive files tracked | Complete |

#### tasks8 (All Complete)
| Feature | Description | Status |
|---------|-------------|--------|
| Task Reminders | Injected TaskReminderScheduler into CreateTaskViewModel; BootReceiver reschedules task reminders | Complete |
| Subtask Depth Limit | MAX_SUBTASK_DEPTH=2, depth check on parent chain, UI rendering capped | Complete |
| Backup & Restore | BackupManager with JSON export/import for all 7 entity types, SAF integration | Complete |
| Stats Card Spacing | Removed weight(1f) from ring box, explicit 24dp text-ring gap | Complete |
| Task Alignment | Always-render priority bar (transparent for NONE) to fix checkbox axis | Complete |
| Task Group Backgrounds | Surface wrapper with horizontal margin for parent+subtask groups | Complete |
| Day Chip Sizing | BoxWithConstraints for uniform fixed-width chips, 3-letter abbreviations | Complete |
| Routine Step Duration | Display estimatedDurationMinutes on routine step rows | Complete |
| Settings Merge | Combined Export/Import into single Backup & Restore sub-view | Complete |
| Repo Cleanup | Removed nul file, untracked docs/tasks/ from git, trimmed verbose comments | Complete |
| Performance | @Immutable annotations, distinctUntilChanged on stats flows, highest refresh rate | Complete |

#### Editor Overhaul & CTO Directives (PR #12)
**Session: Mar 9-10, 2026**

##### Editor Overhaul (10 tasks)
| Feature | Description | Status |
|---------|-------------|--------|
| Cursor-Aware Markdown | MarkdownVisualTransformation highlights syntax around cursor position | Complete |
| Toolbar Relocation | Markdown toolbar anchored above keyboard, scrollable formatting row | Complete |
| Undo/Redo | UndoRedoManager with debounced checkpointing (400ms, max 50 states) | Complete |
| List Continuation | Auto-continue bullets, numbered lists, checkboxes on Enter | Complete |
| BringIntoViewRequester | Replaced blind scroll-to-bottom with cursor-rect-based scroll | Complete |
| Checkbox Toggle | 3-state toolbar cycle: none -> [ ] -> [x] -> remove | Complete |
| Task Card Uniformity | Consistent card background, padding, typography across task list | Complete |
| Completed Task Dimming | `COMPLETED_TASK_ALPHA = 0.45f` applied to completed task rows | Complete |
| Indent/Outdent | Context-aware list indent (2-space nesting) and outdent toolbar buttons | Complete |
| Region Caching | Inline regions cached on text content, avoids re-parsing on cursor moves | Complete |

##### CTO Directives Round 1-2
| Fix | Description | Status |
|-----|-------------|--------|
| Debounced Toolbar Focus | 250ms debounce on focus loss to prevent flicker during scroll gestures | Complete |
| Auto-scroll Gap | 120f pixel margin below cursor rect in bringIntoView | Complete |
| Auto-scroll on Any Input | Changed trigger from lineCount to textFieldValue.text changes | Complete |

##### CTO Directives Round 3
| Fix | Description | Status |
|-----|-------------|--------|
| Bouncing Screen Fix | Moved imePadding() from individual elements to outer Column container | Complete |
| Status Bar Fix | Removed nestedScroll + scrollBehavior from editor Scaffold | Complete |
| BringIntoView Stability | Removed isImeVisible from LaunchedEffect keys to prevent animation-frame restarts | Complete |
| Stats Dashboard Compact | Reduced card heights (148->110dp, 126->100dp), horizontal layout for top row | Complete |
| Pomodoro Time Grid | Replaced TextButton with OutlinedButton + weight(1f) for proper 2x2 alignment | Complete |
| Tomato AOD Fill | Removed animateFloatAsState batching; cells light individually via raw progress | Complete |
| APK Signing Fix | Removed applicationIdSuffix, committed debug.keystore, bumped to v0.1.7 | Complete |
| Session Save Robustness | Added NonCancellable + Dispatchers.IO to saveSession in TimerService | Complete |
| CI Keystore Verification | Added keystore existence check step in release.yml workflow | Complete |

#### Key Architecture Decisions:
- MarkdownVisualTransformation uses identity offset mapping to avoid cursor desync.
- Switched description field to TextFieldValue for cursor position control after auto-formatting.
- Subtasks use flat `parentTaskId` references in Room, with recursive tree-building in ViewModel.
- Auto-formatting uses character diff detection to identify Enter key presses.
- Cross-feature dependency (settings -> pomodoro) documented as tech debt with TODO.
- BroadcastReceivers use goAsync() + coroutine pattern instead of runBlocking.
- `imePadding()` must be applied at the container level, never on individual child elements inside a Column. Applying it per-element causes per-frame layout thrashing as each element independently reacts to keyboard animation.
- `WindowInsets.ime` reads should not be used as LaunchedEffect keys; they change on every animation frame and cause feedback loops with bringIntoView.
- `applicationIdSuffix` changes the installed package name; removing it ensures debug and CI builds install as the same app for in-place updates.
- Tomato AOD animation: `animateFloatAsState` batches multiple cell threshold crossings into one visual frame; using raw progress ensures true one-cell-at-a-time fill.

---

## Completed (continued)

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
1. **PR #12 Test and Merge** (Priority: High)
   - Build alpha APK from `feature/improvements`, verify all fixes on device, merge into `dev`

2. **Pomodoro Data Verification** (Priority: High)
   - Verify focus session data appears on Stats dashboard after completing a full Pomodoro cycle
   - If still empty, add runtime logging to confirm saveSession() execution and DAO query results

3. **Test Coverage** (Priority: Medium)
   - Unit tests for StatsViewModel, TasksViewModel subtask tree, markdown auto-format

4. **Widget** (Priority: Medium)
   - Home screen widget for habit/task quick-check

---

## Issues & Blockers

### Active Issues
- **Pomodoro Stats Data:** Stats dashboard Pomodoro card may show empty data after completing sessions. Data layer code is architecturally correct; the `NonCancellable + Dispatchers.IO` fix in TimerService.saveSession() should resolve any race condition between session save and service lifecycle cancellation. Awaiting user verification.

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
- `ef562c3` - fix: Stabilize editor layout, compact stats cards, fix APK signing and tomato fill logic
- `fc68b94` - fix: Streak phantom fix, pomodoro stats card, session tracking, pixel tomato AOD, editor stability, task perf
- `3f8d5f2` - fix: Auto-scroll on all input, outdent button, save button IME fix, status bar fix, perf optimizations
- `636de73` - fix: Context-aware list indent, debounced toolbar focus, cursor-aware auto-scroll

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

#### 2026-03-10: CTO Directives Round 3 - Layout Stability, Stats Compaction, APK Signing
- Fixed critical editor bouncing: moved `imePadding()` from per-element to container-level Column. Root cause was each child independently reacting to IME inset changes on every animation frame.
- Removed `nestedScroll(scrollBehavior.nestedScrollConnection)` from editor Scaffold to prevent TopAppBar color transitions from flashing the status bar black during text selection.
- Fixed `bringIntoView` LaunchedEffect feedback loop: removed `isImeVisible` from effect keys. The IME visibility changes on every keyboard animation frame, causing the effect to restart continuously.
- Compacted Stats dashboard: reduced card heights (148->110dp row 1, 126->100dp row 2), switched Tasks/Daily Goal to horizontal layout with progress ring on right side, combined Pomodoro sessions/rounds into one line.
- Styled Pomodoro time-adjust dialog: replaced bare `TextButton` with `OutlinedButton` + `weight(1f)` for proper 2x2 grid alignment.
- Fixed tomato AOD animation: removed `animateFloatAsState(tween(800ms))` wrapper that was batching multiple cell threshold crossings into single visual frames. Now uses raw progress for true sequential single-cell fill.
- Fixed APK in-place updates: removed `applicationIdSuffix = ".debug"` and `versionNameSuffix = "-DEBUG"` so all builds install as `com.habitao.app`. Committed `app/debug.keystore` to git (was untracked, causing CI to use a different signing key). Fixed `.gitignore` pattern from `!debug.keystore` to `!**/debug.keystore`. Bumped version to 0.1.7 (versionCode 7).
- Added `NonCancellable + Dispatchers.IO` to `TimerService.saveSession()` to prevent session data loss when service lifecycle is cancelled after timer completion.
- Added keystore verification step in `release.yml` CI workflow.

**Key Insight:** `imePadding()` is a layout modifier, not a spacing utility. When applied to individual elements inside a Column, it creates competing layout calculations. The correct pattern is one `navigationBarsPadding().imePadding()` at the container level — `navigationBarsPadding()` consumes nav bar insets first, then `imePadding()` adds only the keyboard-minus-nav-bar difference.

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
- Release signing config not yet configured (only debug keystore is set up)
- Pomodoro Stats data binding needs on-device verification; code is correct but may have timing edge cases

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
