<<<<<<< HEAD
    pluginManagement {
=======
pluginManagement {
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven ("https://jitpack.io")
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }
}

rootProject.name = "da"
include(":app")
 