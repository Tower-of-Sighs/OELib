pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Sponge Snapshots"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "OELib"
include("common")
include("fabric")
include("forge")

// Auto-discover modules in the modules/ directory
val modulesDir = file("modules")
val moduleDirs: List<File> = if (modulesDir.exists()) {
    modulesDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith("_") } ?: emptyList()
} else {
    emptyList()
}
if (modulesDir.exists()) {
    moduleDirs.forEach { moduleDir ->
        val moduleName = moduleDir.name
        listOf("common", "fabric", "forge").forEach { loader ->
            val subprojectDir = File(moduleDir, "${moduleName}-${loader}")
            if (subprojectDir.exists()) {
                include("modules:${moduleName}:${moduleName}-${loader}")
            }
        }
    }
}

// Load per-module properties from modules/<name>/gradle.properties
// These are applied to subprojects by root build.gradle.kts
fun loadModuleProps(moduleName: String): Map<String, String> {
    val props = java.util.Properties()
    val propsFile = file("modules/$moduleName/gradle.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { props.load(it) }
    }
    return props.mapKeys { it.key.toString() }.mapValues { it.value.toString() }
}

val moduleProps = mutableMapOf<String, Map<String, String>>()
if (modulesDir.exists()) {
    moduleDirs.forEach { moduleDir ->
        val name = moduleDir.name
        moduleProps[name] = loadModuleProps(name)
    }
}
gradle.extra["moduleProps"] = moduleProps
gradle.extra["moduleDirs"] = moduleDirs
