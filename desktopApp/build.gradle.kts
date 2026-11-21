import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.compose.material3.desktop)
    implementation(libs.compose.material.icons.extended.desktop)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.usb4java) {
        exclude(group = "org.usb4java", module = "libusb4java")
    }
    val linuxNativeClassifier = when (System.getProperty("os.arch").lowercase()) {
        "amd64", "x86_64" -> "linux-x86-64"
        "aarch64", "arm64" -> "linux-aarch64"
        else -> error("Linux desktop builds support x86_64 and AArch64, found ${System.getProperty("os.arch")}")
    }
    runtimeOnly("org.usb4java:libusb4java:${libs.versions.usb4java.get()}:$linuxNativeClassifier")
    testImplementation(libs.junit)
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "com.rapli.magivant.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "magivant-dac"
            packageVersion = "3.6.0"
            description = "Expressive control panel for Magivant USB DAC"
            linux {
                appCategory = "AudioVideo"
                iconFile.set(project.file("src/main/resources/magivant-dac.png"))
            }
        }
    }
}
