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

extra["mavenDependencyWhitelist"] = listOf("cn.6tail:lunar")
dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMc}:${parchmentVer}@zip")
    })

    api("cn.6tail:lunar:1.7.3")

    include("cn.6tail:lunar:1.7.3")
}

loom {
    val aw = project(":modules:bless:bless-common").file("src/main/resources/oelib-bless.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibBless"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibBless"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}