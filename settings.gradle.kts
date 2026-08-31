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
    "nilum-common-client",
    "nilum-common-server",
    "nilum-neoforge",
    "nilum-fabric",
    "nilum-paper",
    "nilum-api"
)
