package cc.sighs.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

/**
 * Shared implementation for the small pieces of convention that used to be
 * copied into every module build script.
 *
 * The helpers deliberately keep the call sites in the leaf scripts.  That
 * preserves the point at which the current scripts register their
 * configurations and task actions while putting the implementation in one
 * place.
 */
fun Project.configureCommonSourceArtifacts() {
    configurations.register("commonJava").configure {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    configurations.register("commonResources").configure {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

    val java = extensions.getByType(JavaPluginExtension::class.java)
    val main = java.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    artifacts.add("commonJava", main.java.sourceDirectories.singleFile)
    artifacts.add("commonResources", main.resources.sourceDirectories.singleFile)
}

/**
 * Registers the generated resource directory used by ModDev data generation.
 *
 * This is intentionally limited to the Java source-set API.  The surrounding
 * `legacyForge {}` block remains in each platform script because its DSL types
 * are supplied by the external ModDevGradle plugin and are not available to
 * buildSrc without introducing a second plugin classpath.
 */
fun Project.registerGeneratedResources(directory: String = "src/generated/resources") {
    val java = extensions.getByType(JavaPluginExtension::class.java)
    java.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).resources.srcDir(directory)
}

/**
 * ModDev currently prefixes JarJar entries with their group.  Keep the
 * existing output normalization in one implementation; callers still invoke
 * this at the same point in their script as the former inline task action.
 */
fun Project.configureJarJarFilenameNormalization() {
    tasks.named("jar", Jar::class.java).configure {
        doFirst {
            val jarJarDirectory = project.layout.buildDirectory
                .dir("generated/jarJar/META-INF/jarjar")
                .get()
                .asFile
            if (!jarJarDirectory.exists()) return@doFirst

            val metadataFile = File(jarJarDirectory, "metadata.json")
            if (!metadataFile.exists()) return@doFirst

            val metadata = metadataFile.readText()
            val artifactPattern = Regex("\"artifact\"\\s*:\\s*\"([^\"]+)\"")
            val pathPattern = Regex("\"path\"\\s*:\\s*\"([^\"]+)\"")
            val artifacts = artifactPattern.findAll(metadata)
                .map { it.groupValues[1] }
                .toList()
            val paths = pathPattern.findAll(metadata)
                .map { it.groupValues[1] }
                .toList()

            val renames = linkedMapOf<String, String>()
            for (index in artifacts.indices) {
                val path = paths.getOrNull(index) ?: continue
                val oldFileName = path.substringAfterLast("/")
                val marker = "${artifacts[index]}-"
                val markerIndex = oldFileName.indexOf(marker)
                if (markerIndex > 0) {
                    renames[oldFileName] = oldFileName.substring(markerIndex)
                }
            }

            for ((oldFileName, newFileName) in renames) {
                File(jarJarDirectory, oldFileName)
                    .renameTo(File(jarJarDirectory, newFileName))
            }

            if (renames.isNotEmpty()) {
                var rewrittenMetadata = metadata
                for ((oldFileName, newFileName) in renames) {
                    rewrittenMetadata = rewrittenMetadata.replace(oldFileName, newFileName)
                }
                metadataFile.writeText(rewrittenMetadata)
            }
        }
    }
}

/**
 * Configures the metadata that is identical for the root and module
 * publications.  The caller still decides where this is placed relative to
 * its capability and publication setup.
 */
fun Project.configureStandardArtifactMetadata(
    modId: String,
    modName: String,
    modAuthor: String,
    mcVersion: String,
    mcVersionRange: String,
    fabricVersion: String,
    fabricLoaderVersion: String,
    license: String,
    forgeVersion: String,
    forgeLoaderVersionRange: String,
    credits: String,
    javaVersion: String
) {
    tasks.named("sourcesJar", Jar::class.java).configure {
        from(rootProject.file("LICENSE.txt")) {
            rename { fileName -> "${fileName}_${modName}" }
        }
    }

    tasks.named("jar", Jar::class.java).configure {
        from(rootProject.file("LICENSE.txt")) {
            rename { fileName -> "${fileName}_${modName}" }
        }
        manifest.attributes(
            mapOf(
                "Specification-Title" to modName,
                "Specification-Vendor" to modAuthor,
                "Specification-Version" to project.version,
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to modAuthor,
                "Built-On-Minecraft" to mcVersion
            )
        )
    }

    val expandProps = mapOf(
        "version" to project.version,
        "group" to project.group,
        "minecraft_version" to mcVersion,
        "minecraft_version_range" to mcVersionRange,
        "fabric_version" to fabricVersion,
        "fabric_loader_version" to fabricLoaderVersion,
        "mod_name" to modName,
        "mod_author" to modAuthor,
        "mod_id" to modId,
        "license" to license,
        "description" to (project.description ?: ""),
        "forge_version" to forgeVersion,
        "forge_loader_version_range" to forgeLoaderVersionRange,
        "credits" to credits,
        "java_version" to javaVersion
    )
    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    tasks.named("processResources", ProcessResources::class.java).configure {
        filesMatching(listOf("META-INF/mods.toml")) {
            expand(expandProps)
        }
        filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
            expand(jsonExpandProps)
        }
        inputs.properties(expandProps)
    }
}
