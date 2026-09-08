import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// El plugin de Google Services necesita su google-services.json (la
// configuracion del proyecto de Firebase) para poder correr. Si todavia no
// esta (ver docs/MULTIJUGADOR.md), el plugin no se aplica: el proyecto
// compila igual, nomas que TransporteFirebase no tiene con que conectarse.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.mggx.laberinto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mggx.laberinto"
        minSdk = 24
        targetSdk = 34
        // El versionCode tiene que subir en cada publicacion: Android no deja
        // instalar encima de una version con el mismo numero o mayor.
        versionCode = 9
        versionName = "1.5.1"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/mggx-release.jks")
            storePassword = "MggxLaberinto2026"
            keyAlias = "mggx"
            keyPassword = "MggxLaberinto2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    // El relay de multijugador (net/TransporteFirebase.kt). Con el BOM no
    // hace falta poner version en cada libreria de Firebase por separado.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    // org.json real: en los tests de JVM el de Android es solo un stub vacio.
    testImplementation(libs.org.json)
}

/**
 * Valida los shaders GLSL antes de compilar. Un shader roto no da error de
 * compilacion en Kotlin: revienta recien al abrir el nivel en el telefono.
 * Si la maquina no tiene glslangValidator o python3, la tarea se saltea sola.
 */
val checkShaders by tasks.registering {
    val script = rootProject.file("tools/check_shaders.py")
    inputs.file(rootProject.file("app/src/main/java/com/mggx/laberinto/gl/Shaders.kt"))
    inputs.file(script)
    outputs.upToDateWhen { false }
    doLast {
        fun disponible(cmd: String): Boolean = try {
            providers.exec {
                commandLine("sh", "-c", "command -v $cmd")
                isIgnoreExitValue = true
            }.result.get().exitValue == 0
        } catch (t: Throwable) { false }

        if (!script.exists() || !disponible("python3") || !disponible("glslangValidator")) {
            logger.lifecycle("checkShaders: se saltea (falta python3 o glslangValidator)")
            return@doLast
        }
        val r = providers.exec {
            workingDir = rootProject.projectDir
            commandLine("python3", script.absolutePath)
            isIgnoreExitValue = true
        }
        val salida = r.standardOutput.asText.get() + r.standardError.asText.get()
        if (r.result.get().exitValue != 0) {
            throw GradleException("Hay shaders GLSL con errores:\n$salida")
        }
        logger.lifecycle(salida.trim().lines().last())
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(checkShaders) }

/**
 * Arma la version de debug del google-services.json.
 *
 * La app de debug se instala con el applicationId terminado en ".debug" (ver
 * el buildType de arriba), justamente para poder tener la de prueba y la
 * definitiva instaladas al mismo tiempo. Pero el archivo que baja la consola
 * de Firebase trae registrado un solo package, el de la version definitiva, y
 * el plugin de Google corta el build si no encuentra el que le corresponde a
 * la variante que esta compilando.
 *
 * En vez de tener que registrar una segunda app en la consola, se genera aca
 * la copia para debug: mismo proyecto, misma base de datos, con el package
 * name que el plugin espera. Al ser el mismo proyecto, las dos versiones
 * comparten las salas, que es lo que uno quiere para probar.
 */
val googleServicesDebug by tasks.registering {
    val origen = file("google-services.json")
    val destino = file("src/debug/google-services.json")
    onlyIf { origen.exists() }
    inputs.files(origen)
    outputs.file(destino)
    doLast {
        destino.parentFile.mkdirs()
        destino.writeText(
            origen.readText().replace(
                "\"package_name\": \"com.mggx.laberinto\"",
                "\"package_name\": \"com.mggx.laberinto.debug\""
            )
        )
    }
}

tasks.matching { it.name == "processDebugGoogleServices" }
    .configureEach { dependsOn(googleServicesDebug) }
