import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load local.properties (gitignored) for secrets baked into BuildConfig
// at compile time — same pattern as Config.swift on the iOS side, just
// Android's equivalent mechanism.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun secret(name: String): String =
    localProperties.getProperty(name) ?: System.getenv(name) ?: ""

android {
    namespace = "com.jaysframes.framecraftassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jaysframes.framecraftassistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"${secret("PICOVOICE_ACCESS_KEY")}\"")
        buildConfigField("String", "BACKEND_URL", "\"${secret("BACKEND_URL")}\"")
        buildConfigField("String", "ASSISTANT_API_KEY", "\"${secret("ASSISTANT_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // On-device wake-word detection.
    implementation("ai.picovoice:porcupine-android:3.0.3")

    // HTTP to the FrameCraft backend.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
}
