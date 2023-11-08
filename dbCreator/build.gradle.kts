import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "media.uqab.fuzzyble"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.preview)

                implementation("com.darkrockstudios:mpfilepicker:2.1.0")
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("org.xerial:sqlite-jdbc:3.43.2.2")
                implementation("media.uqab.fuzzyble:fuzzybleJava:0.6.1")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "media.uqab.fuzzyble.dbCreator.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dbCreator"
            packageVersion = "1.0.1"
        }
    }
}
