plugins {
    id("module-loader")
    id("net.neoforged.moddev")
}

val modId: String = extra["mod_id"] as String
val mcVersion: String = property("minecraft_version") as String
val neoVer: String = property("neoforge_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

neoForge {

    val commonProject = project(":modules:{{MODULE_NAME}}:{{MODULE_NAME}}-common")
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

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
}

dependencies {
}

tasks.named<Jar>("jar") {

    // Strip MDG's group. prefix from embedded JAR filenames in jarJar output
    doFirst {
        val jarjarDir = project.layout.buildDirectory.dir("generated/jarJar/META-INF/jarjar").get().asFile
        if (!jarjarDir.exists()) return@doFirst
        val metaFile = File(jarjarDir, "metadata.json")
        if (!metaFile.exists()) return@doFirst
        val meta = metaFile.readText()
        val artifactR = Regex(""""artifact"\s*:\s*"([^"]+)"""")
        val pathR = Regex(""""path"\s*:\s*"([^"]+)"""")
        val artifacts = artifactR.findAll(meta).map { it.groupValues[1] }.toList()
        val paths = pathR.findAll(meta).map { it.groupValues[1] }.toList()
        val renames = linkedMapOf<String, String>()
        for (idx in artifacts.indices) {
            val path = paths.getOrNull(idx) ?: continue
            val oldFile = path.substringAfterLast("/")
            val marker = "${artifacts[idx]}-"
            val mIdx = oldFile.indexOf(marker)
            if (mIdx > 0) renames[oldFile] = oldFile.substring(mIdx)
        }
        for ((oldName, newName) in renames) File(jarjarDir, oldName).renameTo(File(jarjarDir, newName))
        if (renames.isNotEmpty()) {
            var newMeta = meta
            for ((oldName, newName) in renames) newMeta = newMeta.replace(oldName, newName)
            metaFile.writeText(newMeta)
        }
    }
}
