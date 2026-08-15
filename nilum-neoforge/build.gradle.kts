// NeoForge client mod: channel registration, rendering hooks, creative tab injection.
// Targets Minecraft 1.21.11 (see README). Classic Forge is legacy from 1.20.2 onward and
// isn't binary-compatible with mods built for this era, so NeoForge is the tier-3 loader.

plugins {
    id("net.neoforged.moddev") version "2.0.143"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

val generateModMetadata by tasks.registering(Copy::class) {
    inputs.property("version", project.version)
    expand("version" to project.version)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}
sourceSets.main.get().resources.srcDir(generateModMetadata)

neoForge {
    version = "21.11.45"
    ideSyncTask(generateModMetadata)
}

dependencies {
    implementation(project(":nilum-common"))
    jarJar(project(":nilum-common"))
    implementation(project(":nilum-common-client"))
    jarJar(project(":nilum-common-client"))

    // Compile-only, optional at runtime; Iris integration is behind ModList.get().isLoaded("iris")
    compileOnly(files("libs/iris-neoforge-1.10.7+mc1.21.11.jar"))
}
