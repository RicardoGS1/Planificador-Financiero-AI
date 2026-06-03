# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin / coroutines
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.virtualworld.easyexpensecontrol.data.model.** { *; }
-keep interface com.virtualworld.easyexpensecontrol.data.local.** { *; }
-keep class com.virtualworld.easyexpensecontrol.data.local.FinancialDatabase { *; }
-keep class com.virtualworld.easyexpensecontrol.data.local.FinancialDatabase$* { *; }
-keep class com.virtualworld.easyexpensecontrol.data.local.FinancialDatabaseCallback { *; }

# Retrofit + Gson
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-keep class com.virtualworld.easyexpensecontrol.data.remote.dto.** { *; }
-keep class com.virtualworld.easyexpensecontrol.domain.model.** { *; }

# Koin
-keep class org.koin.** { *; }
-keep class com.virtualworld.easyexpensecontrol.di.** { *; }

# App
-keep class com.virtualworld.easyexpensecontrol.BuildConfig { *; }

# Firebase / Crashlytics
-keep public class * extends java.lang.Exception

# AdMob + mediación Unity Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.ads.mediation.unity.** { *; }
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.**

# Excel import
-dontwarn org.dhatim.**
