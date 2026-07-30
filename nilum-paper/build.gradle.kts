// Paper/Bukkit server plugin: plugin channel messaging, TCP asset server, custom block registry,
// anti-cheat integration, Open API. Also hosts the Skript and Denizen integrations as addon
// packages rather than separate modules. Targets Minecraft 1.21.11 (see README).

plugins {
    id("com.gradleup.shadow") version "9.6.0"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":nilum-common"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

// shadowJar is the real output (no classifier); the plain jar task's output is renamed
// so it doesn't collide with it in build/libs.
tasks.jar {
    archiveClassifier.set("slim")
}
tasks.shadowJar {
    archiveClassifier.set("")
}
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
