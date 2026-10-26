plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dshpet.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dshpet.android"
        minSdk = 28
        targetSdk = 34
        versionCode = 13
        versionName = "1.8.1-counter.2"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            // 用 debug 签名，保证任何机器上都能直接安装
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        // 动画 WebP 已经压过，构建时不要再二次处理
        noCompress += listOf("webp")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // 应用运行时只用 Android 框架 API；以下依赖仅供本地单元测试。
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
}
