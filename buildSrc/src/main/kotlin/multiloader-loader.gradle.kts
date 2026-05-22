plugins {
    id("multiloader-common")
}

val mavenGroup: String = property("maven_group") as String
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
    compileOnly(project(":common-api")) {
        capabilities {
            requireCapability("${mavenGroup}:${modId}")
        }
    }
    add("commonJava", project(":common-api", "commonJava"))
    add("commonResources", project(":common-api", "commonResources"))
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
