import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("multiloader-common")
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
