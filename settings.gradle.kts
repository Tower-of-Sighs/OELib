pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Sponge Snapshots"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "oelib"
include("common")
include("fabric")
include("forge")
