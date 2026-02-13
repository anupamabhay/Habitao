# Habitao - Project Overview & Master Plan

**Project Name:** Habitao  
**Type:** Native Android Productivity App  
**Tech Stack:** Kotlin Multiplatform + Jetpack Compose  
**Status:** Planning Phase Complete  
**Target Release:** MVP in 8-12 weeks

---

## 📋 Quick Links

- [Product Requirements](./01-PRODUCT-REQUIREMENTS.md) - Features, user stories, MVP scope
- [Data Model & Schema](./02-DATA-MODEL-SCHEMA.md) - Database design, entities, relationships
- [Technical Architecture](./03-TECHNICAL-ARCHITECTURE.md) - System design, tech stack, patterns
- [Security & Privacy](./04-SECURITY-PRIVACY.md) - Encryption, permissions, data protection
- [Testing Strategy](./05-TESTING-STRATEGY.md) - Test pyramid, frameworks, automation
- [UI/UX Guidelines](./06-UI-UX-DESIGN.md) - Material Design 3, components, design tokens
- [DevOps & CI/CD](./07-DEVOPS-CICD.md) - Pipeline, branching, release process
- [Development Roadmap](./08-DEVELOPMENT-ROADMAP.md) - Sprints, milestones, timeline

---

## 🎯 Project Vision

**Problem:** Users currently juggle multiple apps for habits, tasks, routines, and focus timers (TickTick + habit tracker + Pomodoro app).

**Solution:** Habitao - A unified, beautifully designed Android app combining:
- **Habits:** Count-based tracking with streaks
- **Routines:** Ordered multi-step sequences
- **Tasks:** TickTick/Todoist-style todos with subtasks
- **Pomodoro:** Focus timer with session tracking

**Unique Value:** Native Android performance + Material Design 3 Expressive + Local-first privacy + Seamless integration of all productivity tools

---

## 🏗️ Architecture Decision Summary

### Why Kotlin Multiplatform (KMP) + Jetpack Compose?

Based on comprehensive research of industry leaders (TickTick, Todoist, Microsoft To Do), the decision is:

**✅ CHOSEN: Kotlin Multiplatform + Jetpack Compose**

**Key Reasons:**
1. **Industry Standard:** Todoist (pioneer of KMP), TickTick, Microsoft To Do all use native development
2. **Performance:** Direct native compilation, 60fps guaranteed for complex list views
3. **Widgets:** Native `RemoteViews` support without cross-platform bridging complexity
4. **Background Reliability:** First-class `AlarmManager`, `WorkManager`, `ForegroundService` access
5. **Material Design 3 Expressive:** Latest 2026 design system fully supported in Compose
6. **Future-Proof:** Can share business logic with iOS via KMP without rewriting Android app
7. **Your Background:** Natural progression from Java → Kotlin (similar syntax, same ecosystem)

**Rejected:** React Native (would require native modules for widgets/alarms, introduces JS bridge overhead, lags behind on MD3 Expressive features)

---

## 📊 Tech Stack

### Core Technologies

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Language** | Kotlin | 2.0+ | Modern, null-safe, concise |
| **UI** | Jetpack Compose | 1.7+ | Declarative UI, MD3 Expressive |
| **Architecture** | Clean Architecture + MVI | - | Separation of concerns, predictable state |
| **DI** | Hilt | 2.51+ | Compile-time dependency injection |
| **Database** | Room | 2.7+ (KMP) | Type-safe local persistence |
| **Preferences** | Proto DataStore + Tink | 1.1+ | Encrypted settings storage |
| **Navigation** | Voyager | 1.1+ | Type-safe Compose navigation |
| **Async** | Coroutines + Flow | 1.8+ | Structured concurrency, reactive streams |

### Android System
- AlarmManager (exact reminders)
- WorkManager (background jobs)
- NotificationCompat (notifications)
- RemoteViews (widgets)
- Android Keystore (secure keys)

