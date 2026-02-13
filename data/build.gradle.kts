plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.junit5.android)
}

android {
    namespace = "com.habitao.data"
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

    room {
        schemaDirectory("$projectDir/schemas")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Module Dependencies
    implementation(projects.domain)
    implementation(projects.core.common)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Room Database
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.proto)
    implementation(libs.protobuf.kotlin.lite)

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Security (Encryption)
    implementation(libs.tink.android)

    // Testing - Unit
    testImplementation(libs.bundles.testing.unit)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit5.engine)

    // Testing - Android/Instrumentation
    androidTestImplementation(libs.bundles.testing.android)
}
