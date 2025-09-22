// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("kotlin_version", "2.0.0")
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.oss.licenses.plugin)
        classpath(libs.kotlin.gradle.plugin)
    }
}

// TODO: fix deprecations and unchecked warnings
allprojects {
    tasks.withType(JavaCompile::class.java) {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

tasks.create<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}