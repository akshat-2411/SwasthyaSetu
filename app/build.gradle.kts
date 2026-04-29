plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)

    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.swasthyasetu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.swasthyasetu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // ---------- AndroidX ----------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ---------- Material ----------
    implementation(libs.material)

    // ---------- Lifecycle ----------
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ---------- UI ----------
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.gridlayout)

    // ---------- Google Maps / Location / Places ----------
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // ---------- Firebase (BOM controls versions) ----------
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // ---------- Coroutines ----------
    implementation(libs.coroutines.play.services)

    // ---------- Google Identity ----------
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)

    // ---------- Testing ----------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // Google Maps Utility Library (Heatmaps, Clustering)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.android.libraries.places:places:x.x.x")
    // Room Persistence Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // Swipe Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

//    implementation("com.google.ai.client:generativeai:0.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.mlkit:text-recognition:16.0.0")

    dependencies {
        // HTTP client - for Groq API calls
        implementation("com.squareup.okhttp3:okhttp:4.12.0")

        // JSON parsing - for building/reading API request & response
        implementation("org.json:json:20240303")

        // Coroutines - for async API calls
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }
}