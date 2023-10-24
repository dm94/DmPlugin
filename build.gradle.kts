
plugins {
    id("org.gradle.java-library")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare", "proguard-gradle", "7.2.2")
    }
}

repositories {
    mavenLocal()
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
}

allprojects {
    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

group = "com.deeme"
version = "2.1.10"
description = "DmPlugin"

dependencies {
    api("eu.darkbot.DarkBotAPI", "darkbot-impl", "0.7.8")
    api("eu.darkbot", "DarkBot", "6d026357cb")
    implementation(files("private.jar"))
}

tasks.register<proguard.gradle.ProGuardTask>("proguard") {
    dependsOn("uberJar")
    configuration("proguard.conf")
    dontnote()
    dontwarn()

    injars("./build/libs/DmPlugin-2.1.10.jar")
    outjars("DmPlugin.jar")
}

tasks.register<Jar>("uberJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("DmPlugin.jar")
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.equals("private.jar") }.map { zipTree(it) }
    })
}

tasks.register<Copy>("copyFile") {
    dependsOn("uberJar")
    from(layout.buildDirectory.file("DmPlugin.jar"))
    into("DmPlugin.jar")
}

tasks.register<Exec>("signFile") {
    dependsOn("copyFile")
    commandLine("cmd", "/c", "sign.bat")
}