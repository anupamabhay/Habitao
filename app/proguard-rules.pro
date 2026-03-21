# =============================================================================
# Habitao ProGuard Rules
# =============================================================================

# General Android optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Keep application class
-keep class com.habitao.app.HabitaoApplication { *; }

# =============================================================================
# Kotlin
# =============================================================================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# =============================================================================
# =============================================================================

# =============================================================================
# Room
# =============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# =============================================================================
# Kotlinx Serialization
# =============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# =============================================================================
# Jetpack Navigation
# =============================================================================
-keep class androidx.navigation.** { *; }
# Keep serializable route classes for type-safe navigation
-keep class com.habitao.app.*Route { *; }
-keep class com.habitao.app.*Route$Companion { *; }

# =============================================================================
# Compose
# =============================================================================
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# =============================================================================
# Coil
# =============================================================================
-dontwarn coil.**
-keep class coil.** { *; }

# =============================================================================
# Vico Charts
# =============================================================================
-keep class com.patrykandpatrick.vico.** { *; }

# =============================================================================
# Glance Widgets
# =============================================================================
-keep class androidx.glance.** { *; }

# =============================================================================
# Google Tink (Encryption)
# =============================================================================
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# =============================================================================
# DataStore Proto
# =============================================================================
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# =============================================================================
# Debugging (Remove in production)
# =============================================================================
# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
