plugins {
    id("module-loader")
    id("fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMc}:${parchmentVer}@zip")
    })
}

loom {
    val aw = project(":modules:{{MODULE_NAME}}:{{MODULE_NAME}}-common").file("src/main/resources/{{FULL_MOD_ID}}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - {{CLASS_PREFIX}}"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - {{CLASS_PREFIX}}"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
