import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File

abstract class RenameApkTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputApkDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputApkDir: DirectoryProperty

    @TaskAction
    fun run() {
        val inputDir = inputApkDir.get().asFile
        val outputDir = outputApkDir.get().asFile
        outputDir.mkdirs()
        val metadataFile = inputDir.resolve(APK_METADATA_FILE)
        val (originalApkName, updatedMetadata) = if (metadataFile.exists()) {
            val text = metadataFile.readText()
            val matches = OUTPUT_FILE_REGEX.findAll(text).toList()
            if (matches.size == 1) {
                val original = matches.first().groupValues[1]
                original to OUTPUT_FILE_REGEX.replace(text, "\"outputFile\": \"$TARGET_APK_NAME\"")
            } else {
                null to null
            }
        } else {
            null to null
        }

        inputDir.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            val relative = file.relativeTo(inputDir)
            val renamedRelative = if (originalApkName != null && file.name == originalApkName) {
                val parent = relative.parentFile
                if (parent != null) {
                    parent.resolve(TARGET_APK_NAME)
                } else {
                    File(TARGET_APK_NAME)
                }
            } else {
                relative
            }
            val targetFile = outputDir.resolve(renamedRelative.path)
            targetFile.parentFile?.mkdirs()
            if (file == metadataFile && updatedMetadata != null) {
                targetFile.writeText(updatedMetadata)
            } else {
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }

    private companion object {
        private const val APK_METADATA_FILE = "output-metadata.json"
        private const val TARGET_APK_NAME = "Harmonixia.apk"
        private val OUTPUT_FILE_REGEX = Regex("\"outputFile\"\\s*:\\s*\"([^\"]+)\"")
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.harmonixia.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.harmonixia.android"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            ?: (project.findProperty("ANDROID_KEYSTORE_PATH") as String?)
        val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            ?: (project.findProperty("ANDROID_KEYSTORE_PASSWORD") as String?)
        val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS")
            ?: (project.findProperty("ANDROID_KEY_ALIAS") as String?)
        val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD")
            ?: (project.findProperty("ANDROID_KEY_PASSWORD") as String?)

        if (!keystorePath.isNullOrBlank()
            && !keystorePassword.isNullOrBlank()
            && !keyAliasValue.isNullOrBlank()
            && !keyPasswordValue.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi"
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

kapt {
    correctErrorTypes = true
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val taskName = "rename${variant.name.replaceFirstChar { it.uppercaseChar() }}Apk"
        val renameTask = tasks.register<RenameApkTask>(taskName) {
            outputApkDir.set(layout.buildDirectory.dir("outputs/apk/${variant.name}"))
        }
        variant.artifacts.use(renameTask)
            .wiredWithDirectories(RenameApkTask::inputApkDir, RenameApkTask::outputApkDir)
            .toTransformMany(SingleArtifact.APK)
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    if (name == "bundleRelease") {
        archiveFileName.set("Harmonixia.aab")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Paging
    implementation("androidx.paging:paging-runtime:3.3.5")
    implementation("androidx.paging:paging-compose:3.3.5")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-compiler:2.57.2")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.6")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // OkHttp for WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Media3 for playback
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-session:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-common:1.9.0")

    // Coil for image loading
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.coil-kt.coil3:coil-svg:3.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
