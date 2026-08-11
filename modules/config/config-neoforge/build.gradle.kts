import cc.sighs.gradle.configureJarJarFilenameNormalization
import cc.sighs.gradle.registerGeneratedResources

plugins {
    id("module-loader")
    id("net.neoforged.moddev")
}

val modId: String = extra["mod_id"] as String
val neoVer: String = property("neoforge_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

neoForge {

    val commonProject = project(":modules:config:config-common")
    val at = commonProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    version = neoVer
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

// These libraries are embedded below with JarJar. Do not also publish them as
// runtime dependencies, otherwise ModLauncher sees both the external JAR and
// the embedded JAR as separate JPMS modules with the same module name.
extra["mavenDependencyWhitelist"] = emptyList<String>()
dependencies {
    compileOnlyApi("de.marhali:json5-java:3.0.0")

    jarJar("de.marhali:json5-java:3.0.0")

    "additionalRuntimeClasspath"("de.marhali:json5-java:3.0.0")
}

// Gradle Module Metadata can represent compileOnlyApi correctly: downstream
// projects see compileOnlyApi libraries while compiling OELib's public API,
// but they are absent from runtimeClasspath because JarJar supplies runtime
// copies. The Maven POM cannot faithfully express this distinction.
tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = true
}

configureJarJarFilenameNormalization()
