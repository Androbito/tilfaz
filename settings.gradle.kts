pluginManagement {
    includeBuild("build-logic")
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
        // Mirrors Maven Central; a second route when Central is slow to answer.
        gradlePluginPortal()
    }
}

rootProject.name = "tilfaz"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


include(":app")
include(":core:model")
include(":core:domain")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
include(":feature:onboarding")
include(":feature:home")
include(":feature:live")
include(":feature:vod")
include(":feature:series")
include(":feature:player")
include(":feature:settings")
include(":feature:library")
