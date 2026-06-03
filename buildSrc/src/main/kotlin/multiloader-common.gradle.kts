import cc.sighs.gradle.ModuleDependenciesExtension

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
}

listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        capability("${project.group}:${project.name}:${project.version}")
        capability("${project.group}:${base.archivesName.get()}:${project.version}")
        capability("${project.group}:${modId}-${project.name}-${mcVersion}:${project.version}")
        capability("${project.group}:${modId}:${project.version}")
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${modName}" }
    }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) {
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
        "version" to project.version,
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
        "neoforge_version" to neoforge_version,
        "neoforge_loader_version_range" to neoforge_loader_version_range,
        "credits" to creditsVal,
        "java_version" to javaVersion
    )

    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    filesMatching(listOf("META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}

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

// Maven dependency whitelist for POM filtering
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

                // 1) Whitelist filtering (only when non-empty)
                @Suppress("UNCHECKED_CAST")
                val raw = project.extra["mavenDependencyWhitelist"] as? Iterable<*> ?: emptyList<Any>()
                val whitelist = raw.map { it.toString().trim() }.filter { it.isNotEmpty() }.toSet()
                if (whitelist.isNotEmpty()) {
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
                        val coord = "${depProj.group}:${depProj.name}"
                        if (!existingArtifacts.contains(coord)) {
                            depsNode.appendNode("dependency").apply {
                                appendNode("groupId", depProj.group)
                                appendNode("artifactId", depProj.name)
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
