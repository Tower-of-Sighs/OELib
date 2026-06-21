import cc.sighs.gradle.CreateModuleTask

plugins {
    id("fabric-loom") version "1.9-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.49-beta" apply false
}

// Set version from gradle.properties (mod_version) for all projects
val modVersion: String = property("mod_version") as String
allprojects { version = modVersion }

// Load module properties from gradle.extra (set by settings.gradle.kts)
@Suppress("UNCHECKED_CAST")
val moduleProps: Map<String, Map<String, String>> = gradle.extra["moduleProps"] as? Map<String, Map<String, String>> ?: emptyMap()

// Apply per-module extra properties before subproject build scripts evaluate
subprojects {
    val path = this.path
    if (path.startsWith(":modules:")) {
        val parts = path.split(":")
        if (parts.size >= 3) {
            val moduleName = parts[2]
            moduleProps[moduleName]?.forEach { (key, value) ->
                extra[key] = value
            }
        }
    }
}

// Auto-discover modules for parent JiJ
val discoveredModules = mutableMapOf<String, File>()
if (file("modules").exists()) {
    file("modules").listFiles()?.filter { it.isDirectory && !it.name.startsWith("_") }?.forEach { dir ->
        discoveredModules[dir.name] = dir
    }
}
extra["discoveredModules"] = discoveredModules

// Convenience task: create a new module from template
val modId: String = property("mod_id") as String
tasks.register<CreateModuleTask>("createModule") {
    parentModId = modId
}
