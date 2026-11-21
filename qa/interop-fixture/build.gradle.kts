// CI-only Android application that acts as a separate third-party caller.
plugins {
    id("com.android.application")
}

android {
    namespace = "com.goreecloud.gifs.interopfixture"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.goreecloud.gifs.interopfixture"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-qa"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
