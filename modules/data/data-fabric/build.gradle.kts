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

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMc}:${parchmentVer}@zip")
    })

    api("org.mvel:mvel2:2.5.0.Final")

    include("org.mvel:mvel2:2.5.0.Final")

}

loom {
    val aw = project(":modules:data:data-common").file("src/main/resources/oelib-data.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibData"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibData"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}