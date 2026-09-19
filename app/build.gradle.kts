import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildingBundle = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }
val releaseKeystorePropertiesFile = rootProject.file("release-keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.isFile) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = releaseKeystorePropertiesFile.isFile

android {
    namespace = "com.aihearingassist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aihearingassist"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.aihearingassist.LocalModelSmokeInstrumentation"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "speechModel"

    productFlavors {
        create("englishIn") {
            dimension = "speechModel"
            buildConfigField("String", "BUNDLED_SPEECH_MODELS", "\"model-en-in\"")
        }
        create("englishUs") {
            dimension = "speechModel"
            buildConfigField("String", "BUNDLED_SPEECH_MODELS", "\"model-en-us\"")
        }
        create("hindi") {
            dimension = "speechModel"
            buildConfigField("String", "BUNDLED_SPEECH_MODELS", "\"model-hi\"")
        }
        create("full") {
            dimension = "speechModel"
            buildConfigField("String", "BUNDLED_SPEECH_MODELS", "\"model-en-us,model-en-in,model-hi\"")
        }
    }

    sourceSets {
        getByName("englishIn") {
            assets.srcDir("src/englishIn/assets")
        }
        getByName("englishUs") {
            assets.srcDir("src/englishUs/assets")
        }
        getByName("hindi") {
            assets.srcDir("src/hindi/assets")
        }
        getByName("full") {
            assets.srcDirs("src/englishIn/assets", "src/englishUs/assets", "src/hindi/assets")
        }
    }

    splits {
        abi {
            isEnable = !buildingBundle
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val storeFilePath = releaseKeystoreProperties.getProperty("storeFile")
                storeFile = rootProject.file(storeFilePath)
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "litertlm"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
    implementation("com.alphacephei:vosk-android:0.3.75@aar")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.21")
}
