// Top-level build file for Habitao
// Clean Architecture Multi-Module Android Project

plugins {
    // Android plugins (applied to subprojects)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    
    // Kotlin plugins
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    
    // KSP for annotation processing
    alias(libs.plugins.ksp) apply false
    
    // Dependency Injection
    alias(libs.plugins.hilt) apply false
    
    // Database
    alias(libs.plugins.room) apply false
    
    // Proto DataStore
    alias(libs.plugins.protobuf) apply false
    
    // Code Quality
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Configure Detekt for code quality
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
}

// Configure Ktlint for code formatting
ktlint {
    version.set("1.2.1")
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Common configuration for all subprojects
subprojects {
    // Apply Ktlint to all subprojects
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    
    // Configure Ktlint for subprojects
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.2.1")
        android.set(true)
        outputToConsole.set(true)
    }
}

// Task to clean all build directories
tasks.register("cleanAll", Delete::class) {
    delete(rootProject.layout.buildDirectory)
    subprojects.forEach { project ->
        delete(project.layout.buildDirectory)
    }
}
