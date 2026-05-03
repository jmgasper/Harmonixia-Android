import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

abstract class RenameApkTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputApkDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputApkDir: DirectoryProperty

    @get:Internal
    abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameApkTask>>

    @TaskAction
    fun run() {
        val inputDir = inputApkDir.get().asFile
        val outputDir = outputApkDir.get().asFile
        outputDir.mkdirs()
        val renameToFixedName = hasSingleOutput(inputDir)

        transformationRequest.get().submit(this) { builtArtifact ->
            val inputFile = File(builtArtifact.outputFile)
            val outputFile = if (renameToFixedName) {
                File(outputDir, TARGET_APK_NAME)
            } else {
                val relative = runCatching { inputFile.relativeTo(inputDir) }.getOrNull()
                if (relative != null) {
                    outputDir.resolve(relative.path)
                } else {
                    File(outputDir, inputFile.name)
                }
            }
            outputFile.parentFile?.mkdirs()
            inputFile.copyTo(outputFile, overwrite = true)
            outputFile
        }
    }

    private fun hasSingleOutput(inputDir: File): Boolean {
        val metadataFile = inputDir.resolve(APK_METADATA_FILE)
        if (!metadataFile.exists()) {
            return false
        }
        val matches = OUTPUT_FILE_REGEX.findAll(metadataFile.readText()).toList()
        return matches.size == 1
    }

    private companion object {
        private const val APK_METADATA_FILE = "output-metadata.json"
        private const val TARGET_APK_NAME = "Harmonixia.apk"
        private val OUTPUT_FILE_REGEX = Regex("\"outputFile\"\\s*:\\s*\"([^\"]+)\"")
    }
}

plugins {
    id("com.android.application")
    id("com.android.legacy-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.harmonixia.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.harmonixia.android"
        minSdk = 30
        targetSdk = 36
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add(
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi"
        )
    }
}

kapt {
    correctErrorTypes = true
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val taskName = "rename${variant.name.replaceFirstChar { it.uppercaseChar() }}Apk"
        val renameTask = tasks.register<RenameApkTask>(taskName)
        val transformRequest = variant.artifacts.use(renameTask)
            .wiredWithDirectories(RenameApkTask::inputApkDir, RenameApkTask::outputApkDir)
            .toTransformMany(SingleArtifact.APK)
        renameTask.configure {
            outputApkDir.set(layout.buildDirectory.dir("outputs/apk/${variant.name}"))
            transformationRequest.set(transformRequest)
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    if (name == "bundleRelease") {
        archiveFileName.set("Harmonixia.aab")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.documentfile:documentfile:1.1.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
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
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.4.2")
    implementation("androidx.paging:paging-compose:3.4.2")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.59.2")
    kapt("com.google.dagger:hilt-compiler:2.59.2")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.security:security-crypto:1.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    // OkHttp for WebSocket
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // Media3 for playback
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-session:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-common:1.10.0")

    // Coil for image loading
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    implementation("io.coil-kt.coil3:coil-svg:3.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
