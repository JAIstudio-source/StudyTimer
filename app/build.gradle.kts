plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

val localProps = Properties().apply {
    val f = File(rootDir, "local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val envProps = Properties().apply {
    val f = File(rootDir, ".env")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(key: String, default: String = ""): String =
    System.getenv(key) ?: envProps.getProperty(key) ?: localProps.getProperty(key) ?: default

android {
    namespace = "com.madeby.JAI"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.madeby.JAI"
        
        // Supports Android 9 (API 28) up to Android 16 (API 36)
        minSdk = 28
        targetSdk = 36
        
        versionCode = 12
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.p12")
            storePassword = secret("STORE_PASSWORD")
            keyPassword = secret("KEY_PASSWORD")
            keyAlias = secret("KEY_ALIAS", "key0")
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signs release builds with your JAI Labs key
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
