import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
    id("me.champeau.jmh") version "0.7.2"
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
    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    // Aggregate every module's common API. Consumers can depend on the single
    // oelib-common coordinate while javac still receives each module as a real
    // classpath entry (nested jars would not be visible to the compiler).
    @Suppress("UNCHECKED_CAST")
    val discoveredModules = rootProject.extra["discoveredModules"] as? Map<String, File> ?: emptyMap()
    for ((name, _) in discoveredModules) {
        val path = ":modules:$name:$name-common"
        try {
            api(project(path))
        } catch (_: UnknownProjectException) {
        }
    }
}

extra["mavenDependencyWhitelist"] = listOf(
    "cc.sighs.oelib",
    "cn.6tail",
    "de.marhali",
    "com.electronwill.night-config",
    "org.mvel",
    "io.smallrye.classfile"
)

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

configureCommonSourceArtifacts()

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
