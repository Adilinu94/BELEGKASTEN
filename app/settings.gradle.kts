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
rootProject.name = "Belegkasten"
include(":app")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:ocr")
include(":core:embed")
include(":core:search")
include(":core:backup")
include(":core:license")
include(":core:ui")
include(":feature:capture")
include(":feature:receipts")
include(":feature:search")
include(":feature:settings")
