plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.habitao.core.testing"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Module Dependencies
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.core.common)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.test)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Testing Libraries (exposed as API for other modules to use)
    api(libs.junit5.api)
    api(libs.junit5.params)
    api(libs.mockk)
    api(libs.mockk.android)
    api(libs.turbine)
    api(libs.truth)
    api(libs.androidx.test.core)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.rules)
    api(libs.androidx.arch.core.testing)
    api(libs.hilt.android.testing)
    api(libs.room.testing)
    api(libs.work.testing)
    api(libs.robolectric)

    // JUnit 5 Engine
    runtimeOnly(libs.junit5.engine)
}
