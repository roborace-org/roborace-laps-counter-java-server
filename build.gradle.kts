plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("net.researchgate.release") version "3.0.2"
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
    testImplementation("org.glassfish.tyrus:tyrus-client:1.17")
    testImplementation("org.glassfish.tyrus:tyrus-container-grizzly:1.2.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("io.mockk:mockk-jvm:1.13.9")
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
    maxParallelForks = 2
    useJUnitPlatform()
}
