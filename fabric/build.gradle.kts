plugins {
    id("multiloader-loader")
    id("fabric-loom")
}

val modId: String = property("mod_id") as String
val mcVersion: String = property("minecraft_version") as String
val fabricLoaderVer: String = property("fabric_loader_version") as String
val fabricApiVer: String = property("fabric_api_version") as String
val modmenuVer: String = property("modmenu_version") as String
val parchmentMC: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String
val jeiVer: String = property("jei_version") as String

repositories {
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMC}:${parchmentVer}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVer}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVer}")
    modImplementation("com.terraformersmc:modmenu:${modmenuVer}")
//    modImplementation("mezz.jei:jei-${mcVersion}-fabric:${jeiVer}")

    implementation("cn.6tail:lunar:1.7.3")
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("de.marhali:json5-java:3.0.0")
    implementation("org.mvel:mvel2:2.5.0.Final")
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")

    annotationProcessor(project(":common"))

    include("cn.6tail:lunar:1.7.3")
    include("com.electronwill.night-config:core:3.8.3")
    include("com.electronwill.night-config:toml:3.8.3")
    include("de.marhali:json5-java:3.0.0")
    include("io.smallrye.classfile:jdk-classfile-backport:26")
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/oelib.accesswidener")
    mixin {
        defaultRefmapName = "${modId}.refmap.json"
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}