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
version = "0.0.6.4"

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
    // 优先用本机已安装的 IDEA，避免再下 1.4GB 的 idea-*-aarch64.dmg。
    // 覆盖路径：-PintellijPlatform.localPath=/path/to/IntelliJ IDEA.app
    // 本机没有 IDEA 时回落到 2026.2.0.1 安装包（2026.2 起 IC 不再单独发布，统一用 intellijIdea）
    intellijPlatform {
        val localIdePath = (findProperty("intellijPlatform.localPath") as String?)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && file(it).exists() }
            ?: listOf(
                file("${System.getProperty("user.home")}/Applications/IntelliJ IDEA.app"),
                file("/Applications/IntelliJ IDEA.app"),
            ).firstOrNull { it.isDirectory }?.absolutePath

        if (localIdePath != null) {
            logger.lifecycle("IntelliJ Platform: using local IDE at $localIdePath")
            local(localIdePath)
        } else {
            intellijIdea("2026.2.0.1")
        }
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
        // 最低支持 2022.2(222)；不设上限，避免每次 IDEA 大版本（262→263→271…）都因 until-build 被判定不兼容
        sinceBuild.set("222")
        // 不能写成 999.* 这类虚构号（Marketplace/Verifier 会拒）；provider { null } 才会真正去掉 until-build
        // 直接 untilBuild.set(null) 会回落到 Gradle/IPGP 默认值（当前编译平台 262.*），等于没放开
        untilBuild.set(provider { null })
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
