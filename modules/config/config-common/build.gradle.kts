import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("module-common")
    id("net.neoforged.moddev.legacyforge")
    id("me.champeau.jmh") version "0.7.2"
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

extra["mavenDependencyWhitelist"] = listOf("de.marhali:json5-java", "com.electronwill.night-config:toml")
dependencies {

    implementation("de.marhali:json5-java:3.0.0")
    implementation("com.electronwill.night-config:toml:3.8.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly ("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation(files(sourceSets.main.get().compileClasspath))

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

moduleDependencies {
    api(":modules:network")
    api(":modules:event")
    api(":modules:misc")
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

configureCommonSourceArtifacts()
