allprojects {
    group = "io.github.r4t2.nilum"
    version = "0.3.3"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

// Merges per-loader jars into single distributable jars for players. Deferred until all
// projects are evaluated, since nilum-fabric/nilum-neoforge/nilum-paper register their own
// archive tasks in their own build scripts. Fabric's contribution must always be added first
// in each merge: its MANIFEST.MF carries Loom/Mixin attributes (Fabric-Loom-Mixin-Remap-Type,
// Fabric-Mapping-Namespace) that Fabric Loader needs at runtime, and DuplicatesStrategy.EXCLUDE
// keeps whichever MANIFEST.MF was added first, discarding NeoForge's/Paper's near-empty ones.
gradle.projectsEvaluated {
    val fabricJar = project(":nilum-fabric").tasks.named<AbstractArchiveTask>("remapJar")
    val neoforgeJar = project(":nilum-neoforge").tasks.named<AbstractArchiveTask>("jar")
    val paperJar = project(":nilum-paper").tasks.named<AbstractArchiveTask>("shadowJar")
    val fabricClientOnlyJar = project(":nilum-fabric").tasks.named<AbstractArchiveTask>("clientOnlyJar")
    val neoforgeClientOnlyJar = project(":nilum-neoforge").tasks.named<AbstractArchiveTask>("clientOnlyJar")

    // Fabric mod, NeoForge mod, and Paper plugin are three independent loading mechanisms
    // reading three differently-named manifest files (fabric.mod.json, META-INF/neoforge.mods.toml,
    // plugin.yml) out of the same jar, so one file works no matter which loader it's dropped into.
    tasks.register<Zip>("mergeUniversalJar") {
        group = "build"
        description = "Merges the Fabric, NeoForge, and Paper jars into one everything-included jar."

        dependsOn(fabricJar, neoforgeJar, paperJar)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(zipTree(fabricJar.flatMap { it.archiveFile }))
        from(zipTree(neoforgeJar.flatMap { it.archiveFile }))
        from(zipTree(paperJar.flatMap { it.archiveFile }))

        archiveBaseName.set("nilum-universal")
        archiveVersion.set(project.version.toString())
        archiveExtension.set("jar")
        archiveClassifier.set("")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
    }

    // Client-only equivalent: Fabric and NeoForge's stripped clientOnlyJar output merged, no
    // Paper (server-only, nothing to strip down to a client with).
    tasks.register<Zip>("mergeMultiloaderJar") {
        group = "build"
        description = "Merges the Fabric and NeoForge client-only jars into one distributable jar."

        dependsOn(fabricClientOnlyJar, neoforgeClientOnlyJar)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(zipTree(fabricClientOnlyJar.flatMap { it.archiveFile }))
        from(zipTree(neoforgeClientOnlyJar.flatMap { it.archiveFile }))

        archiveBaseName.set("nilum-multiloader")
        archiveVersion.set(project.version.toString())
        archiveExtension.set("jar")
        archiveClassifier.set("")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
    }
}
