pluginManagement {
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { setUrl("https://dl.google.com/dl/android/maven2") }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }   // 🔥 新增
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }   // 🔥 新增
    }
}
rootProject.name = "BillApp"
include(":app")