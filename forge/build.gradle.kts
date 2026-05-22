plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev.legacyforge")
}

val modId: String = property("mod_id") as String
val mcVersion: String = property("minecraft_version") as String
val forgeVer: String = property("forge_version") as String
val parchmentMC: String = property("parchment_minecraft") as String
val parchmentVer: String =  property("parchment_version") as String
val jeiVer: String = property("jei_version") as String

mixin {
    add(sourceSets.main.get(), "${modId}.refmap.json")
    config("${modId}.mixins.json")
    config("${modId}.forge.mixins.json")
}

legacyForge {
    version = "${mcVersion}-${forgeVer}"

    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    parchment {
        minecraftVersion = parchmentMC
        mappingsVersion = parchmentVer
    }

    runs {
        create("client") {
            client()
        }
        create("data") {
            data()
            programArguments.addAll(
                "--mod",
                modId,
                "--all",
                "--output",
                project.file("src/generated/resources/").absolutePath,
                "--existing",
                project.file("src/main/resources/").absolutePath
            )
        }
        create("server") {
            server()
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")
    annotationProcessor(project(":common"))

//    modImplementation("mezz.jei:jei-${mcVersion}-forge:${jeiVer}")

    implementation("cn.6tail:lunar:1.7.3")
    implementation("de.marhali:json5-java:3.0.0")
    implementation("org.mvel:mvel2:2.5.0.Final")
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")

    jarJar("cn.6tail:lunar:1.7.3")
    jarJar("de.marhali:json5-java:3.0.0")
    jarJar("io.smallrye.classfile:jdk-classfile-backport:26")

    compileOnly("org.jetbrains:annotations:24.1.0")

    "additionalRuntimeClasspath"("cn.6tail:lunar:1.7.3")
    "additionalRuntimeClasspath"("de.marhali:json5-java:3.0.0")
    "additionalRuntimeClasspath"("io.smallrye.classfile:jdk-classfile-backport:26")
}

tasks.named<Jar>("jar") {
    finalizedBy("reobfJar")
    manifest {
        attributes(
            mapOf(
                "MixinConfigs" to "${modId}.mixins.json,${modId}.forge.mixins.json"
            )
        )
    }
}
