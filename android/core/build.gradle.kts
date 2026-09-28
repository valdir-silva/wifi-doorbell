plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        compileSdk = 35
        minSdk = 26
        namespace = "com.alunando.wifidoorbell.core"
    }

    // iosArm64()            // uncomment when iOS target is added
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth.kmp)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
    }
}

dependencies {
    // Firebase BOM provides versions for com.google.firebase:* transitive deps from dev.gitlive
    "androidMainImplementation"(platform(libs.firebase.bom))
}

sqldelight {
    databases {
        create("WifiDoorbellDb") {
            packageName.set("com.alunando.wifidoorbell.core.db")
        }
    }
}
