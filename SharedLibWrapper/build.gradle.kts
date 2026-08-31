plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.android.launcher3.sharedlibwrapper"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/java")
            kotlin.srcDirs("src/main/java")
            res.setSrcDirs(listOf("res"))
        }
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.core)
    api(libs.androidx.core.ktx)
    api(libs.androidx.graphics.shapes)
}
