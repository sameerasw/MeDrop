# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Gson rules
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Ensure @Keep annotations are always honored
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Kotlin Reflect & Domain Models
-keep class kotlin.reflect.** { *; }
-keep class com.sameerasw.medrop.domain.model.** { *; }
-keep class com.sameerasw.medrop.data.model.** { *; }
-keepclassmembers class com.sameerasw.medrop.data.model.** { *; }
-keep class com.sameerasw.medrop.data.repository.** { *; }

# Keep ViewModel constructors for reflection-based instantiation
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Ensure anonymous TypeToken subclasses (used for GSON generic lists) are kept
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    protected <init>(...);
}

# Keep R.string class and fields for runtime reflection lookup
-keep class com.sameerasw.medrop.R$string { *; }
-keepclassmembers class com.sameerasw.medrop.R$string {
    public static <fields>;
}
