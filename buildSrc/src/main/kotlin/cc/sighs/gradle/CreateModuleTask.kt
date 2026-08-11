package cc.sighs.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class CreateModuleTask : DefaultTask() {

    @set:Option(option = "moduleName", description = "Module name (lowercase, e.g., 'network')")
    @get:Input
    var moduleName: String = ""

    @set:Option(option = "moduleDisplayName", description = "Human-readable module display name (e.g., 'Network Module')")
    @get:Input
    @get:Optional
    var moduleDisplayName: String = ""

    @set:Option(option = "classNamePrefix", description = "Class name prefix for the main entry point class (e.g., 'ExamplemodNetwork')")
    @get:Input
    @get:Optional
    var classNamePrefix: String = ""

    @set:Option(option = "rootPackage", description = "Java package for the module (e.g., 'com.example.examplemod.network')")
    @get:Input
    @get:Optional
    var rootPackage: String = ""

    @get:Internal
    var parentModId: String = ""

    @TaskAction
    fun create() {
        require(moduleName.isNotBlank()) {
            "moduleName is required. Usage: gradlew createModule --moduleName=xxx [--classNamePrefix=Xxx] [--moduleDisplayName=\"Xxx Module\"] [--rootPackage=com.example.xxx]"
        }
        require(moduleName.matches(Regex("^[a-z][a-z0-9_]*$"))) {
            "moduleName must be lowercase alphanumeric/underscore, starting with a letter. Got: $moduleName"
        }

        if (moduleDisplayName.isBlank()) {
            moduleDisplayName = moduleName.replaceFirstChar { it.uppercase() } + " Module"
        }

        if (parentModId.isBlank()) {
            parentModId = project.findProperty("mod_id") as String? ?: "examplemod"
        }

        if (classNamePrefix.isBlank()) {
            classNamePrefix = parentModId.replaceFirstChar { it.uppercase() } + moduleName.replaceFirstChar { it.uppercase() }
        }

        if (rootPackage.isBlank()) {
            rootPackage = "${project.group}.${moduleName}"
        }

        val parentModName = project.findProperty("mod_name") as String? ?: parentModId
        val fullModId = "${parentModId}_${moduleName}"
        val fullModName = "$parentModName ${moduleName.replaceFirstChar { it.uppercase() }}"

        val packagePath = rootPackage.replace('.', '/')

        val templateDir = project.rootProject.file("modules/_template")
        require(templateDir.exists()) {
            "Template directory not found: ${templateDir.absolutePath}"
        }

        val targetDir = project.rootProject.file("modules/${moduleName}")
        require(!targetDir.exists()) {
            "Module directory already exists: ${targetDir.absolutePath}"
        }

        // Copy template
        project.copy {
            from(templateDir)
            into(targetDir)
        }

        // Create module-level gradle.properties (loaded by settings.gradle.kts)
        val modulePropsFile = File(targetDir, "gradle.properties")
        modulePropsFile.writeText("""
            mod_id=${moduleName}
            mod_name=${moduleDisplayName}
            description=${moduleDisplayName} for ${parentModId}
        """.trimIndent() + "\n")

        val tokens = mapOf(
            "{{MODULE_NAME}}" to moduleName,
            "{{CLASS_PREFIX}}" to classNamePrefix,
            "{{MOD_NAME}}" to moduleDisplayName,
            "{{MOD_DESCRIPTION}}" to "${moduleDisplayName} for ${parentModId}",
            "{{PACKAGE}}" to rootPackage,
            "{{PACKAGE_PATH}}" to packagePath,
            "{{FULL_MOD_ID}}" to fullModId,
            "{{FULL_MOD_NAME}}" to fullModName,
            "{{PARENT_MOD_ID}}" to parentModId
        )

        // Step 1: Rename top-level module directories
        listOf("common", "fabric", "forge").forEach { loader ->
            val oldDir = File(targetDir, "{{MODULE_NAME}}-${loader}")
            val newDir = File(targetDir, "${moduleName}-${loader}")
            if (oldDir.exists()) {
                oldDir.renameTo(newDir)
            }
        }

        // Step 2: Replace tokens in file contents
        targetDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                var content = file.readText()
                var modified = false
                for ((token, replacement) in tokens) {
                    if (content.contains(token)) {
                        content = content.replace(token, replacement)
                        modified = true
                    }
                }
                if (modified) {
                    file.writeText(content)
                }
            }
        }

        // Step 3: Rename files containing tokens
        targetDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                var newName = file.name
                for ((token, replacement) in tokens) {
                    if (newName.contains(token)) {
                        newName = newName.replace(token, replacement)
                    }
                }
                if (newName != file.name) {
                    val newFile = File(file.parentFile, newName)
                    file.renameTo(newFile)
                }
            }
        }

        // Step 4a: Collect all __PACKAGE__ placeholder directories
        val packageDirs = mutableListOf<File>()
        targetDir.walkTopDown().forEach { dir ->
            if (dir.isDirectory && dir.name == "__PACKAGE__") {
                packageDirs.add(dir)
            }
        }

        // Step 4b: Move files from each __PACKAGE__ to the real package path
        for (placeholderDir in packageDirs) {
            val realPackageDir = File(placeholderDir.parentFile, packagePath)
            realPackageDir.mkdirs()
            placeholderDir.walkTopDown().forEach { f ->
                val relative = placeholderDir.toPath().relativize(f.toPath()).toString()
                val dest = File(realPackageDir, relative)
                if (f.isDirectory) {
                    dest.mkdirs()
                } else {
                    dest.parentFile.mkdirs()
                    f.renameTo(dest)
                }
            }
            placeholderDir.deleteRecursively()
        }

        val relPath = "modules/$moduleName"
        println("")
        println("Module '${moduleName}' created successfully!")
        println("  Directory:    $relPath/")
        println("  Package:      $rootPackage")
        println("  Main class:   $classNamePrefix")
        println("  Mod ID:       $fullModId")
        println("  Mod Name:     $fullModName")
        println("")
        println("Created subprojects:")
        println("  :${relPath.replace('/', ':')}:${moduleName}-common")
        println("  :${relPath.replace('/', ':')}:${moduleName}-fabric")
        println("  :${relPath.replace('/', ':')}:${moduleName}-forge")
        println("")
        println("Refresh Gradle to auto-detect the new module (IDE sync or gradlew --refresh-dependencies)")
        println("Build standalone: gradlew :${relPath.replace('/', ':')}:${moduleName}-fabric:build")
    }
}
