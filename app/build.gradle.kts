plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)

    //Typesafe Nav
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.example.urbanfinder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.urbanfinder"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    // Core and Lifecycle Dependencies
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom.v20241201))
    implementation(libs.androidx.ui.v160)
    implementation(libs.androidx.ui.graphics.v160)
    implementation(libs.androidx.ui.tooling.preview.v160)
    implementation(libs.androidx.material3.v120alpha06)

    // AndroidX Preferences
    implementation(libs.androidx.preference.ktx)
    implementation(libs.play.services.location)

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20241201))
    androidTestImplementation(libs.androidx.ui.test.junit4.v160)
    debugImplementation(libs.androidx.ui.tooling.v160)
    debugImplementation(libs.androidx.ui.test.manifest.v160)

    // Firebase Dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.auth.ktx)

    // Navigation
    implementation(libs.androidx.navigation.runtime.ktx.v273)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // Accompanist for Swipe Refresh and Pager
    implementation(libs.accompanist.swiperefresh.v0301)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    // OpenStreetMap (OSMDroid)
    implementation(libs.osmdroid.android)
    implementation(libs.osmdroid.wms)

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    //Storing Session Data
    implementation (libs.androidx.datastore.preferences)

    implementation (libs.androidx.material3.v120alpha07)
    implementation (libs.play.services.location)

    implementation(libs.play.services.auth)

    //Video Player
    implementation (libs.androidx.media3.exoplayer)
    implementation (libs.androidx.media3.ui)
}
