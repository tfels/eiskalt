import java.util.Properties
import java.io.FileInputStream

val isGitHub = System.getenv("CI") == "true"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "de.felsernet.android.eiskalt"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.felsernet.android.eiskalt"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "0.2.4"

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
    buildFeatures {
        viewBinding = true
    }

    lint {
        sarifReport = true
    }
}

dependencies {
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:${libs.versions.firebaseBom.get()}"))

    // Firebase dependencies (versions managed by BoM)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)

    // Other dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.recyclerview.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.app.update.ktx)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
