plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.toyota32k"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.toyota32k"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res")
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
    implementation(libs.coreKtx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintLayout)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.lifecycleRuntimeKtx)
    implementation(libs.lifecycleLiveDataKtx)
    implementation(libs.lifecycleViewModelKtx)
    implementation(libs.lifecycleViewModelSavedState)

    implementation(libs.android.utilities)
    implementation(libs.android.binding)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxkotlin)

    implementation(project(":dialog"))
    implementation(libs.androidx.activity)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.espressoCore)
}