### UI Libraries
- Material 3 Compose (MD3 Expressive components)
- Vico (charts for statistics)
- kizitonwose/Calendar (calendar views)
- Coil (image loading)

### Testing
- JUnit 5 (unit tests)
- Mockk (mocking)
- Compose Test (UI tests)
- Maestro (E2E tests)
- Turbine (Flow testing)

### DevOps
- Detekt + Ktlint (code quality)
- GitHub Actions (CI/CD)
- Firebase Crashlytics (crash reporting)
- Firebase Performance (performance monitoring)

---

## 🎨 Design System

**Material Design 3 Expressive (2026)**

The latest evolution of Material Design, introduced in May 2025 with Android 16.

**Key Features:**
- **Expressive Shapes:** Morphing stars, clovers, organic forms (not just rounded corners)
- **Physics-Based Motion:** Springs, bounces, weight-aware animations
- **Vibrant Colors:** High-contrast, deeper tones, dynamic theming
- **Large Typography:** Headlines as graphical elements
- **Hero Layouts:** Images bleeding into status bars, split-pane views

**Implementation:**
- Jetpack Compose Material 3 library v1.4.0+
- Dynamic color scheme from wallpaper
- Expressive shape templates
- Spring animations for micro-interactions

---

## 🗂️ Module Structure

```
habitao/
├── app/                    # Android application module
├── feature/                # Feature modules (UI + ViewModels)
│   ├── habits/
│   ├── routines/
│   ├── tasks/
│   └── pomodoro/
├── domain/                 # Business logic (pure Kotlin, KMP-ready)
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/                   # Data layer (repositories, database, API)
│   ├── local/
│   │   ├── database/       # Room
│   │   └── preferences/    # Proto DataStore
│   └── remote/             # Future: API client
├── core/                   # Shared utilities
│   ├── common/             # Pure Kotlin utils
│   ├── ui/                 # Shared Compose components
│   └── testing/            # Test utilities
└── system/                 # Android system integrations
    ├── notifications/
    ├── alarms/
    ├── work/
    └── widget/
```

---

## 🔐 Security & Privacy

### Data Encryption
- ✅ **Proto DataStore + Google Tink:** AES-256-GCM for app settings
- ✅ **Android Keystore:** Hardware-backed key storage
- ✅ **Encrypted Backups:** Daily automated backups with encryption
- ⚠️ **Room Database:** NOT encrypted (relies on Android filesystem encryption for performance)

### Privacy
- ✅ **Local-First:** All data stored on device (no cloud in MVP)
- ✅ **No Account Required:** No email, phone, or personal identifiers
- ✅ **Export Anytime:** JSON export of all user data
- ✅ **GDPR-Ready:** Right to access, right to deletion

### Permissions
- `POST_NOTIFICATIONS` - Habit/task reminders
- `SCHEDULE_EXACT_ALARM` - Precise timing for reminders
- `FOREGROUND_SERVICE` - Pomodoro timer background operation
- `RECEIVE_BOOT_COMPLETED` - Restore alarms after reboot

### Code Protection
- R8 full mode obfuscation
- ProGuard rules for sensitive code
- No hardcoded secrets (Keystore only)
- OWASP dependency scanning

---

## 🧪 Testing Strategy

### Test Pyramid (70/20/10)

```
        ┌─────────┐
        │   E2E   │  10% - Maestro (critical user flows)
        └─────────┘
     ┌─────────────┐
     │ Integration │  20% - Repository + Room, ViewModel + UseCase
     └─────────────┘
  ┌─────────────────┐
  │  Unit Tests     │  70% - Use Cases, ViewModels (mocked repos)
  └─────────────────┘
```

### Coverage Targets
- **Domain Layer:** 90%+ (business logic must be bulletproof)
- **Data Layer:** 80%+ (repository, DAO)
- **Presentation Layer:** 60%+ (ViewModels, critical UI paths)

