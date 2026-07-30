pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
    }
}

rootProject.name = "nilum"

include(
    "nilum-common",
    "nilum-neoforge",
    "nilum-fabric",
    "nilum-paper"
)
