plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val modId: String = property("mod_id") as String
val mcVersion: String = property("minecraft_version") as String
val neoVer: String = property("neoforge_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

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

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
}

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
            implementation(targetProject)
        } catch (_: UnknownProjectException) {
        }
    }
}

tasks.named<Jar>("jar") {

    manifest {
    }
    // Strip MDG's group. prefix from embedded JAR filenames in jarJar output
    // (MDG hardcodes {group}.{filename} for project deps; we want clean names like Fabric)
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
        for ((oldName, newName) in renames) {
            File(jarjarDir, oldName).renameTo(File(jarjarDir, newName))
        }
        if (renames.isNotEmpty()) {
            var newMeta = meta
            for ((oldName, newName) in renames) newMeta = newMeta.replace(oldName, newName)
            metaFile.writeText(newMeta)
        }
    }
}
