plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    id("net.researchgate.release") version "3.1.0"
    id("com.github.ben-manes.versions") version "0.52.0"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("javax.websocket:javax.websocket-client-api:1.1")
    testImplementation("org.glassfish.tyrus:tyrus-client:2.2.0")
    testImplementation("org.glassfish.tyrus:tyrus-container-grizzly:1.2.1")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("io.mockk:mockk-jvm:1.13.16")
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}

group = "org.roborace"
description = "roborace-laps-counter"

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

release.git.requireBranch = "master"

tasks.test {
    maxParallelForks = 1
    useJUnitPlatform()
}
