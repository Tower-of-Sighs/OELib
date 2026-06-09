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
    implementation("com.google.code.findbugs:jsr305:3.0.1")

    implementation("cn.6tail:lunar:1.7.3")
    implementation("de.marhali:json5-java:3.0.0")
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("com.electronwill.night-config:core:3.8.3")
    implementation("org.mvel:mvel2:2.5.0.Final")
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly ("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation(files(sourceSets.main.get().compileClasspath))

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
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