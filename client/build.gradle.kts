plugins {
    id("com.android.library")
    id("maven-publish")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fpf.smartscan.client"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        aidl = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }

    lint {
        targetSdk = 34
    }
}

dependencies {
    api(libs.smartscan.core)
}

val gitVersion: String by lazy {
    runCatching {
        val proc = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
        if (proc.waitFor() == 0) out.removePrefix("v") else throw RuntimeException("git failed")
    }.getOrDefault("1.0.0")
}


publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.dev-diaries41.smartscan-client"
            artifactId = "smartscan-${project.name}"
            version = gitVersion

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}