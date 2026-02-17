plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Domain module is a pure Kotlin library with NO Android dependencies
// This makes it KMP-ready for future iOS support

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    // Core Module (shared utilities)
    implementation(projects.core.common)

    // Kotlin
    implementation(libs.kotlin.stdlib)

    // Coroutines (core only - no Android)
    implementation(libs.kotlinx.coroutines.core)

    // Serialization (for data classes)
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
