plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// Core Common module: shared utilities, Result wrapper, extensions.
// Uses android-library plugin for proper AAR packaging with AGP 8.13+.
// Source remains Android-free for future KMP migration.

android {
    namespace = "com.habitao.core.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs +=
            listOf(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.stdlib)

    // Coroutines (core only - no Android)
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DateTime (KMP-compatible)
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(libs.bundles.testing.unit)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
