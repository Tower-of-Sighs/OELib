import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("module-common")
    id("net.neoforged.moddev.legacyforge")
}

val mcVersion: String = property("minecraft_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

legacyForge {
    mcpVersion = mcVersion

    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")
    }

    parchment {
        minecraftVersion = parchmentMc
        mappingsVersion = parchmentVer
    }
}

extra["mavenDependencyWhitelist"] = listOf("cn.6tail:lunar")
dependencies {

    implementation("cn.6tail:lunar:1.7.3")
}
moduleDependencies {
    api(":modules:config")
    api(":modules:registry")
    api(":modules:misc")
}
configureCommonSourceArtifacts()