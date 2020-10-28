import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "cz.vlada"
version = "1.0.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.assertj:assertj-core:3.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "cz.vlada.beer.fridge.MainKt"
    }

}
tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

task<Exec>("deploy") {
    dependsOn("build")
    commandLine = listOf("sh", "deploy.sh")
}
