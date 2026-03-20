plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.habitao.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.habitao.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isDebuggable = true
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // TODO: Configure signing config for release builds
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs +=
            listOf(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkDependencies = true
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Module Dependencies
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.system.notifications)
    implementation(projects.system.alarms)
    implementation(projects.system.work)
    implementation(projects.system.widget)
    implementation(projects.feature.habits)
    implementation(projects.feature.routines)
    implementation(projects.feature.tasks)
    implementation(projects.feature.pomodoro)
    implementation(projects.feature.settings)
    implementation(projects.composeApp)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.bundles.lifecycle)

    // Material Components (for XML themes)
    implementation(libs.material.components)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material3)
    debugImplementation(libs.bundles.compose.debug)

    // Navigation
    implementation(libs.navigation.compose)

    // Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Image Loading
    implementation(libs.coil.compose)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // Testing - Unit
    testImplementation(libs.bundles.testing.unit)
    testRuntimeOnly(libs.junit5.engine)

    // Testing - Android/Instrumentation
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
