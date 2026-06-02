import groovy.util.Node
import groovy.util.NodeList

plugins {
    `java-library`
    `maven-publish`
}

val modId: String = property("mod_id") as String
val modName: String = property("mod_name") as String
val modAuthor: String = property("mod_author") as String
val modVersion: String = property("mod_version") as String
val mcVersion: String = property("minecraft_version") as String
val mcVersionRange: String = property("minecraft_version_range") as String
val mavenGroup: String = property("maven_group") as String
val javaVersion: String = property("java_version") as String
val fabricVersion: String = property("fabric_api_version") as String
val fabricLoaderVersion: String = property("fabric_loader_version") as String
val forgeVersion: String = property("forge_version") as String
val forgeLoaderVersionRange: String = property("forge_loader_version_range") as String
val licenseVal: String = property("license") as String
val creditsVal: String = findProperty("credits") as String? ?: ""

base {
    archivesName = "${modName}-${project.name}-${mcVersion}-${modVersion}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
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
}

listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        capability("${mavenGroup}:${project.name}:${modVersion}")
        capability("${mavenGroup}:${base.archivesName.get()}:${modVersion}")
        capability("${mavenGroup}:${modId}-${project.name}-${mcVersion}:${modVersion}")
        capability("${mavenGroup}:${modId}:${modVersion}")
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_${modName}" }
    }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE.txt")) {
        rename { "${it}_${modName}" }
    }
    manifest {
        attributes(
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
}

tasks.named<ProcessResources>("processResources") {
    val expandProps = mapOf(
        "version" to modVersion,
        "group" to project.group,
        "minecraft_version" to mcVersion,
        "minecraft_version_range" to mcVersionRange,
        "fabric_version" to fabricVersion,
        "fabric_loader_version" to fabricLoaderVersion,
        "mod_name" to modName,
        "mod_author" to modAuthor,
        "mod_id" to modId,
        "license" to licenseVal,
        "description" to (project.description ?: ""),
        "forge_version" to forgeVersion,
        "forge_loader_version_range" to forgeLoaderVersionRange,
        "credits" to creditsVal,
        "java_version" to javaVersion
    )

    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    filesMatching(listOf("META-INF/mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}

extra["mavenDependencyWhitelist"] = emptySet<String>()

fun childText(node: Node, name: String): String {
    for (child in node.children()) {
        if (child is Node) {
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

extensions.configure<PublishingExtension> {
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

    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])

            pom.withXml {
                @Suppress("UNCHECKED_CAST")
                val raw = project.extra["mavenDependencyWhitelist"] as? Iterable<*> ?: emptyList<Any>()
                val whitelist = raw.map { it.toString().trim() }.filter { it.isNotEmpty() }.toSet()
                if (whitelist.isEmpty()) return@withXml

                fun match(g: String, a: String) =
                    whitelist.contains(g) || whitelist.contains(a) || whitelist.contains("$g:$a")

                val xml = asNode()
                val depsRaw = xml.get("dependencies")
                val depsNode = when (depsRaw) {
                    is Node -> depsRaw
                    is NodeList -> depsRaw.find { it is Node } as? Node
                    else -> null
                }

                if (depsNode != null) {
                    val toRemove = depsNode.children()
                        .filterIsInstance<Node>()
                        .filterNot { match(childText(it, "groupId"), childText(it, "artifactId")) }
                    toRemove.forEach { depsNode.remove(it) }
                }

                val declared = linkedMapOf<String, Triple<String, String, String>>()
                for ((cfgName, scope) in listOf(
                    "api" to "compile", "implementation" to "runtime",
                    "runtimeOnly" to "runtime", "compileOnly" to "compile"
                )) {
                    project.configurations.findByName(cfgName)?.dependencies
                        ?.withType(ExternalModuleDependency::class.java)
                        ?.forEach { dep ->
                            val g = dep.group?.trim().orEmpty()
                            val a = dep.name?.trim().orEmpty()
                            val v = dep.version?.trim().orEmpty()
                            if (g.isEmpty() || a.isEmpty() || v.isEmpty() || !match(g, a)) return@forEach
                            val key = "$g:$a"
                            if (!declared.containsKey(key) || scope == "compile") {
                                declared[key] = Triple(g, a, v)
                            }
                        }
                }

                if (declared.isEmpty()) return@withXml

                val depNode = depsNode ?: (xml.appendNode("dependencies") as Node)
                val existingKeys = depNode.children()
                    .filterIsInstance<Node>()
                    .map { "${childText(it, "groupId")}:${childText(it, "artifactId")}" }
                    .toSet()

                for (info in declared.values) {
                    val key = "${info.first}:${info.second}"
                    if (!existingKeys.contains(key)) {
                        depNode.appendNode("dependency").apply {
                            appendNode("groupId", info.first)
                            appendNode("artifactId", info.second)
                            appendNode("version", info.third)
                            appendNode("scope", if (key.startsWith("net.fabricmc")) "compile" else "runtime")
                        }
                    }
                }

                if (depNode.children().isEmpty()) xml.remove(depNode)
            }
        }
    }
}