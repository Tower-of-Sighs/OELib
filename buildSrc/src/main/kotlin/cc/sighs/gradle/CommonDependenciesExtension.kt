package cc.sighs.gradle

import org.gradle.api.Project

/**
 * sharedDependencies {
 *     compileOnly("org.spongepowered:mixin:0.8.5")           // all modules
 *     implementation("com.google.code.findbugs:jsr305:3.0.1") // all modules
 *     fabric("com.terraformersmc:modmenu:7.2.2")              // → modImplementation
 *     fabricJava("com.google.code.findbugs:jsr305:3.0.1")    // → implementation
 *     forge("some:dep:1.0")                                   // → implementation
 *     neoforge("cn.6tail:lunar:1.7.3")                       // → implementation
 * }
 *
 * Usage:
 *     fun sharedDependencies(action: SharedDependenciesExtension.() -> Unit) {
 *         SharedDependenciesExtension(project).action()
 *     }
 */
open class SharedDependenciesExtension(val project: Project) {

    /** All modules: adds to compileOnly. */
    fun compileOnly(notation: String) = addNow("compileOnly", notation)

    /** All modules: adds to implementation. */
    fun implementation(notation: String) = addNow("implementation", notation)

    /** Fabric mod: adds to modImplementation. */
    fun fabric(notation: String) = addWhen("-fabric", "modImplementation", notation)

    /** Fabric java library: adds to implementation. */
    fun fabricJava(notation: String) = addWhen("-fabric", "implementation", notation)

    /** Forge: always implementation. */
    fun forge(notation: String) = addWhen("-forge", "implementation", notation)

    /** NeoForge: always implementation. */
    fun neoforge(notation: String) = addWhen("-neoforge", "implementation", notation)

    private fun addNow(config: String, notation: String) {
        project.dependencies.add(config, notation)
    }

    private fun addWhen(suffix: String, config: String, notation: String) {
        if (project.name.endsWith(suffix)) {
            project.afterEvaluate {
                project.dependencies.add(config, notation)
            }
        }
    }
}
