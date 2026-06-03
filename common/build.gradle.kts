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
}

extra["mavenDependencyWhitelist"] = listOf(
    "cn.6tail",
    "de.marhali",
    "com.electronwill.night-config",
    "org.mvel",
    "io.smallrye.classfile"
)

configurations {
    register("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    register("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}