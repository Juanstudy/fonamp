# fonamp release ProGuard/R8 rules (release-only; debug has minify off).
# Each rule below exists because R8 full-mode would otherwise break a
# reflective/serialization boundary. No global -dontobfuscate, no blanket
# -ignorewarnings.

# --- Media3 / ExoPlayer: playback + MediaSession service in release ---
# Media3 wires renderers, extractors and the session service via reflection/
# ServiceLoader; stripping them breaks local + radio playback.
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# --- Room (core:database): entities/DAOs accessed via generated code ---
# FonampDatabase, FavoriteStation, ThemePref + FavoriteDao/ThemeDao are read
# reflectively by Room's annotation processor output at runtime.
-keep class com.fonamp.core.database.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging.**

# --- kotlinx.serialization (core:network RadioDtos/DirectoryCache) ---
# @Serializable DTOs (station search results, cache envelopes) are
# instantiated by generated Serializers looked up by name at runtime.
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialInfo <fields>;
}
-keepclasseswithmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Hilt / Dagger: entry points generated at compile time ---
# @HiltAndroidApp / @AndroidEntryPoint / @Module classes are referenced from
# generated Hilt components; keep the app's entry points, let the rest shrink.
-keep class com.fonamp.app.FonampApp { *; }
-keep class com.fonamp.app.MainActivity { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# --- Retrofit / OkHttp (core:network RadioBrowserClient) ---
# Retrofit creates implementations of service interfaces at runtime; keep
# interface signatures and generic info so the converter keeps working.
-keep,allowobfuscation,allowshrinking interface retrofit2.http.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# --- Coil (core:ui artwork thumbs) ---
# Coil decodes drawables/bitmaps via ImageLoader factories; keep its public
# API so R8 does not strip decoder constructors used reflectively.
-keep class coil.** { *; }
-dontwarn coil.**
