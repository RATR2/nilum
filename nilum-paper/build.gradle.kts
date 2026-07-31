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

// Only for /nilum ver's display - never fed into plugin.yml's version, which
// SemanticVersions compares between client and server during the handshake.
// Reads .git directly rather than shelling out to a git binary, which isn't
// guaranteed to be on PATH in every build environment.
val gitCommit: String = try {
    val gitDir = File(rootDir, ".git")
    val head = File(gitDir, "HEAD").readText().trim()
    val sha = if (head.startsWith("ref:")) {
        val refPath = head.removePrefix("ref:").trim()
        val refFile = File(gitDir, refPath)
        when {
            refFile.exists() -> refFile.readText().trim()
            else -> File(gitDir, "packed-refs").takeIf { it.exists() }
                ?.readLines()
                ?.firstOrNull { it.endsWith(refPath) }
                ?.substringBefore(' ')
                ?: ""
        }
    } else {
        head
    }
    if (sha.length >= 7) sha.substring(0, 7) else "unknown"
} catch (e: Exception) {
    "unknown"
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("commit", gitCommit)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
    filesMatching("nilum-build.properties") {
        expand("commit" to gitCommit)
    }
}
