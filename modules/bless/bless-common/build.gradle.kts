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
