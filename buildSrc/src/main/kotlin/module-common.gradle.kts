import cc.sighs.gradle.ModuleDependenciesExtension
import cc.sighs.gradle.configureStandardArtifactMetadata

plugins {
    `java-library`
    `maven-publish`
}

ModuleDependenciesExtension.init(project)
extensions.add("moduleDependencies", ModuleDependenciesExtension(project))

val modId: String = extra["mod_id"] as String
val modName: String = extra["mod_name"] as String

// Common boilerplate deps for all module types
project.dependencies.add("compileOnly", "org.spongepowered:mixin:0.8.5")
project.dependencies.add("compileOnly", "io.github.llamalad7:mixinextras-common:0.3.5")
project.dependencies.add("annotationProcessor", "io.github.llamalad7:mixinextras-common:0.3.5")
val modAuthor: String = property("mod_author") as String
val mcVersion: String = property("minecraft_version") as String
val mcVersionRange: String = property("minecraft_version_range") as String
val javaVersion: String = property("java_version") as String
val fabricVersion: String = property("fabric_version") as String
val fabricLoaderVersion: String = property("fabric_loader_version") as String
val neoforge_version: String by project
val neoforge_loader_version_range: String by project
val licenseVal: String = property("license") as String
val creditsVal: String = findProperty("credits") as String? ?: ""

base {
    archivesName = "${project.name}-${mcVersion}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
//    withJavadocJar()
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }

    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
    maven {
        url = uri("https://maven.sighs.cc/repository/maven-releases/")
    }
    maven {
        url = uri("https://maven.sighs.cc/repository/maven-snapshots/")
    }
}

listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        // Project dependencies select the project-name capability while external
        // consumers select the published Maven coordinate. Since artifactId adds
        // the Minecraft version, both capabilities must be present.
        capability("${project.group}:${project.name}:${project.version}")
        capability("${project.group}:${base.archivesName.get()}:${project.version}")
    }
}

// The published dependency model is intentionally authored in the Maven POM
// below. Loom and ModDev add build-only dependencies to the Java variants that
// must not leak to consumers, and pom.withXml cannot also rewrite .module files.
tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata>().configureEach {
    enabled = false
}

configureStandardArtifactMetadata(
    modId = modId,
    modName = modName,
    modAuthor = modAuthor,
    mcVersion = mcVersion,
    mcVersionRange = mcVersionRange,
    fabricVersion = fabricVersion,
    fabricLoaderVersion = fabricLoaderVersion,
    license = licenseVal,
    neoforgeVersion = neoforge_version,
    neoforgeLoaderVersionRange = neoforge_loader_version_range,
    credits = creditsVal,
    javaVersion = javaVersion
)

// Extract text from a POM node's child element, handling namespace prefixes
fun childText(node: groovy.util.Node, name: String): String {
    for (child in node.children()) {
        if (child is groovy.util.Node) {
            val cn = child.name().toString()
            if (cn.endsWith("}$name") || cn == name) {
                val v = child.value()
                return when (v) {
                    is String -> v.trim()
                    is Iterable<*> -> v.joinToString("")
                    else -> v?.toString()?.trim() ?: ""
                }
            }
        }
    }
    return ""
}

