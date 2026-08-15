// Fabric client mod: channel registration, rendering hooks, creative tab injection.
// Targets Minecraft 1.21.11 (see README).

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
}

dependencies {
    implementation(project(":nilum-common"))
    include(project(":nilum-common"))
    implementation(project(":nilum-common-client"))
    include(project(":nilum-common-client"))

    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.19.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")

    // Compile-only, optional at runtime; Iris integration is behind FabricLoader.isModLoaded("iris")
    // checks everywhere it's used, so the game runs fine without Iris installed. Vendored locally
    // rather than pulled from Iris's maven since that coordinate wasn't verified.
    modCompileOnly(files("libs/iris-fabric-1.10.7+mc1.21.11.jar"))
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
