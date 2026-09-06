# Adhan library rules
-keep class com.batoulapps.adhan.** { *; }

# Keep models
-keep class com.atf.akbartime.data.** { *; }

# General ProGuard rules
-dontwarn java.time.**
-keepattributes Signature
-keepattributes *Annotation*
