import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinSerialization)
}

val generateHanaInfo by tasks.registering {
    val gitHash = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        workingDir(rootProject.rootDir)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().ifBlank { "dev" } }
    val gitVersion = providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
        workingDir(rootProject.rootDir)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { raw ->
        raw.trim().ifBlank { "v0.1-alpha" }
    }
    val gitTagRefs = providers.exec {
        commandLine(
            "git",
            "for-each-ref",
            "--format=%(if)%(*objectname)%(then)%(*objectname)%(else)%(objectname)%(end)|%(refname:short)",
            "refs/tags",
        )
        workingDir(rootProject.rootDir)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }
    val changelog = providers.exec {
        commandLine("git", "log", "-n", "40", "--pretty=format:%H|%h|%ad|%s", "--date=short")
        workingDir(rootProject.rootDir)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }
    val outputDir = layout.buildDirectory.dir("generated/hanaInfo/kotlin")

    inputs.property("gitVersion", gitVersion)
    inputs.property("gitHash", gitHash)
    inputs.property("gitTagRefs", gitTagRefs)
    inputs.property("changelog", changelog)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile.resolve("com/acite/katahana/generated")
        dir.mkdirs()
        fun String.kotlinString(): String = buildString {
            append('"')
            for (ch in this@kotlinString) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\\$")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
        fun List<String>.kotlinList(): String =
            if (isEmpty()) "emptyList()"
            else "listOf(${joinToString { it.kotlinString() }})"

        val tagsByCommit = HashMap<String, MutableList<String>>()
        gitTagRefs.get().lineSequence().forEach { line ->
            val sep = line.indexOf('|')
            if (sep <= 0) return@forEach
            val commit = line.substring(0, sep).trim()
            val tag = line.substring(sep + 1).trim()
            if (commit.isEmpty() || tag.isEmpty()) return@forEach
            tagsByCommit.getOrPut(commit) { ArrayList() }.add(tag)
        }

        val entries = changelog.get().lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.count { c -> c == '|' } >= 3 }
            .joinToString(",\n        ") { line ->
                val first = line.indexOf('|')
                val second = line.indexOf('|', first + 1)
                val third = line.indexOf('|', second + 1)
                val full = line.substring(0, first)
                val hash = line.substring(first + 1, second)
                val date = line.substring(second + 1, third)
                val subject = line.substring(third + 1)
                val tags = tagsByCommit[full].orEmpty().distinct()
                "ChangelogEntry(${hash.kotlinString()}, ${date.kotlinString()}, ${subject.kotlinString()}, ${tags.kotlinList()})"
            }
        val body = buildString {
            appendLine("package com.acite.katahana.generated")
            appendLine()
            appendLine("import com.acite.katahana.changelog.ChangelogEntry")
            appendLine()
            appendLine("object AppInfo {")
            appendLine("    const val version: String = ${gitVersion.get().kotlinString()}")
            appendLine("    const val gitHash: String = ${gitHash.get().kotlinString()}")
            appendLine("    val changelog: List<ChangelogEntry> = listOf(")
            if (entries.isNotEmpty()) {
                appendLine("        $entries")
            }
            appendLine("    )")
            appendLine("}")
        }
        dir.resolve("AppInfo.kt").writeText(body)
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvmToolchain(17)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    android {
        namespace = "com.acite.katahana.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.savedstate.compose)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain {
            kotlin.srcDir(generateHanaInfo.map { it.outputs.files.singleFile })
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.uiBackhandler)
                implementation(libs.navigationevent.compose)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                api(libs.metrox.viewmodel.compose)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.transitions)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websockets)
                implementation(libs.haze)
                implementation(libs.haze.blur)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}