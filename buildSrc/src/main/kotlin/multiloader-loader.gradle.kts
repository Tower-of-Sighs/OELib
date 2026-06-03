plugins {
    id("multiloader-common")
}

val mavenGroup: String = property("group") as String
val modId: String = property("mod_id") as String

configurations {
    register("commonJava") {
        isCanBeResolved = true
    }
    register("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project(":common")) {
        capabilities {
            requireCapability("${mavenGroup}:${modId}")
        }
    }
    add("commonJava", project(":common", "commonJava"))
    add("commonResources", project(":common", "commonResources"))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(configurations.getByName("commonResources"))
    from(configurations.getByName("commonResources"))
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.getByName("commonJava"))
    from(configurations.getByName("commonJava"))
    dependsOn(configurations.getByName("commonResources"))
    from(configurations.getByName("commonResources"))
}
