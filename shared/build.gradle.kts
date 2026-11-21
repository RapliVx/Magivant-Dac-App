plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.gson)
    testImplementation(libs.junit)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnit()
}
