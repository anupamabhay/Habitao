# Habitao

**Unified Productivity App for Android**

Habits • Routines • Tasks • Pomodoro Timer - All in One Beautiful App

---

## 🚀 Quick Start

This repository contains comprehensive planning documentation for Habitao, a native Android productivity app combining habit tracking, routine management, task organization, and focus tools.

**Current Status:** ✅ Planning Phase Complete - Ready for Implementation

---

## 📚 Documentation

All planning documents are in the [`docs/`](./docs) directory:

### Essential Reading (Start Here)

1. **[Project Overview](./docs/00-PROJECT-OVERVIEW.md)** ← **START HERE**
   - Executive summary, tech stack decision, roadmap
   - Quick reference for all other documents

2. **[Product Requirements (PRD)](./docs/01-PRODUCT-REQUIREMENTS.md)**
   - Features, user stories, MVP scope
   - What we're building and why

3. **[Technical Architecture](./docs/03-TECHNICAL-ARCHITECTURE.md)**
   - System design, design patterns (Clean Architecture + MVI)
   - Module structure, tech stack details

### Deep Dives

4. **[Data Model & Schema](./docs/02-DATA-MODEL-SCHEMA.md)**
   - Database design (Room), entities, relationships
   - SQL schema, indexes, migrations

5. **[Security & Privacy](./docs/04-SECURITY-PRIVACY.md)**
   - Encryption (Proto DataStore + Tink), permissions
   - GDPR compliance, data protection

6. **[Testing Strategy](./docs/05-TESTING-STRATEGY.md)**
   - Test pyramid (unit, integration, E2E)
   - Frameworks: JUnit 5, Mockk, Maestro

---

## 🎯 Project Vision

**Problem:**  
Users juggle 3-4 separate apps for productivity:
- Habit tracker (Loop, Habitica)
- Task manager (TickTick, Todoist)
- Pomodoro timer (Forest)

**Solution:**  
Habitao unifies all these tools in a single, beautifully designed native Android app following Material Design 3 Expressive principles.

**Key Features:**
- ✅ **Habits:** Count-based tracking (e.g., "3 of 8 glasses of water"), streaks, calendar views
- ✅ **Routines:** Multi-step sequences with ordered completion (e.g., "Morning routine: wake → stretch → meditate")
- ✅ **Tasks:** Todoist-style todos with subtasks, due dates, priorities
- ✅ **Pomodoro Timer:** Customizable focus sessions with background operation
- ✅ **Widgets:** Home screen quick access
- ✅ **Local-First:** Full offline functionality, optional cloud sync (future)

---

## 🏗️ Tech Stack

### Core
- **Language:** Kotlin 2.0+
- **UI:** Jetpack Compose 1.7+ (Material Design 3 Expressive)
- **Architecture:** Clean Architecture + MVI
- **Database:** Room 2.7+ (KMP-ready)
- **DI:** Hilt 2.51+
- **Navigation:** Voyager 1.1+

### Why Kotlin Multiplatform (not React Native)?

Based on research of industry leaders (Todoist, TickTick, Microsoft To Do):

✅ **Native Performance:** 60fps guaranteed for complex list views  
✅ **Widgets:** First-class support (no cross-platform bridging)  
✅ **Material Design 3 Expressive:** Latest 2026 design system fully supported  
✅ **Background Reliability:** Direct AlarmManager/WorkManager access  
✅ **Future-Proof:** Share logic with iOS later via KMP (like Todoist does)

