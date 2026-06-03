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

# Excel import (fastexcel usa APIs JDK/XML no presentes en Android)
-dontwarn org.dhatim.**

# R8: clases opcionales de JDK (ver app/build/outputs/mapping/release/missing_rules.txt)
-dontwarn java.sql.JDBCType
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.ElementVisitor
-dontwarn javax.lang.model.element.ExecutableElement
-dontwarn javax.lang.model.element.Name
-dontwarn javax.lang.model.element.PackageElement
-dontwarn javax.lang.model.element.TypeElement
-dontwarn javax.lang.model.element.TypeParameterElement
-dontwarn javax.lang.model.element.VariableElement
-dontwarn javax.lang.model.type.ArrayType
-dontwarn javax.lang.model.type.DeclaredType
-dontwarn javax.lang.model.type.ExecutableType
-dontwarn javax.lang.model.type.TypeKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVariable
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.ElementFilter
-dontwarn javax.lang.model.util.SimpleElementVisitor8
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn javax.lang.model.util.Types
-dontwarn javax.xml.stream.XMLEventFactory
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.XMLOutputFactory
-dontwarn javax.xml.stream.XMLReporter
-dontwarn javax.xml.stream.XMLResolver
-dontwarn javax.xml.stream.util.XMLEventAllocator
