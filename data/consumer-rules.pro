# Data module consumer ProGuard rules
# These rules are applied to consumers of this library

# Keep Room entities and DAOs
-keep class com.habitao.data.local.database.entity.** { *; }
-keep class com.habitao.data.local.database.dao.** { *; }

# Keep repository implementations
-keep class com.habitao.data.repository.** { *; }

# Proto DataStore
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
