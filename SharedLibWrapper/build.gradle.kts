plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.launcher3.sharedlibwrapper"

    sourceSets {
        named("main") {
            res.setSrcDirs(listOf("res"))
        }
    }
}