### Frameworks
- **Unit:** JUnit 5 + Mockk + Turbine
- **Integration:** AndroidJUnit4 + Room (in-memory)
- **UI:** Compose Test (UI components)
- **E2E:** Maestro (YAML-based flows)

---

## 🚀 Development Roadmap

### Phase 1: Foundation (Weeks 1-2)
- ✅ Project setup (multi-module Gradle structure)
- ✅ Database schema implementation (Room)
- ✅ Design system setup (MD3 tokens, theme)
- ✅ Navigation infrastructure (Voyager)
- ✅ CI/CD pipeline (GitHub Actions)

### Phase 2: Core Features - Habits (Weeks 3-4)
- Habit CRUD operations
- Day view + Week view calendar
- Count-based progress tracking
- Reminder notifications (AlarmManager)
- Streak calculation

### Phase 3: Tasks & Routines (Weeks 5-6)
- Task management with subtasks
- Due date reminders
- Routine creation with steps
- Routine scheduling

### Phase 4: Pomodoro & Integration (Week 7)
- Pomodoro timer with foreground service
- Timer notifications
- Session history
- Unified home screen

### Phase 5: Polish & Launch (Weeks 8-10)
- Home screen widget
- Statistics dashboard (basic charts)
- Dark mode refinement
- E2E testing (Maestro)
- Performance optimization
- Beta testing

### Phase 6: Release (Weeks 11-12)
- Bug fixes from beta
- Play Store listing
- Release build signing
- v1.0 Launch

---

## 📦 MVP Feature Scope

### ✅ Included in v1.0

**Habits:**
- Add/edit/delete habits with count-based tracking
- Day view + Week view calendar
- Reminder notifications
- Streak tracking
- Dark mode

**Tasks:**
- Create tasks with due dates, priority, subtasks (1 level)
- Task completion tracking
- Deadline reminders
- Today/Tomorrow/This Week views

**Routines:**
- Create routines with ordered steps
- Step completion tracking
- Routine scheduling (repeat patterns)

**Pomodoro:**
- Customizable work/break timers
- Background operation
- Completion notifications

