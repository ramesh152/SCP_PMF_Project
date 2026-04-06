# Keep project-specific rules here.

# Room generates implementation classes that are instantiated reflectively by
# the library at runtime.
-keep class * extends androidx.room.RoomDatabase { *; }

# ML Kit text recognition entry points should remain available in release builds.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
