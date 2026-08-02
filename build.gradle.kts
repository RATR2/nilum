allprojects {
    group = "io.github.r4t2.nilum"
    version = "0.1.2-SNAPSHOT"

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

// Merges the Fabric and NeoForge client jars into one distributable jar for players.
// Deferred until all projects are evaluated, since nilum-fabric/nilum-neoforge register
// their remapJar/jar tasks in their own build scripts.
gradle.projectsEvaluated {
    val fabricClientJar = project(":nilum-fabric").tasks.named<AbstractArchiveTask>("remapJar")
    val neoforgeClientJar = project(":nilum-neoforge").tasks.named<AbstractArchiveTask>("jar")

    tasks.register<Zip>("mergeClientJars") {
        group = "build"
        description = "Merges the Fabric and NeoForge client jars into one distributable jar."

        dependsOn(fabricClientJar, neoforgeClientJar)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(zipTree(fabricClientJar.flatMap { it.archiveFile }))
        from(zipTree(neoforgeClientJar.flatMap { it.archiveFile }))

        archiveBaseName.set("nilum-client")
        archiveVersion.set(project.version.toString())
        archiveExtension.set("jar")
        archiveClassifier.set("")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
    }
}
