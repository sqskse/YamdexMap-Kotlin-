plugins {
    id("com.android.application") version "9.5.1" apply false
    // AGP 9 has built-in Kotlin support: the Kotlin plugin must be on the
    // buildscript classpath, but it must NOT be applied to the Android module.
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
}

// The workspace path contains non-ASCII characters (Cyrillic). Kotlin/Gradle
// workers on Windows can fail when such a path ends up in a classpath argfile,
// so keep build outputs in an ASCII temporary directory.
val asciiBuildRoot = providers.gradleProperty("app.buildDir").orNull
    ?: if (rootDir.path.any { it.code > 127 }) {
        "${System.getProperty("java.io.tmpdir")}/yandex-maps-app-build"
    } else {
        null
    }

asciiBuildRoot?.let { base ->
    allprojects {
        layout.buildDirectory.set(file("$base/${project.name}"))
    }
}
