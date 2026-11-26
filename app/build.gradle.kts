import java.util.Properties

plugins {
    id("com.android.application")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

// The key from local.properties wins. The fallback keeps the demo buildable
// out of the box; replace it with your own MapKit key for real projects.
val mapkitApiKey = localProperties.getProperty("MAPKIT_API_KEY")
    ?.takeIf { it.isNotBlank() }
    ?: "51bb0562-cabc-4b14-a41a-bedda57d8292"

val mapkitVersion = providers.gradleProperty("mapkitVersion")
    .getOrElse("4.45.0-full")

android {
    namespace = "com.example.yandexmaps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.yandexmaps"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "MAPKIT_API_KEY", "\"$mapkitApiKey\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    // "full" is required: the lite build does not include search/geocoder.
    implementation("com.yandex.android:maps.mobile:$mapkitVersion")
}
