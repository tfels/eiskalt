import java.util.Properties
import java.io.FileInputStream

val isGitHub = System.getenv("CI") == "true"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "de.felsernet.android.eiskalt"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "de.felsernet.android.eiskalt"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (!isGitHub) {
                val releaseSignProperties = Properties()
                releaseSignProperties.load(FileInputStream(file("release-signing.properties")))

                storeFile =     file(releaseSignProperties["storeFile"] as String)
                storePassword = releaseSignProperties["storePassword"] as String
                keyAlias =      releaseSignProperties["keyAlias"] as String
                keyPassword =   releaseSignProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true  // Enable code shrinking for release
            isShrinkResources = true  // Shrink resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // we don't have signing credential on github, so build unsigned
            if (isGitHub) {
                println("⚠️ WARNING: Running on GitHub — release signing is DISABLED.")
            } else {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
