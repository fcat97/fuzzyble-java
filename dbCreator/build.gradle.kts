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
                implementation("media.uqab.fuzzyble:fuzzybleJava:0.6.4")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "media.uqab.fuzzyble.dbCreator.MainKt"

        // https://github.com/JetBrains/compose-multiplatform/issues/2668#issuecomment-1419178642
        buildTypes.release {
            proguard {
                configurationFiles.from("compose-desktop.pro")
            }
        }

        nativeDistributions {
            // https://github.com/JetBrains/compose-multiplatform/issues/2668#issuecomment-1419178642
            // https://github.com/Wavesonics/compose-multiplatform-file-picker/issues/5#issuecomment-1808325426
            includeAllModules = true

            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "dbCreator"
            packageVersion = "1.0.3"
            windows {
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
