plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "de.belegkasten.app"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "de.belegkasten.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.3.0-beta"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Local Gradle property; absent key means billing cannot grant a purchase.
        resValue("string", "play_billing_public_key", providers.gradleProperty("belegkastenPlayPublicKey").orElse("").get())
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/*" } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:receipts"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    val bom = libs.compose.bom
    implementation(platform(bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.play.billing)
    // PAD library only; no actual model/asset-pack module yet.
    implementation(libs.play.asset.delivery)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform(bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
