import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.compose") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

version = "0.3.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation(kotlin("test"))
}


// Kotlin 2.2.x can't target JDK 25 bytecode yet; compile for 21, which runs fine on newer JDKs.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }
}

tasks.test { useJUnit() }

compose.desktop {
    application {
        mainClass = "app.cleanmac.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "CleanMac"
            // macOS packages require MAJOR >= 1, so 0.y.z ships as 1.y.z
            packageVersion = project.version.toString().let { v ->
                v.split(".").let { p -> if (p[0] == "0") "1.${p[1]}.${p[2]}" else v }
            }
        }
    }
}
