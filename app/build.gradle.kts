plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.compose)


}

android {
    namespace = "com.example.eclinic1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.eclinic1"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation (platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-messaging:24.0.0") // lub najnowsze

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.7.8") // Główna biblioteka Compose
    implementation("androidx.compose.material3:material3:1.3.1") // Material3 dla Compose
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")  // Narzędzia do podglądu
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")  // ViewModel dla Compose
    implementation(libs.androidx.activity.compose.v160)  // Zależności Activity dla Compose
    implementation("androidx.compose.runtime:runtime-livedata:1.7.8")  // Obsługa LiveData w Compose
    implementation("androidx.compose.material:material-icons-extended")

    implementation ("androidx.compose.material:material-icons-extended")
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.31.5-beta")
    implementation ("androidx.browser:browser:1.5.0")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.android.gms:play-services-base:18.2.0")

    // Firebase dependencies
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    // AndroidX dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.activity.compose)

    // Navigation dependencies
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle dependencies
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Other dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.firebase.functions.ktx)  // Bill of Materials dla Compose

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Google Play Services - najnowsze wersje
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("com.google.android.gms:play-services-base:18.7.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-tasks:18.3.0")

    
}