import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Cargar GEMINI_API_KEY desde local.properties (findProperty no la lee por defecto)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val geminiKey = localProperties.getProperty("GEMINI_API_KEY", "")
    .ifEmpty { (project.findProperty("GEMINI_API_KEY") as String?) ?: "" }

// Signing config para release. Lee credenciales desde local.properties o variables de entorno.
// Si no están configuradas, el release NO se firma con la key de debug (lo que impediría
// publicar en Google Play). En su lugar, queda sin firmar y Gradle avisa.
fun secret(key: String): String? =
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }

val releaseStorePath = secret("RELEASE_KEYSTORE_PATH")
val releaseStorePassword = secret("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = secret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD")
val releaseSigningReady = releaseStorePath != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null &&
    rootProject.file(releaseStorePath).exists()

android {
    namespace = "com.virtualworld.easyexpensecontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.virtualworld.easyexpensecontrol"
        minSdk = 24
        targetSdk = 35
        versionCode = 100200200
        versionName = "1.2.2"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "⚠️  Release signing config no encontrada. " +
                        "Define RELEASE_KEYSTORE_PATH, RELEASE_KEYSTORE_PASSWORD, " +
                        "RELEASE_KEY_ALIAS y RELEASE_KEY_PASSWORD en local.properties o " +
                        "variables de entorno. El AAB/APK release saldrá SIN FIRMAR."
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime){
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.androidx.room.ktx){
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.androidx.room.compiler){
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.kotlinx.datetime)
    implementation(libs.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation(libs.junit.jupiter)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.material)
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")

    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
}