plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.launcher3.sharedlibwrapper"

    sourceSets {
        named("main") {
            java.setSrcDirs(listOf("src/main/java"))
            res.setSrcDirs(listOf("res"))
        }
    }
}
