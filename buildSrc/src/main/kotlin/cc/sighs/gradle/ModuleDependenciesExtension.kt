package cc.sighs.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

/**
 * moduleDependencies { api(":modules:config") } — cross-layer module dependency DSL.
 *
 * Write in any xxx-common/build.gradle.kts, platform modules inherit automatically.
 * The actual Gradle project dependency is added immediately; platform modules get
 * the forwarded variant via module-loader.gradle.kts.
 */
open class ModuleDependenciesExtension(val project: Project) {
    private val extra: ExtraPropertiesExtension = project.extensions.getByType(ExtraPropertiesExtension::class.java)

    fun api(modulePath: String) {
        val name = normalize(modulePath)
        addToList("_moduleDepsApi", name)
        val suffix = getSuffix(project.name)
        val depPath = ":modules:$name:$name-$suffix"
        project.dependencies.add("api", project.dependencies.project(mapOf("path" to depPath)))
    }

    fun implementation(modulePath: String) {
        val name = normalize(modulePath)
        addToList("_moduleDepsImpl", name)
        val suffix = getSuffix(project.name)
        val depPath = ":modules:$name:$name-$suffix"
        project.dependencies.add("implementation", project.dependencies.project(mapOf("path" to depPath)))
    }

    fun annotationProcessor(modulePath: String) {
        val name = normalize(modulePath)
        val suffix = getSuffix(project.name)
        val depPath = ":modules:$name:$name-$suffix"
        project.dependencies.add("annotationProcessor", project.dependencies.project(mapOf("path" to depPath)))
    }

    companion object {
        fun init(project: Project) {
            val extra = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            extra["_moduleDepsApi"] = mutableListOf<String>()
            extra["_moduleDepsImpl"] = mutableListOf<String>()
        }

        @Suppress("UNCHECKED_CAST")
        fun getApiDeps(project: Project): List<String> {
            val extra = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            return (extra["_moduleDepsApi"] as? List<String>) ?: emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        fun getImplDeps(project: Project): List<String> {
            val extra = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            return (extra["_moduleDepsImpl"] as? List<String>) ?: emptyList()
        }

        fun getSuffix(projectName: String): String = when {
            projectName.endsWith("-fabric") -> "fabric"
            projectName.endsWith("-forge") -> "forge"
            projectName.endsWith("-neoforge") -> "neoforge"
            projectName.endsWith("-common") -> "common"
            else -> throw GradleException("Unknown module suffix for: $projectName")
        }
    }

    private fun normalize(path: String): String = path.split(":").last().removeSuffix("-common")
    private fun addToList(key: String, value: String) {
        @Suppress("UNCHECKED_CAST")
        val list = (extra[key] as? MutableList<String>) ?: mutableListOf()
        list.add(value)
        extra[key] = list
    }
}
