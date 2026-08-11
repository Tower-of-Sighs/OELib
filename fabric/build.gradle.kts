plugins {
    id("multiloader-loader")
    id("fabric-loom")
}

val modId: String = property("mod_id") as String
val mcVersion: String = property("minecraft_version") as String
val fabricLoaderVer: String = property("fabric_loader_version") as String
val fabricApiVer: String = property("fabric_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String
// The final JiJ stays self-contained, while these module dependencies remain
// available transitively for Loom's development-time remapping and compilation.
// Platform dependencies such as Fabric Loader/API are intentionally omitted.
extra["mavenDependencyWhitelist"] = listOf("cc.sighs.oelib")

repositories {
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMc}:${parchmentVer}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVer}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVer}")
    // Jar-in-Jar: embed all module Fabric subprojects
    @Suppress("UNCHECKED_CAST")
    val discoveredModules = rootProject.extra["discoveredModules"] as? Map<String, File> ?: emptyMap()
    for ((name, _) in discoveredModules) {
        val path = ":modules:${name}:${name}-fabric"
        try {
            val targetProject = project(path)

            include(targetProject)
            // Loom strips nested jars from mod dependencies in development.
            // Expose each embedded module on the API graph so Loom can remap it
            // for both the java-api compile classpath and the runtime classpath.
            api(targetProject)
        } catch (_: UnknownProjectException) {
        }
    }
}

loom {
    val aw = project(":common").file("src/main/resources/${modId}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    mixin {
        defaultRefmapName = "${modId}.refmap.json"
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

// Register each module fabric source set as a mod so AW/mixin are processed in dev
project.afterEvaluate {
    @Suppress("UNCHECKED_CAST")
    val discoveredModules = rootProject.extra["discoveredModules"] as? Map<String, File> ?: emptyMap()
    discoveredModules.forEach { (name, _) ->
        try {
            val proj = project(":modules:$name:${name}-fabric")
            project.evaluationDependsOn(proj.path)
            loom.mods.register("oelib_$name") {
                sourceSet(proj.sourceSets.main.get())
            }
        } catch (_: Exception) {
            // Skip if module not available or not fabric
        }
    }
}
