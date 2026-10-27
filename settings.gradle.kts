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
        maven(url = "https://raw.githubusercontent.com/VBotDevTeam/VBotPhoneSDKAndroid-Public/main/")
    }
}

rootProject.name = "VBotPhone-Android-Example"
include(":app")
