import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val formattedDate: String = SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())

android {
    namespace = "com.video.downloader.downloadvideo.videodownloader"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.video.downloader.downloadvideo.videodownloader"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/gameempire.jks")
            storePassword = "gameempire"
            keyAlias = "gameempire"
            keyPassword = "gameempire"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        val type = variant.buildType.name

        variant.outputs.all {
            val output = this as? com.android.build.gradle.api.ApkVariantOutput
            if (type == "release") {
                output?.outputFileName =
                    "A500_VideoDownload_${variant.versionName}_${variant.versionCode}_${formattedDate}.apk"
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        val versionName = android.defaultConfig.versionName ?: "1.0.0"
        val versionCode = android.defaultConfig.versionCode ?: 1
        val type = variant.buildType ?: variant.name

        if (type == "release") {
            variant.outputs.forEach { output ->
                output.outputFileName.set(
                    "A500_VideoDownload_${versionName}_${versionCode}_${formattedDate}.apk"
                )
            }
        }
    }
}

tasks.matching { it.name.startsWith("bundle") && it.name.endsWith("Release") }.configureEach {
    doLast {
        val bundleDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        val targetName =
            "A500_VideoDownload_${android.defaultConfig.versionName}_${android.defaultConfig.versionCode}_${formattedDate}.aab"
        bundleDir.listFiles { f -> f.extension == "aab" }?.forEach { aab ->
            if (aab.name != targetName) {
                val targetFile = File(aab.parent, targetName)
                aab.copyTo(targetFile, overwrite = true)
                println("AAB successfully copied to: ${targetFile.name}")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation("com.cscapp:library-gma:0.0.8")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.config)

    // UI dependencies for startup & ads
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.airbnb.android:lottie:6.4.0")

    // Mediation SDKs matching themeapp
    implementation("com.facebook.android:facebook-android-sdk:18.3.0")
    implementation("com.google.ads.mediation:facebook:6.22.0.0")
    implementation("com.google.ads.mediation:applovin:13.6.4.0")
    implementation("com.google.ads.mediation:inmobi:11.4.0.0")
    implementation("com.google.ads.mediation:pangle:8.2.0.4.0")
    implementation("com.google.ads.mediation:mintegral:17.1.71.0")
    implementation("com.unity3d.ads:unity-ads:4.20.0")
    implementation("com.google.ads.mediation:unity:4.19.0.1")
    implementation("com.pubscale.ads:admob-adapter:1.0.5")


    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Jsoup
    implementation(libs.jsoup)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Unit tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
    exclude(group = "com.google.android.gms", module = "play-services-ads-base")
    exclude(group = "com.google.android.gms", module = "play-services-ads-api")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.2.10")
    }
}


