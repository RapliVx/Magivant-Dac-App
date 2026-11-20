import java.security.KeyStore
import java.security.PrivateKey

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.lsplugin.apksign)
}

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    doLast {
        val names = listOf("KEYSTORE_FILE", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
        val properties = names.associateWith { providers.gradleProperty(it).orNull }
        check(properties.values.all { !it.isNullOrBlank() }) {
            "Release signing requires KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
        }
        val keystoreFile = rootProject.file(requireNotNull(properties["KEYSTORE_FILE"]))
        check(keystoreFile.isFile) { "The release keystore file does not exist." }
        val keystore = KeyStore.getInstance(
            keystoreFile, requireNotNull(properties["KEYSTORE_PASSWORD"]).toCharArray()
        )
        val alias = requireNotNull(properties["KEY_ALIAS"])
        check(keystore.isKeyEntry(alias)) { "The release key alias does not contain a private key." }
        check(keystore.getKey(alias, requireNotNull(properties["KEY_PASSWORD"]).toCharArray()) is PrivateKey) {
            "The release signing private key is unavailable."
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(verifyReleaseSigning)
}

android {
    namespace = "com.rapli.magivant"
    compileSdk = 37
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.rapli.magivant"
        minSdk = 29
        targetSdk = 37
        versionCode = 7
        versionName = "3.5-Expressive"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material.kolor)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.ucrop)
    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
