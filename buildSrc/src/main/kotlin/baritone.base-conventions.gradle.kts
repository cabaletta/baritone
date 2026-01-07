/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.bundling.Jar

plugins {
    java
    id("baritone.publishing-conventions")
}

// Access the version catalog
val libs = the<VersionCatalogsExtension>().named("libs")

// Lazy evaluation for better performance - using providers API
val javaVersion: Int = libs.findVersion("java").map { it.toString().toInt() }.orElse(17)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

// Configuration cache compatible property access using version catalog
group = libs.findVersion("maven-group").map { it.toString() }.orElse("baritone")
version = libs.findVersion("mod-version").map { it.toString() }.orElse("0.0.0")

base {
    archivesName.set(libs.findVersion("archives-base-name").map { it.toString() }.orElse("baritone"))
}

// Configure tasks lazily for better performance
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
    // Add helpful compiler flags
    options.compilerArgs.addAll(listOf(
        "-Xlint:deprecation",
        "-Xlint:unchecked"
    ))
}

// Configure test tasks for better performance
tasks.withType<Test>().configureEach {
    useJUnit()  // Using JUnit 4 since that's what the project uses
    maxHeapSize = "2G"
    jvmArgs("-XX:+UseG1GC")
}

// Reproducible builds configuration
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}