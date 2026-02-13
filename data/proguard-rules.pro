# Data module ProGuard rules
# Add project specific ProGuard rules here.

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
