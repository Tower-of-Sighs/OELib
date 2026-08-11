import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("module-common")
    id("net.neoforged.moddev")
}

val NFMVersion = property("neo_form_version") as String

neoForge {
    neoFormVersion = NFMVersion

    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")
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
