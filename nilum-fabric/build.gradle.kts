// Fabric client mod: channel registration, rendering hooks, creative tab injection.
// Targets Minecraft 1.21.11 (see README).

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
}

dependencies {
    implementation(project(":nilum-common"))
    include(project(":nilum-common"))

    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.19.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
