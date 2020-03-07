plugins {
    kotlin("multiplatform") version "1.3.61"
}

repositories {
    mavenCentral()
    jcenter()
}

kotlin {

    macosX64("macos") {
        sourceSets["macosMain"].kotlin.srcDir("src")
        sourceSets["macosTest"].kotlin.srcDir("test")

        binaries {
            executable(buildTypes = setOf(DEBUG)) {
                entryPoint = "com.jcthenerd.personal.terminal.main"
            }
        }

        val main by compilations.getting
        val interop by main.cinterops.creating {
            defFile(project.file("ncurses.def"))
            packageName("ncurses")
            includeDirs(
                    "/usr/local/Cellar/ncurses/6.2/include/",
                    "/usr/local/Cellar/ncurses/6.2/include/ncursesw"
            )
        }

        val gitInterop by main.cinterops.creating {
            defFile(project.file("libgit2.def"))
            packageName("git2")
            includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
        }
    }

    sourceSets {
        val macosMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("stdlib"))
                implementation("com.soywiz.korlibs.klock:klock-macosx64:1.8.2")
                implementation("io.ktor:ktor-client-core:1.3.1")
                implementation("io.ktor:ktor-client-curl:1.3.1")
                implementation("io.ktor:ktor-client-json-native:1.3.1")
                implementation("io.ktor:ktor-client-serialization-native:1.3.1")
            }

            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }
    }
}