Full justification: [docs/00-PROJECT-OVERVIEW.md](./docs/00-PROJECT-OVERVIEW.md#why-kotlin-multiplatform-kmp--jetpack-compose)

---

## 📊 Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│          PRESENTATION LAYER (Jetpack Compose)        │
│  UI Screens • ViewModels (MVI State) • Navigation   │
└───────────────────┬─────────────────────────────────┘
                    │ State Flow + Intents
┌───────────────────▼─────────────────────────────────┐
│          DOMAIN LAYER (Pure Kotlin, KMP-ready)      │
│  Use Cases • Business Logic • Repository Interfaces │
└───────────────────┬─────────────────────────────────┘
                    │ Domain Models
┌───────────────────▼─────────────────────────────────┐
│          DATA LAYER (Room + Proto DataStore)        │
│  Repository Impl • Room DAO • Remote API (future)   │
└─────────────────────────────────────────────────────┘
```

**Pattern:** MVI (Model-View-Intent) for predictable state management

Full details: [docs/03-TECHNICAL-ARCHITECTURE.md](./docs/03-TECHNICAL-ARCHITECTURE.md)

---

## 🗂️ Module Structure

```
habitao/
├── app/                  # Android application module
├── feature/              # Feature modules (UI + ViewModels)
│   ├── habits/
│   ├── routines/
│   ├── tasks/
│   └── pomodoro/
├── domain/               # Business logic (KMP-ready)
├── data/                 # Repositories, Room database
├── core/                 # Shared utilities, UI components
└── system/               # Android integrations (notifications, widgets)
```

---

## 🔐 Security & Privacy

- ✅ **Local-First:** All data stored on device (no cloud in MVP)
- ✅ **Encrypted Settings:** Proto DataStore + Google Tink (AES-256-GCM)
- ✅ **Secure Keys:** Android Keystore (hardware-backed)
- ✅ **No Account Required:** Zero personal data collection in MVP
- ✅ **GDPR-Ready:** Export/delete data anytime

Full details: [docs/04-SECURITY-PRIVACY.md](./docs/04-SECURITY-PRIVACY.md)

---

## 🧪 Testing

**Test Pyramid:** 70% Unit / 20% Integration / 10% E2E

- **Unit Tests:** JUnit 5 + Mockk (Use Cases, ViewModels)
- **Integration Tests:** AndroidJUnit4 + Room in-memory (Repository + DAO)
- **UI Tests:** Compose Test (components, screens)
- **E2E Tests:** Maestro (critical user flows)

**Coverage Targets:**
- Domain Layer: 90%+
- Data Layer: 80%+
- Presentation: 60%+

Full details: [docs/05-TESTING-STRATEGY.md](./docs/05-TESTING-STRATEGY.md)

---

## 🚧 Development Roadmap

### MVP Timeline: 8-12 Weeks

| Phase | Duration | Focus |
|-------|----------|-------|
| **Phase 1: Foundation** | Weeks 1-2 | Project setup, database, design system, CI/CD |
| **Phase 2: Habits** | Weeks 3-4 | CRUD, calendar views, notifications, streaks |
| **Phase 3: Tasks & Routines** | Weeks 5-6 | Task management, routine creation |
| **Phase 4: Pomodoro & Integration** | Week 7 | Timer, unified home screen |
| **Phase 5: Polish** | Weeks 8-10 | Widgets, statistics, E2E testing, optimization |
| **Phase 6: Release** | Weeks 11-12 | Beta testing, bug fixes, Play Store launch |

**Target:** MVP v1.0 release in Q2 2026

---

## 🎨 Design System

**Material Design 3 Expressive (2026)**

The absolute latest evolution of Material Design, introduced May 2025 with Android 16.

**Key Features:**
- 🎨 **Expressive Shapes:** Morphing stars, organic forms, physics-based motion
- 🌈 **Dynamic Color:** Wallpaper-derived color schemes
- ⚡ **Spring Animations:** Weight-aware, bouncy micro-interactions
- 📱 **Adaptive Layouts:** Hero images, split-pane views

**Implementation:** Jetpack Compose Material 3 library v1.4.0+

---

## 🔀 Git Workflow

### Branching Strategy

```
main (protected) ── Production releases only
  │
  └── dev (primary) ── All feature branches merge here
        ├── feature/habits-crud
        ├── feature/task-management
        └── bugfix/notification-crash
```

**Rules:**
- ✅ `dev` is the primary development branch
- ✅ Create feature branches from `dev`
- ✅ All merges via Pull Request (no direct commits)
- ✅ Merge to `main` only for releases
- ❌ **Never merge without user approval**

---

## 📋 Next Steps

1. **Review Documentation:** Read [Project Overview](./docs/00-PROJECT-OVERVIEW.md) for complete context
2. **Resolve Open Questions:** See [PRD Section 9](./docs/01-PRODUCT-REQUIREMENTS.md#9-open-questions)
3. **Initialize Repository:** Create Git repo with `dev` branch
4. **Project Setup:** Multi-module Gradle structure
5. **Sprint 1:** Database schema + Design system implementation

---

## 📞 Project Info

**Status:** Planning Complete ✅  
**Target Platform:** Android 8.0+ (API 26+)  
**Development Model:** Agile sprints (2-week iterations)  
**Repository:** `C:\Development\Projects\Habitao`

---

## 🤝 Contributing

This is a personal project in planning phase. Implementation will begin after documentation review and approval.

---

## 📄 License

To be determined.

---

**Built with ❤️ using Kotlin Multiplatform + Jetpack Compose**
