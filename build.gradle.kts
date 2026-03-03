import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

group = "com.spyglass.connect"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Ktor WebSocket server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // NBT / Minecraft
    implementation(libs.querz.nbt)

    // QR code generation
    implementation(libs.zxing.core)

    // mDNS service discovery
    implementation(libs.jmdns)

    // Encryption (ECDH + AES-GCM)
    implementation(libs.bouncycastle)

    // Testing
    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.spyglass.connect.SpyglassConnectKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Spyglass Connect"
            packageVersion = "1.0.0"
            description = "Minecraft companion — stream world data to Spyglass on your phone"
            vendor = "Spyglass"

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "Spyglass"
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "com.spyglass.connect"
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
