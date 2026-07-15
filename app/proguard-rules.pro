# Keep model/DTO classes used by Gson serialization
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / Gson
-dontwarn retrofit2.**
-keep class com.esn.fitdiet.data.remote.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
