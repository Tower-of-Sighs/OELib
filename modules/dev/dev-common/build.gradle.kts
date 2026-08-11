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

dependencies {
}

moduleDependencies {
    api(":modules:registry")
    api(":modules:data")
    api(":modules:bless")
    api(":modules:renderer")
    api(":modules:event")
    api(":modules:misc")
    api(":modules:config")
    api(":modules:network")
    annotationProcessor(":modules:data")
}

configureCommonSourceArtifacts()
