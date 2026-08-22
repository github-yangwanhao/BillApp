plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.24"
    kotlin("kapt")  // ← 新增这一行
}

android {
    namespace = "cn.yangwanhao.billapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "cn.yangwanhao.billapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")  // ← 改回 kapt，不是 ksp 也不是 annotationProcessor

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.4")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
}