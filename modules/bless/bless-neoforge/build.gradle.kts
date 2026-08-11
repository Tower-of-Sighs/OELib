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

    val commonProject = project(":modules:bless:bless-common")
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

extra["mavenDependencyWhitelist"] = listOf("cn.6tail:lunar")
dependencies {
    jarJar("cn.6tail:lunar:1.7.3")
    api("cn.6tail:lunar:1.7.3")
    "additionalRuntimeClasspath"("cn.6tail:lunar:1.7.3")
}

configureJarJarFilenameNormalization()
