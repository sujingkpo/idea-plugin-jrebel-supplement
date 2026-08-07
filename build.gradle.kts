import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    // IDEA 2026.x 平台类使用 Kotlin 2.4.0 元数据编译，编译器必须 >= 2.4 才能解析（1.8.21 会报 incompatible metadata）
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    // IntelliJ Platform Gradle Plugin 1.x 不支持 2024.2+ (242+) 平台，官方要求升级到 2.x
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.czy"
version = "0.0.6.3"

// IPGP 2.x 默认按平台 JBR（2026.2 = Java 25）配置编译 toolchain，本机无 JDK 25。
// 为兼容 2022.2.5（JBR 11），编译为 Java 11 字节码（2022.3+ 的 JBR 17、2026.x 的 JBR 25 均向下兼容 11）
kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
    // 2.x 需要显式声明 IntelliJ Platform 官方仓库（releases/snapshots/plugins-marker 等）
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // 2.x 将平台与插件依赖迁移到 dependencies 块内（替代 1.x 的 intellij { } DSL）
    // 2026.2 起 IDEA Community(IC) 不再单独发布，统一使用 intellijIdea
    intellijPlatform {
        intellijIdea("2026.2.0.1")
        bundledPlugin("com.intellij.java")
    }
    implementation(fileTree("libs") {
        include("*.jar")
    })
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        // Kotlin 2.x 中 kotlinOptions 已废弃，改用 compilerOptions DSL
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    patchPluginXml {
        // 最低支持 2022.2(222) ~ 最高 2026.2(262)；编译平台仍为 2026.2 SDK（仅改声明，不重下依赖）
        sinceBuild.set("222")
        // IDEA 2026.2 对应 build 号 262.x，此前 244.* 会导致插件在 2026.2 上被判定不兼容而无法加载
        untilBuild.set("262.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
