import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = "com.github.toyota-m2k"
version = "1.0"

configure<LibraryExtension> {
    namespace = "io.github.toyota32k.dialog"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.coreKtx)
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.lifecycleLiveDataKtx)
    implementation(libs.lifecycleViewModelKtx)
    implementation(libs.lifecycleRuntimeKtx)
    implementation(libs.lifecycleViewModelSavedState)

    implementation(libs.kotlinReflect)

    api(libs.android.utilities)
    implementation(libs.android.binding)
    implementation(libs.android.viewex)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espressoCore)
}

// ./gradlew publishToMavenLocal
publishing {
    publications {
        // Creates a Maven publication called "release".
        register<MavenPublication>("release") {
            // You can then customize attributes of the publication as shown below.
            groupId = "com.github.toyota-m2k"
            artifactId = "android-dialog"
            version = project.findProperty("githubReleaseTag") as String? ?: "LOCAL"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/toyota-m2k/android-dialog")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
