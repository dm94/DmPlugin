
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
    configuration("proguard.conf")
    dontnote()
    dontwarn()

    injars("./DmPluginTest.jar")
    outjars("DmPlugin.jar")
}