import java.io.FileInputStream
import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.12"
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.watering.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.watering.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // mockk-android가 끌어오는 JUnit 5(Jupiter) 계열 전이 의존성들이 androidTest APK 패키징 시
    // 서로 같은 META-INF 라이선스 파일을 중복으로 갖고 있어 충돌 — 실행에 영향 없는 문서성
    // 파일이라 안전하게 제외
    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    // Java 8+ API desugaring (LocalDate 등 사용)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.splashscreen)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    // Material Components (XML 테마용)
    implementation(libs.material)

    // Glance (위젯)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt (DI)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.workmanager.ktx)

    // Google Play Billing
    implementation(libs.billing.ktx)

    // Google Play In-App Review
    implementation(libs.review.ktx)

    // Health Connect
    implementation(libs.health.connect)

    // Firebase (Crashlytics + Analytics + Auth + Firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    group = "Reporting"
    description = "Service/Repository/ViewModel 레이어 커버리지 리포트 생성 (HTML+XML)"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // 분모 = core/** (model/data/datastore/service) + features/**의 ViewModel만.
    // UI(Compose 화면)·widget(Glance)·navigation·di는 애초에 테스트 대상이 아니라 제외 —
    // 기획서 "Service 레이어 80%" 목표와 직접 비교 가능한 숫자를 만들기 위한 의도적 스코프.
    val businessLogicIncludes = listOf(
        "**/core/**",
        "**/features/**/*ViewModel.class",
        "**/features/**/*ViewModel\$*.class"
    )

    // Hilt/Dagger·Compose 컴파일러·kotlinx.serialization이 생성한, 손으로 짠 로직이 없는
    // 보일러플레이트 — 위 include 범위(core/**) 안에도 *_Factory 등이 섞여 있어 별도 제외 필요.
    val generatedCodeExcludes = listOf(
        "**/Hilt_*.class",
        "**/*_HiltModules*.class",
        "**/*_Factory.class",
        "**/*_MembersInjector.class",
        "**/*_GeneratedInjector.class",
        "**/*_AssistedFactory.class",
        "**/*_AssistedFactory_Impl.class",
        "**/*_HiltModule.class",
        "**/Dagger*.class",
        "**/*\$serializer.class",
        "**/*\$\$serializer.class",
        "**/BuildConfig.class"
    )

    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include(businessLogicIncludes)
        exclude(generatedCodeExcludes)
    }

    classDirectories.setFrom(kotlinClasses)
    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("jacoco/testDebugUnitTest.exec")
        }
    )
}
