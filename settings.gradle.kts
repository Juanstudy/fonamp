pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "fonamp"
include(
    ":app",
    ":core:player",
    ":core:network",
    ":core:database",
    ":core:permissions",
    ":core:ui",
    ":provider:api",
    ":provider:radio",
    ":provider:local",
    ":feature:library",
    ":feature:radio",
    ":feature:settings",
)
