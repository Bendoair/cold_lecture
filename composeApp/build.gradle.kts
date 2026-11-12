import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlin.io.encoding.ExperimentalEncodingApi")
        }
        wasmJsMain {
            languageSettings.optIn("kotlin.js.ExperimentalWasmJsInterop")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.room.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sqlite.bundled)
                implementation(libs.pdfbox)
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.sqlite.web)
            implementation(npm("cold-lecture-web", layout.projectDirectory.dir("web").asFile))
        }
    }
}

dependencies {
    add("kspDesktop", libs.room.compiler)
    add("kspWasmJs", libs.room.compiler)
}

room3 {
    schemaDirectory(layout.projectDirectory.dir("schemas"))
}

compose.resources {
    packageOfResClass = "dev.coldlecture.resources"
    generateResClass = always
}

// Compose Desktop :run ignores kotlin.jvmToolchain and follows JAVA_HOME by default.
// Pin both run and packaging to JDK 21 so a shell still on 17 can't launch class-file 65.
val jdk21Home = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}.get().metadata.installationPath.asFile.absolutePath

compose.desktop {
    application {
        mainClass = "dev.coldlecture.MainKt"
        javaHome = jdk21Home
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "ColdLecture"
            packageVersion = "1.0.0"
            javaHome = jdk21Home
            linux {
                iconFile.set(project.file("icon/icon.png"))
            }
            windows {
                iconFile.set(project.file("icon/icon.ico"))
            }
            macOS {
                // PNG works as a source for the packaging toolchain on CI/mac hosts.
                iconFile.set(project.file("icon/icon.png"))
            }
        }
    }
}
