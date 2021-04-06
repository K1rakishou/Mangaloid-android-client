plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
}

android {
  compileSdk = 30
  buildToolsVersion = "30.0.3"

  defaultConfig {
    applicationId = "com.github.mangaloid.client"
    minSdk = 21
    targetSdk = 30
    versionCode = 2
    versionName = "v0.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    applicationVariants.all {
      outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "Mangaloid.apk"
      }
    }

    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
    useIR = true
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = rootProject.extra["compose_version"] as String
    kotlinCompilerVersion = "1.4.31"
  }
}

dependencies {

  implementation("androidx.core:core-ktx:1.3.2")
  implementation("androidx.appcompat:appcompat:1.2.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
  implementation("androidx.compose.ui:ui:${rootProject.extra["compose_version"]}")
  implementation("androidx.compose.material:material:${rootProject.extra["compose_version"]}")
  implementation("androidx.compose.ui:ui-tooling:${rootProject.extra["compose_version"]}")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha03")
  implementation("androidx.activity:activity-compose:1.3.0-alpha05")
  implementation("androidx.navigation:navigation-compose:1.0.0-alpha09")

  implementation("com.google.android.material:material:1.3.0")
  implementation("com.google.accompanist:accompanist-pager:0.7.0")
  implementation("com.google.accompanist:accompanist-coil:0.7.0")
  implementation("com.squareup.okhttp3:okhttp:4.9.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
  implementation("com.google.code.findbugs:jsr305:3.0.2")
  implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
  implementation("dev.chrisbanes.accompanist:accompanist-insets:0.6.2")
  implementation("joda-time:joda-time:2.10.10")

  testImplementation("junit:junit:4.+")
  androidTestImplementation("androidx.test.ext:junit:1.1.2")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:${rootProject.extra["compose_version"]}")
}