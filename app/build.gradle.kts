plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // ✅ add this plugin
}
android {
    namespace = "com.example.fittrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fittrack"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        //noinspection DataBindingWithoutKapt
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.androidx.fragment.ktx)
    // CameraX latest stable versions (for Compose + Fragment)
    //noinspection UseTomlInstead
    implementation (libs.androidx.camera.extensions)


// Guava for ListenableFuture (if still needed)
    implementation (libs.guava)


    // Google Sign-in
    implementation(libs.play.services.auth)

    // Google Fit (manual latest version — for SleepStage etc)
    implementation (libs.play.services.fitness)
    implementation(libs.androidx.connect.client)
    implementation (libs.lecho.hellocharts.library)





    // WorkManager
    implementation (libs.androidx.work.runtime.ktx)
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.material)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.androidx.activity)  // latest stable
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
