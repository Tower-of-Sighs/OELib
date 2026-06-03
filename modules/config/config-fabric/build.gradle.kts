plugins {
    id("module-loader")
    id("fabric-loom")
}

val modId: String = extra["mod_id"] as String
val mcVersion: String = property("minecraft_version") as String
val fabricLoaderVer: String = property("fabric_loader_version") as String
val fabricApiVer: String = property("fabric_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

extra["mavenDependencyWhitelist"] = listOf("de.marhali:json5-java", "com.electronwill.night-config:toml", "com.electronwill.night-config:core", "io.smallrye.classfile:jdk-classfile-backport")
dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMc}:${parchmentVer}@zip")
    })

    api("com.electronwill.night-config:toml:3.8.3")
    api("de.marhali:json5-java:3.0.0")
    api("io.smallrye.classfile:jdk-classfile-backport:26")

    include("com.electronwill.night-config:core:3.8.3")
    include("com.electronwill.night-config:toml:3.8.3")
    include("de.marhali:json5-java:3.0.0")
    include("io.smallrye.classfile:jdk-classfile-backport:26")
}

loom {
    val aw = project(":modules:config:config-common").file("src/main/resources/oelib-config.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibConfig"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibConfig"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
