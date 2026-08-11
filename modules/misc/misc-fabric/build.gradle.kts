plugins {
    id("module-loader")
    id("fabric-loom")
}

val mcVersion: String = property("minecraft_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

extra["mavenDependencyWhitelist"] = listOf("com.flechazo:optics-java", "io.smallrye.classfile:jdk-classfile-backport")
dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMc}:${parchmentVer}@zip")
    })

    api("io.smallrye.classfile:jdk-classfile-backport:26")
    api("com.flechazo:optics-java:1.0.8-beta") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
    include("io.smallrye.classfile:jdk-classfile-backport:26")
    include("com.flechazo:optics-java:1.0.8-beta")
}

loom {
    val aw = project(":modules:misc:misc-common").file("src/main/resources/oelib-misc.accesswidener")
    if (aw.exists()) {
        accessWidenerPath = aw
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client - OELibMisc"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server - OELibMisc"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