// Maven dependency whitelist for POM filtering. Empty means that no external
// dependencies are published; explicit moduleDependencies are added separately.
extra["mavenDependencyWhitelist"] = emptyList<String>()

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])

            pom.withXml {
                val xml = asNode()
                val depsNode = xml.children().filterIsInstance<groovy.util.Node>()
                    .find { it.name().toString().endsWith("}dependencies") || it.name().toString() == "dependencies" }
                    ?: (xml.appendNode("dependencies") as groovy.util.Node)

                // 1) Keep only explicitly whitelisted external dependencies.
                @Suppress("UNCHECKED_CAST")
                val raw = project.extra.properties["mavenDependencyWhitelist"]
                val whitelist = (raw as? Iterable<*>)?.map { it.toString().trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
                fun match(g: String, a: String) =
                    whitelist.contains(g) || whitelist.contains(a) || whitelist.contains("$g:$a")

                val toRemove = depsNode.children().filterIsInstance<groovy.util.Node>()
                    .filterNot { match(childText(it, "groupId"), childText(it, "artifactId")) }
                toRemove.forEach { depsNode.remove(it) }

                val declared = linkedMapOf<String, Triple<String, String, String>>()
                for ((cfgName, scope) in listOf(
                    "api" to "compile", "implementation" to "runtime",
                    "runtimeOnly" to "runtime", "compileOnly" to "compile"
                )) {
                    project.configurations.findByName(cfgName)?.dependencies
                        ?.withType(org.gradle.api.artifacts.ExternalModuleDependency::class.java)
                        ?.forEach { dep ->
                            val g = dep.group?.trim() ?: ""
                            val a = dep.name?.trim() ?: ""
                            val v = dep.version?.toString()?.trim() ?: ""
                            if (g.isEmpty() || a.isEmpty() || v.isEmpty() || !match(g, a)) return@forEach
                            val key = "$g:$a"
                            if (!declared.containsKey(key) || scope == "compile") {
                                declared[key] = Triple(g, a, v)
                            }
                        }
                }

                val existingKeys = depsNode.children().filterIsInstance<groovy.util.Node>()
                    .map { "${childText(it, "groupId")}:${childText(it, "artifactId")}" }.toSet()
                for (info in declared.values) {
                    val key = "${info.first}:${info.second}"
                    if (!existingKeys.contains(key)) {
                        depsNode.appendNode("dependency").apply {
                            appendNode("groupId", info.first)
                            appendNode("artifactId", info.second)
                            appendNode("version", info.third)
                            appendNode("scope", if (key.startsWith("net.fabricmc")) "compile" else "runtime")
                        }
                    }
                }

                // 2) Always add moduleDependencies (no filtering, skip duplicates)
                val suffix = ModuleDependenciesExtension.getSuffix(project.name)
                val existingArtifacts = depsNode.children().filterIsInstance<groovy.util.Node>()
                    .map { "${childText(it, "groupId")}:${childText(it, "artifactId")}" }.toSet()
                for (dep in ModuleDependenciesExtension.getApiDeps(project)) {
                    val depProj = project(":modules:$dep:$dep-$suffix")
                    val depArtifactId = depProj.extensions
                        .getByType(org.gradle.api.plugins.BasePluginExtension::class.java)
                        .archivesName.get()
                    val coord = "${depProj.group}:$depArtifactId"
                    if (!existingArtifacts.contains(coord)) {
                        depsNode.appendNode("dependency").apply {
                            appendNode("groupId", depProj.group)
                            appendNode("artifactId", depArtifactId)
                            appendNode("version", depProj.version)
                            appendNode("scope", "compile")
                        }
                    }
                }

                if (depsNode.children().isEmpty()) xml.remove(depsNode)
            }
        }
    }
    repositories {
        val localMavenUrl = providers.environmentVariable("local_maven_url").orNull
        if (!localMavenUrl.isNullOrBlank()) {
            maven {
                url = uri(localMavenUrl)
            }
        }

        val modVersion = project.version.toString().ifBlank { "unknown" }
        val isSnapshot = modVersion.contains("snapshot", ignoreCase = true)
        val publishUrl = if (isSnapshot) {
            "https://maven.sighs.cc/repository/maven-snapshots/"
        } else {
            "https://maven.sighs.cc/repository/maven-releases/"
        }

        maven {
            name = "remoteRepo"
            url = uri(publishUrl)
            credentials {
                username = providers.environmentVariable("SIGHS_PUBLISH_USER").orNull
                password = providers.environmentVariable("SIGHS_PUBLISH_PASSWORD").orNull
            }
        }
    }
}
