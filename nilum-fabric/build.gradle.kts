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
    implementation(project(":nilum-common-server"))
    include(project(":nilum-common-server"))

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

// Client-only jar: takes the already-remapped full jar and strips out dedicated-server hosting
// code (NilumFabricDedicatedServer never gets any static reference from NilumFabricMod/Client,
// Fabric Loader only looks it up by name via the "server" entrypoint key, so removing the class
// and that key is enough) plus the embedded nilum-common-server nested jar.
val clientOnlyModJson by tasks.registering {
    val remapJarTask = tasks.named<AbstractArchiveTask>("remapJar")
    dependsOn(remapJarTask)
    inputs.file(remapJarTask.flatMap { it.archiveFile })
    val outputFile = layout.buildDirectory.file("generated/clientOnly/fabric.mod.json")
    outputs.file(outputFile)
    doLast {
        val jarFile = remapJarTask.get().archiveFile.get().asFile
        val jsonText = zipTree(jarFile).matching { include("fabric.mod.json") }.singleFile.readText()
        @Suppress("UNCHECKED_CAST")
        val json = groovy.json.JsonSlurper().parseText(jsonText) as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val entrypoints = json["entrypoints"] as MutableMap<String, Any?>
        entrypoints.remove("server")
        @Suppress("UNCHECKED_CAST")
        val jars = json["jars"] as MutableList<Map<String, Any?>>
        jars.removeIf { (it["file"] as String).contains("nilum-common-server") }
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(groovy.json.JsonOutput.toJson(json))
    }
}

// A plain Jar task writes its own bare manifest by default; without copying remapJar's,
// Fabric-Loom-Mixin-Remap-Type/Fabric-Mapping-Namespace go missing and mixins stop remapping
// correctly on this jar. Manifest.from() needs a real manifest-format file, not a zip to pull
// one entry out of, so extract it first.
val clientOnlyManifest by tasks.registering {
    val remapJarTask = tasks.named<AbstractArchiveTask>("remapJar")
    dependsOn(remapJarTask)
    inputs.file(remapJarTask.flatMap { it.archiveFile })
    val outputFile = layout.buildDirectory.file("generated/clientOnly/MANIFEST.MF")
    outputs.file(outputFile)
    doLast {
        val jarFile = remapJarTask.get().archiveFile.get().asFile
        val manifestFile = zipTree(jarFile).matching { include("META-INF/MANIFEST.MF") }.singleFile
        manifestFile.copyTo(outputFile.get().asFile, overwrite = true)
    }
}

val clientOnlyJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Client-only jar with dedicated-server hosting code and nilum-common-server stripped out."
    val remapJarTask = tasks.named<AbstractArchiveTask>("remapJar")
    dependsOn(remapJarTask, clientOnlyModJson, clientOnlyManifest)
    archiveClassifier.set("client-only")
    manifest.from(clientOnlyManifest.map { it.outputs.files.singleFile })
    from({ zipTree(remapJarTask.get().archiveFile.get().asFile) }) {
        exclude("io/github/r4t2/nilum/fabric/NilumFabricDedicatedServer*.class")
        exclude("io/github/r4t2/nilum/fabric/handshake/FabricServerHandshake*.class")
        exclude("io/github/r4t2/nilum/fabric/server/**")
        exclude("META-INF/jars/nilum-common-server-*.jar")
        exclude("fabric.mod.json")
    }
    from(clientOnlyModJson.map { it.outputs.files.singleFile })
}
