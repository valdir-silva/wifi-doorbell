plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    // iosArm64()            // uncomment when iOS target is added
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth.kmp)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
    }
}

android {
    namespace = "com.alunando.wifidoorbell.core"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}

sqldelight {
    databases {
        create("WifiDoorbellDb") {
            packageName.set("com.alunando.wifidoorbell.core.db")
        }
    }
}
