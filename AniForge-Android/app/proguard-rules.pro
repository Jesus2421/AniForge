# AniForge ProGuard Rules

# Mantener modelos de datos (Gson los necesita por reflection)
-keep class com.aniforge.model.** { *; }

# Mantener clases de la API (Jsoup reflection)
-keep class com.aniforge.api.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep public class org.jsoup.** { *; }

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
