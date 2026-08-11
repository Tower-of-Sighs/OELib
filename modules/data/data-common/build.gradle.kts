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

extra["mavenDependencyWhitelist"] = listOf("org.mvel:mvel2")
dependencies {
    implementation("org.mvel:mvel2:2.5.0.Final")
}

moduleDependencies {
    api(":modules:event")
    api(":modules:network")
    api(":modules:misc")
}

configureCommonSourceArtifacts()
