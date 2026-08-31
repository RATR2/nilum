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
    implementation(project(":nilum-common-server"))
    jarJar(project(":nilum-common-server"))

    // Compile-only, optional at runtime; Iris integration is behind ModList.get().isLoaded("iris")
    compileOnly(files("libs/iris-neoforge-1.10.7+mc1.21.11.jar"))
}

// Client-only jar: takes the already-built full jar and strips out dedicated-server hosting code
// (NilumNeoForgeDedicatedServer only ever gets a lazily-resolved reference from a dist-guarded
// branch never taken on a client) plus the embedded nilum-common-server jarJar entry.
val clientOnlyJarJarMetadata by tasks.registering {
    val jarTask = tasks.named<Jar>("jar")
    dependsOn(jarTask)
    val outputFile = layout.buildDirectory.file("generated/clientOnly/metadata.json")
    outputs.file(outputFile)
    doLast {
        val jarFile = jarTask.get().archiveFile.get().asFile
        val jsonText = zipTree(jarFile).matching { include("META-INF/jarjar/metadata.json") }.singleFile.readText()
        @Suppress("UNCHECKED_CAST")
        val json = groovy.json.JsonSlurper().parseText(jsonText) as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val jars = json["jars"] as MutableList<Map<String, Any?>>
        jars.removeIf { entry ->
            @Suppress("UNCHECKED_CAST")
            val identifier = entry["identifier"] as Map<String, Any?>
            identifier["artifact"] == "nilum-common-server"
        }
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)))
    }
}

val clientOnlyJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Client-only jar with dedicated-server hosting code and nilum-common-server stripped out."
    val jarTask = tasks.named<Jar>("jar")
    dependsOn(jarTask, clientOnlyJarJarMetadata)
    archiveClassifier.set("client-only")
    from({ zipTree(jarTask.get().archiveFile.get().asFile) }) {
        exclude("io/github/r4t2/nilum/neoforge/NilumNeoForgeDedicatedServer*.class")
        exclude("io/github/r4t2/nilum/neoforge/handshake/NeoForgeServerHandshake*.class")
        exclude("io/github/r4t2/nilum/neoforge/server/**")
        exclude("META-INF/jarjar/io.github.r4t2.nilum.nilum-common-server-*.jar")
        exclude("META-INF/jarjar/metadata.json")
    }
    from(clientOnlyJarJarMetadata.map { it.outputs.files.singleFile }) {
        into("META-INF/jarjar")
    }
}
