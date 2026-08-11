import cc.sighs.gradle.ModuleDependenciesExtension
import cc.sighs.gradle.configureStandardArtifactMetadata

plugins {
    `java-library`
    `maven-publish`
}

val modId: String = property("mod_id") as String
val modName: String = property("mod_name") as String
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
    archivesName = "${modId}-${project.name}-${mcVersion}"
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
        name = "SighsReleases"
        url = uri("https://maven.sighs.cc/repository/maven-releases/")
        mavenContent { releasesOnly() }
    }
    maven {
        name = "SighsSnapshots"
        url = uri("https://maven.sighs.cc/repository/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
}

listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        capability("${project.group}:${project.name}:${project.version}")
        capability("${project.group}:${base.archivesName.get()}:${project.version}")
        capability("${project.group}:${modId}-${project.name}-${mcVersion}:${project.version}")
        capability("${project.group}:${modId}:${project.version}")
    }
}

// Publish the curated Maven POM as the single external dependency model. The
// Java variants contain Loom/ModDev build dependencies and embedded module
// projects which must not be exposed transitively to consumers.
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

// Maven dependency whitelist for POM filtering. Empty means no external
// dependencies are published.
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
                val raw = project.extra["mavenDependencyWhitelist"] as? Iterable<*> ?: emptyList<Any>()
                val whitelist = raw.map { it.toString().trim() }.filter { it.isNotEmpty() }.toSet()
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
                        ?.withType(ExternalModuleDependency::class.java)
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

                for (info in declared.values) {
                    val key = "${info.first}:${info.second}"
                    depsNode.appendNode("dependency").apply {
                        appendNode("groupId", info.first)
                        appendNode("artifactId", info.second)
                        appendNode("version", info.third)
                        appendNode("scope", if (key.startsWith("net.fabricmc")) "compile" else "runtime")
                    }
                }

                // 2) Always add moduleDependencies (no filtering, skip duplicates)
                @Suppress("UNCHECKED_CAST")
                val moduleApiDeps = project.extra.properties["_moduleDepsApi"] as? List<String> ?: emptyList()
                if (moduleApiDeps.isNotEmpty()) {
                    val suffix = ModuleDependenciesExtension.getSuffix(project.name)
                    val existingArtifacts = depsNode.children().filterIsInstance<groovy.util.Node>()
                        .map { "${childText(it, "groupId")}:${childText(it, "artifactId")}" }.toSet()
                    for (dep in moduleApiDeps) {
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
