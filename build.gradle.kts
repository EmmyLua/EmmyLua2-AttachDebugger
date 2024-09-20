/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import de.undercouch.gradle.tasks.download.*

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0-Beta4"
    id("org.jetbrains.intellij") version "1.17.2"
    id("de.undercouch.download").version("5.3.0")
}

data class BuildData(
    val ideaSDKShortVersion: String,
    // https://www.jetbrains.com/intellij-repository/releases
    val ideaSDKVersion: String,
    val sinceBuild: String,
    val untilBuild: String,
    val archiveName: String = "EmmyLua2-AttachDebugger",
    val jvmTarget: String = "17",
    val targetCompatibilityLevel: JavaVersion = JavaVersion.VERSION_17,
    // https://github.com/JetBrains/gradle-intellij-plugin/issues/403#issuecomment-542890849
    val instrumentCodeCompilerVersion: String = ideaSDKVersion,
    val type: String = "IU"
)

val buildDataList = listOf(
    BuildData(
        ideaSDKShortVersion = "242",
        ideaSDKVersion = "2024.2",
        sinceBuild = "232",
        untilBuild = "242.*",
    )
)

group = "com.cppcxy"
val emmyluaDebuggerVersion = "1.8.2"
val emmyluaDebuggerProjectUrl = "https://github.com/EmmyLua/EmmyLuaDebugger"

val buildVersion = System.getProperty("IDEA_VER") ?: buildDataList.first().ideaSDKShortVersion

val buildVersionData = buildDataList.find { it.ideaSDKShortVersion == buildVersion }!!

val runnerNumber = System.getenv("RUNNER_NUMBER") ?: "Dev"

version = "${emmyluaDebuggerVersion}.${runnerNumber}-IDEA${buildVersion}"

repositories {
    mavenCentral()
}

intellij {
    pluginName.set("EmmyLua2-AttachDebugger")
    version.set(buildVersionData.ideaSDKVersion)
    type.set(buildVersionData.type) // Target IDE Platform
    sandboxDir.set("${project.buildDir}/${buildVersionData.ideaSDKShortVersion}/idea-sandbox")
    plugins.set(listOf("com.cppcxy.Intellij-EmmyLua:0.7.1.20-IDEA242"))
}



task("downloadDebugger", type = Download::class) {
    src(arrayOf(
        "${emmyluaDebuggerProjectUrl }/releases/download/${emmyluaDebuggerVersion}/win32-x86.zip",
        "${emmyluaDebuggerProjectUrl }/releases/download/${emmyluaDebuggerVersion}/win32-x64.zip",
    ))

    dest("temp")
}

task("unzipDebugger", type = Copy::class) {
    dependsOn("downloadDebugger")
    from(zipTree("temp/win32-x64.zip")) {
        into("bin/win32-x64")
    }
    from(zipTree("temp/win32-x86.zip")) {
        into("bin/win32-x86")
    }
    destinationDir = file("temp")
}

task("installDebugger", type = Copy::class) {
    dependsOn("unzipDebugger")
    from("temp/bin") {
        into("bin")
    }

    destinationDir = file("src/main/resources/debugger")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = buildVersionData.jvmTarget
    }
    instrumentCode {
        compilerVersion.set(buildVersionData.instrumentCodeCompilerVersion)
    }

    patchPluginXml {
        sinceBuild.set(buildVersionData.sinceBuild)
        untilBuild.set(buildVersionData.untilBuild)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    buildPlugin {
        dependsOn("installDebugger")
    }

    withType<org.jetbrains.intellij.tasks.PrepareSandboxTask> {
        doLast {
            copy {
                from("src/main/resources/debugger/bin")
                into("$destinationDir/${pluginName.get()}/debugger/bin")
            }
        }
    }
}
