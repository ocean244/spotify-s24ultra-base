import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val storeFileProp = (project.findProperty("android.injected.signing.store.file") as? String)
    ?: System.getenv("ANDROID_KEYSTORE_PATH")
    ?: ""
val storePassProp = (project.findProperty("android.injected.signing.store.password") as? String)
    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
    ?: ""
val keyAliasProp = (project.findProperty("android.injected.signing.key.alias") as? String)
    ?: System.getenv("ANDROID_KEY_ALIAS")
    ?: ""
val keyPassProp = (project.findProperty("android.injected.signing.key.password") as? String)
    ?: System.getenv("ANDROID_KEY_PASSWORD")
    ?: ""

val hasReleaseSigning = storeFileProp.isNotBlank() &&
    File(storeFileProp).exists() &&
    storePassProp.isNotBlank() &&
    keyAliasProp.isNotBlank() &&
    keyPassProp.isNotBlank()

android {
    namespace = "com.s24ultra.spotifybase"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.s24ultra.spotifybase"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-s24ultra"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = File(storeFileProp)
                storePassword = storePassProp
                keyAlias = keyAliasProp
                keyPassword = keyPassProp
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
