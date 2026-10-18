pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://repo2.maven.org/maven2") }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            // the OSS licenses plugin has no plugin marker artifact, so map its id to the module
            if (requested.id.id == "com.google.android.gms.oss-licenses-plugin") {
                useModule("com.google.android.gms:oss-licenses-plugin:0.13.0")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://repo2.maven.org/maven2") }
    }
}

rootProject.name = "eiskalt"
include(":app")
