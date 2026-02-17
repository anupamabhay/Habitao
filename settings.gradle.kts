pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Habitao"

// =============================================================================
// Module Structure (Clean Architecture)
// =============================================================================

// App Module (entry point)
include(":app")

// Domain Layer (Pure Kotlin - KMP ready)
include(":domain")

// Data Layer (Room, DataStore, Repositories)
include(":data")

// Core Modules (Shared utilities)
include(":core:common")
include(":core:ui")
include(":core:testing")

// System Modules (Android system integrations)
include(":system:notifications")
include(":system:alarms")
include(":system:work")
include(":system:widget")

// Feature Modules (Presentation layer)
include(":feature:habits")
include(":feature:routines")
include(":feature:tasks")
include(":feature:pomodoro")

// =============================================================================
// Module Build Conventions
// =============================================================================
// Each module should follow these conventions:
//
// :app                    - Android Application (depends on all feature modules)
// :domain                 - Pure Kotlin library (no Android dependencies)
// :data                   - Android Library (Room, DataStore, implements domain)
// :core:common            - Pure Kotlin library (utilities, Result wrapper)
// :core:ui                - Android Library (Compose theme, shared components)
// :core:testing           - Android Library (test utilities, fakes)
// :system:*               - Android Library (system integrations)
// :feature:*              - Android Library (Compose UI, ViewModels)
// =============================================================================

// Enable type-safe project accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
