plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.glasscrane.flannery"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.glasscrane.flannery"
        minSdk = 29
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
    }

    signingConfigs {
        // A fixed key, so every build can install over the last one. Android
        // refuses to update an app whose signature changed, and CI runners have
        // no debug keystore — they generate a random one per run, which made
        // every build refuse to update the one before it.
        create("sideload") {
            storeFile = rootProject.file("flannery.keystore")
            storePassword = "flannery"
            keyAlias = "flannery"
            keyPassword = "flannery"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sideload")
        }
        debug {
            // keeps a debug build installable alongside the real one
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
