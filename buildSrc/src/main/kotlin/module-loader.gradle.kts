import cc.sighs.gradle.ModuleDependenciesExtension
import cc.sighs.gradle.SharedDependenciesExtension

plugins {
    id("module-common")
}

val mavenGroup: String = property("group") as String
val modId: String = extra["mod_id"] as String

// Shared dependencies DSL — add loader-specific deps for all modules
// Usage: sharedDependencies { fabric("com.terraformersmc:modmenu:7.2.2") }
fun sharedDependencies(action: SharedDependenciesExtension.() -> Unit) {
    SharedDependenciesExtension(project).action()
}

val projectName = project.name
val commonProjectName = when {
    projectName.endsWith("-fabric") -> projectName.removeSuffix("-fabric") + "-common"
    projectName.endsWith("-forge") -> projectName.removeSuffix("-forge") + "-common"
    projectName.endsWith("-neoforge") -> projectName.removeSuffix("-neoforge") + "-common"
    else -> throw GradleException("Module loader project name must end with -fabric, -forge or -neoforge, got: $projectName")
}

val parentPath = project.path.substringBeforeLast(":")
val commonProjectPath = "${parentPath}:${commonProjectName}"
val modmenuVersion = property("modmenu_version") as String

configurations {
    register("commonJava") {
        isCanBeResolved = true
    }
    register("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project(commonProjectPath))
    add("commonJava", project(commonProjectPath, "commonJava"))
    add("commonResources", project(commonProjectPath, "commonResources"))
}

sharedDependencies {
    fabric("com.terraformersmc:modmenu:${modmenuVersion}")
    fabric("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    fabric("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
}

// Forward module deps declared in -common via moduleDependencies { api() / implementation() }
val suffix = ModuleDependenciesExtension.getSuffix(projectName)
project.evaluationDependsOn(commonProjectPath)
val commonProj = project(commonProjectPath)
val apiDeps = ModuleDependenciesExtension.getApiDeps(commonProj)
val implDeps = ModuleDependenciesExtension.getImplDeps(commonProj)
// Use sourceSet output as lazy file deps to bypass Loom's mod/remap processing.
// Provider lambda resolves at execution time when all projects are evaluated.
if (apiDeps.isNotEmpty() || implDeps.isNotEmpty()) {
    for (dep in apiDeps) {
        val depPath = ":modules:$dep:$dep-$suffix"
        val depProj = project(depPath)
        val output = project.files(project.provider { depProj.sourceSets.main.get().output })
        project.dependencies.add("api", output)
        // Also add to Loom's mod runtime classpath for dev environment
        project.afterEvaluate {
            if (project.configurations.findByName("modRuntimeClasspath") != null) {
                project.dependencies.add("modRuntimeClasspath", output)
            }
        }
    }
    for (dep in implDeps) {
        val depPath = ":modules:$dep:$dep-$suffix"
        val depProj = project(depPath)
        val output = project.files(project.provider { depProj.sourceSets.main.get().output })
        project.dependencies.add("implementation", output)
        project.afterEvaluate {
            if (project.configurations.findByName("modRuntimeClasspath") != null) {
                project.dependencies.add("modRuntimeClasspath", output)
            }
        }
    }
}
// Copy dep names to current project so module-common.gradle.kts POM generation picks them up
if (apiDeps.isNotEmpty() || implDeps.isNotEmpty()) {
    project.extra["_moduleDepsApi"] = apiDeps
    project.extra["_moduleDepsImpl"] = implDeps

    // Forward deps from dependency modules (config-name -> current-module config)
    val forwardBuffer = mutableListOf<Pair<String, String>>()
    for (dep in apiDeps + implDeps) {
        val depPath = ":modules:$dep:$dep-$suffix"
        val depProj = try { project(depPath) } catch (_: Exception) { continue }
        fun collect(srcConfig: String, dstConfig: String = srcConfig) {
            depProj.configurations.findByName(srcConfig)?.dependencies
                ?.withType(org.gradle.api.artifacts.ExternalModuleDependency::class.java)
                ?.forEach { ed ->
                    val g = ed.group ?: return@forEach; val a = ed.name ?: return@forEach; val v = ed.version ?: return@forEach
                    forwardBuffer.add(dstConfig to "$g:$a:$v")
                }
        }
        collect("api", "api"); collect("modApi", "api"); collect("additionalRuntimeClasspath")
    }
    // Add forwarded deps in afterEvaluate so MDG/Loom configs exist
    if (forwardBuffer.isNotEmpty()) {
        project.afterEvaluate {
            for ((cfg, notation) in forwardBuffer) {
                if (project.configurations.findByName(cfg) != null) {
                    project.dependencies.add(cfg, notation)
                }
            }
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(configurations.getByName("commonResources"))
    from(configurations.getByName("commonResources"))
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.getByName("commonJava"))
    from(configurations.getByName("commonJava"))
    dependsOn(configurations.getByName("commonResources"))
    from(configurations.getByName("commonResources"))
}
