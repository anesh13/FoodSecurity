plugins {
    id("com.android.application")
}

android {
    namespace = "com.sjsu.cmpe277.foodsecurity277"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.sjsu.cmpe277.foodsecurity277"
        minSdk = 24
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.navigation:navigation-fragment:2.4.2")
    implementation("androidx.navigation:navigation-ui:2.4.2")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.navigation:navigation-runtime:2.7.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.jjoe64:graphview:3.1.4")
    implementation("com.jjoe64:graphview:4.2.0")

}