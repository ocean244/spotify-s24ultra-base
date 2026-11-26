import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ksFile = (project.findProperty("RELEASE_STORE_FILE") as? String)
    ?: System.getenv("RELEASE_STORE_FILE")
    ?: ""
val ksStorePass = (project.findProperty("RELEASE_STORE_PASSWORD") as? String)
    ?: System.getenv("RELEASE_STORE_PASSWORD")
    ?: ""
val ksAlias = (project.findProperty("RELEASE_KEY_ALIAS") as? String)
    ?: System.getenv("RELEASE_KEY_ALIAS")
    ?: ""
val ksKeyPass = (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
    ?: System.getenv("RELEASE_KEY_PASSWORD")
    ?: ""

val hasReleaseKeystore = ksFile.isNotBlank() &&
    File(ksFile).exists() &&
    ksStorePass.isNotBlank() &&
    ksAlias.isNotBlank() &&
    ksKeyPass.isNotBlank()

android {
    namespace = "com.s24ultra.spotifybase"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.s24ultra.spotifybase"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0-s24ultra"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = File(ksFile)
                storePassword = ksStorePass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
                enableV1Signing = true
                enableV2Signing = true
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
            if (hasReleaseKeystore) {
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