**System:**
- Home screen widget (today's habits + tasks)
- Local storage (offline-first)
- Data export (JSON)
- Basic statistics (completion %, simple charts)

### ❌ Deferred to v1.1+

- Month calendar view (Day + Week only in MVP)
- Cloud sync (architecture ready, implementation later)
- User accounts / authentication
- Advanced statistics (heatmaps, detailed analytics)
- Habit templates library
- Projects / Categories
- Tags / Labels
- Data import from other apps
- Social features

---

## 🔀 Git Workflow

### Branching Strategy

```
main (protected, production-ready)
  │
  ├── dev (primary, latest development)
  │     │
  │     ├── feature/habits-crud
  │     ├── feature/task-management
  │     ├── feature/pomodoro-timer
  │     ├── feature/widget-implementation
  │     │
  │     └── bugfix/notification-crash
  │
  └── hotfix/critical-data-loss (emergency only)
```

**Rules:**
- ✅ `dev` is the primary branch (where all features merge)
- ✅ Create feature branches from `dev`: `feature/<description>`
- ✅ Never commit directly to `dev` or `main`
- ✅ All merges via Pull Request with code review
- ✅ Merge to `main` only for releases (after QA)
- ❌ No merges without explicit user approval

**Branch Naming:**
- `feature/habit-streak-calculation`
- `bugfix/notification-not-firing`
- `refactor/repository-pattern`
- `docs/architecture-update`

---

## 👥 Stakeholder Roles

### From User Requirements
Think from these perspectives:

| Role | Responsibilities | Concerns |
|------|------------------|----------|
| **Product Manager** | Feature prioritization, MVP scope | User value, time-to-market |
| **Tech Lead/Architect** | Architecture decisions, tech stack | Scalability, maintainability, performance |
| **Developer** | Implementation, code quality | Clean code, testability, documentation |
| **QA/Tester** | Test strategy, quality gates | Coverage, automation, bug prevention |
| **DevOps** | CI/CD, deployment, monitoring | Pipeline reliability, release process |
| **Designer** | UI/UX, Material Design compliance | User experience, accessibility, aesthetics |
| **Security Analyst** | Data protection, permissions | Encryption, privacy, compliance |
| **End User** | Daily usage, productivity | Simplicity, reliability, speed |

---

## 🎯 Success Metrics

### User Experience
- Time to first habit created: < 2 minutes
- Daily active usage rate: > 60%
- Habit completion rate: > 40%
- Widget usage: > 30%
- App responsiveness: 60fps scrolling

### Technical
- App startup time: < 1.5s (cold start)
- Notification delivery accuracy: > 95%
- Crash-free rate: > 99.5%
- Battery impact: < 2% daily drain
- APK size: < 15MB

### Quality Gates
- Zero P0 bugs before release
- 90% unit test coverage on domain/data layers
- All critical flows covered by E2E tests
- Accessibility score > 85%
- Material Design 3 compliance verified

---

## 🚨 Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Notification Reliability** (Doze, OEM battery optimization) | HIGH | Exact alarms + user education + WorkManager backup |
| **Feature Creep** | HIGH | Strict MVP definition, regular scope reviews |
| **Calendar Performance** (large datasets) | MEDIUM | LazyColumn virtualization, pagination |
| **Database Migrations** | HIGH | Version schema from day 1, migration tests |
| **Battery Drain** (background timer) | MEDIUM | AlarmManager (no wake locks), battery testing |

---

## 📚 Documentation Index

All planning documents are in the `docs/` directory:

1. **00-PROJECT-OVERVIEW.md** ← You are here
2. **01-PRODUCT-REQUIREMENTS.md** - Comprehensive feature specifications
3. **02-DATA-MODEL-SCHEMA.md** - Database design and entity relationships
4. **03-TECHNICAL-ARCHITECTURE.md** - System architecture and design patterns
5. **04-SECURITY-PRIVACY.md** - Encryption, permissions, data protection
6. **05-TESTING-STRATEGY.md** - Test pyramid, frameworks, CI integration
7. **06-UI-UX-DESIGN.md** - Material Design 3 Expressive guidelines
8. **07-DEVOPS-CICD.md** - GitHub Actions pipeline, release process
9. **08-DEVELOPMENT-ROADMAP.md** - Sprint breakdown, milestones, timeline

---

## 🏁 Next Steps

1. **Review & Approve:** User reviews all planning documents
2. **Initialize Repository:** Set up Git with `dev` as primary branch
3. **Project Setup:** Create multi-module Gradle structure
4. **Sprint 1 Kickoff:** Implement database schema + design system
5. **Iterative Development:** Feature branches → PR → dev → test → repeat
6. **Launch Preparation:** Beta testing → bug fixes → Play Store release

---

## 📝 Open Questions (Pre-Implementation)

### To Be Resolved:
- ⏳ **Onboarding Flow:** Step-by-step wizard vs. template gallery vs. blank slate?
- ⏳ **Widget Variants:** Single unified widget or separate (Habits, Tasks, Timer)?
- ⏳ **Routine Completion Logic:** All steps required or percentage-based (80%)?
- ⏳ **Pomodoro-Task Association:** Link Pomodoro to specific task in MVP or defer?
- ⏳ **Default Values:** Should new habits default to count=1 or force user input?

---

## 🔧 Development Prerequisites

### Required Tools:
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 35 (API 35)
- Git
- Gradle 8.4+

### Recommended Plugins:
- Detekt (IntelliJ/AS plugin)
- Compose Multipreview
- Room Schema Export Viewer

---

## 📞 Contact & Collaboration

**Project Owner:** [User]  
**Repository:** [To be created at C:\Development\Projects\Habitao]  
**CI/CD:** GitHub Actions  
**Issue Tracking:** GitHub Issues  

---

**Last Updated:** February 13, 2026  
**Document Status:** ✅ APPROVED - Ready for implementation

---

**End of Project Overview**
