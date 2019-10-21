plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    target {
        browser()
        val isIrBackend = project.findProperty("kotlin.js.useIrBackend")?.toString()?.toBoolean() ?: false
        if (isIrBackend) {
            compilations["main"].kotlinOptions.freeCompilerArgs += listOf("-Xir-produce-klib-dir", "-Xir-only")
            compilations["test"].kotlinOptions.freeCompilerArgs += "-Xir-produce-js"
        }
    }

    sourceSets {
        getByName("main") {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        getByName("test") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}