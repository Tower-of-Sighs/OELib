plugins {
    id("module-common")
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

extra["mavenDependencyWhitelist"] = listOf("de.marhali:json5-java", "com.electronwill.night-config:toml", "io.smallrye.classfile:jdk-classfile-backport", "com.flechazo:optics-java")
dependencies {

    implementation("de.marhali:json5-java:3.0.0")
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")
    api("com.flechazo:optics-java:1.0-20260621.110319-4")

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

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}
