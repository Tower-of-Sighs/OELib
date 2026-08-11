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

extra["mavenDependencyWhitelist"] = listOf("com.flechazo:optics-java", "io.smallrye.classfile:jdk-classfile-backport")
dependencies {
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")
    api("com.flechazo:optics-java:1.0.8-beta") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "it.unimi.dsi", module = "fastutil")
    }
}

configureCommonSourceArtifacts()
