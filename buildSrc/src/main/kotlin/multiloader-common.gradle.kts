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

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(System.getenv("local_maven_url") ?: "")
        }
    }
}
