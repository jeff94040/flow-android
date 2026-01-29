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
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Specifically required for Risk and Flow SDK dependencies
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.fpregistry.io/releases") }
    }
}

rootProject.name = "Checkout Flow Mobile Android(SDK"
include(":app")
