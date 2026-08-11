import cc.sighs.gradle.configureJarJarFilenameNormalization
import cc.sighs.gradle.registerGeneratedResources

plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val modId: String = property("mod_id") as String
val neoVer: String = property("neoforge_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

// Keep the final JarJar self-contained, but publish the embedded OEL modules as
// compile dependencies as well. ModDev loads JarJar entries at game runtime;
// javac does not treat nested jars as compile-classpath entries.
extra["mavenDependencyWhitelist"] = listOf("cc.sighs.oelib")

neoForge {
    version = neoVer

    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    parchment {
        minecraftVersion = parchmentMc
        mappingsVersion = parchmentVer
    }

    runs {
        create("client") { client() }
        create("data") {
            data()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", project.file("src/generated/resources/").absolutePath,
                "--existing", project.file("src/main/resources/").absolutePath
            )
        }
        create("server") { server() }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

registerGeneratedResources()

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")


    // Jar-in-Jar: embed all module Forge subprojects
    @Suppress("UNCHECKED_CAST")
    val discoveredModules = rootProject.extra["discoveredModules"] as? Map<String, File> ?: emptyMap()
    for ((name, _) in discoveredModules) {
        val path = ":modules:${name}:${name}-neoforge"
        try {
            val targetProject = project(path)

            jarJar(targetProject)
            api(targetProject)
        } catch (_: UnknownProjectException) {
        }
    }
}

tasks.named<Jar>("jar") {
    manifest {
    }
}

configureJarJarFilenameNormalization()
