# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# jaudiotagger (tag reading/writing) — uses reflection in several ID3 paths
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-keepclassmembers class org.jaudiotagger.** {
    <init>(...);
    <fields>;
}
-keep class org.jaudiotagger.tag.images.** { *; }
-dontnote org.jaudiotagger.**

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.spimp3.app.**$$serializer { *; }
-keepclassmembers class com.spimp3.app.** { *** Companion; }
-keepclasseswithmembers class com.spimp3.app.** { kotlinx.serialization.KSerializer serializer(...); }

# ---- Hardening: repackers get an unreadable, flattened mess ----
# Everything not pinned above is flattened into one package with dictionary
# names, so a decompiler shows hundreds of classes like p.a.b(Qx0vtr9) instead
# of self-documenting paths such as com.spimp3.app.ui.screens.RootScreen.
-repackageclasses 'p'
-allowaccessmodification
-obfuscationdictionary r8-dict.txt
-classobfuscationdictionary r8-dict.txt

# Strip verbose logging from release bytecode.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Never rename what the OS resolves by name (belt-and-braces; AAPT rules also
# keep manifest components).
-keep class com.spimp3.app.MainActivity { <init>(); }
-keep class com.spimp3.app.playback.PlaybackService { <init>(); }
