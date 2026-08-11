import cc.sighs.gradle.configureJarJarFilenameNormalization
import cc.sighs.gradle.registerGeneratedResources

plugins {
    id("module-loader")
    id("net.neoforged.moddev.legacyforge")
}

val modId: String = extra["mod_id"] as String
val mcVersion: String = property("minecraft_version") as String
val forgeVer: String = property("forge_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

mixin {
    config("${modId}.mixins.json")
    config("${modId}.forge.mixins.json")
}

legacyForge {
    version = "${mcVersion}-${forgeVer}"

    val commonProject = project(":modules:dev:dev-common")
    val at = commonProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    parchment {
        minecraftVersion = parchmentMc
        mappingsVersion = parchmentVer
    }

    runs {
        create("client") {
            client()
        }
        create("data") {
            data()
            programArguments.addAll(
                "--mod",
                modId,
                "--all",
                "--output",
                project.file("src/generated/resources/").absolutePath,
                "--existing",
                project.file("src/main/resources/").absolutePath
            )
        }
        create("server") {
            server()
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

registerGeneratedResources()

dependencies {
}

configureJarJarFilenameNormalization()
