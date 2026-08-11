import cc.sighs.gradle.CreateModuleTask

plugins {
    id("fabric-loom") version "1.9-SNAPSHOT" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.77" apply false
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

// Reuse the module directory snapshot collected during settings evaluation.
// Keeping one snapshot avoids repeated filesystem scans while preserving the
// current discovery order and the set of directories selected by settings.
@Suppress("UNCHECKED_CAST")
val moduleDirs: List<File> = gradle.extra["moduleDirs"] as? List<File> ?: emptyList()
val discoveredModules = moduleDirs.associateBy { it.name }
extra["discoveredModules"] = discoveredModules

// Convenience task: create a new module from template
val modId: String = property("mod_id") as String
tasks.register<CreateModuleTask>("createModule") {
    parentModId = modId
}
