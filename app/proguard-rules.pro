# Keep Room schema and entities
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.blatnox.tvplayer.data.api.dto.** { *; }

# Keep Media3 ExoPlayer
-keep class androidx.media3.** { *; }
