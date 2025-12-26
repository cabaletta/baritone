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

/*
 * Base conventions for all Baritone modules
 * Provides common Java configuration, versioning, and basic setup
 */

import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.bundling.Jar

plugins {
    java
    `maven-publish`
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
    useJUnitPlatform()
    maxHeapSize = "2G"
    jvmArgs("-XX:+UseG1GC")
}

// Reproducible builds configuration
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// Publishing configuration with lazy evaluation
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = base.archivesName.get()

            pom {
                name.set("Baritone")
                description.set("Minecraft pathfinding bot")
                url.set("https://github.com/cabaletta/baritone")

                licenses {
                    license {
                        name.set("LGPL-3.0")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.html")
                    }
                }

                developers {
                    developer {
                        id.set("baritone")
                        name.set("Baritone Team")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/cabaletta/baritone.git")
                    developerConnection.set("scm:git:ssh://github.com/cabaletta/baritone.git")
                    url.set("https://github.com/cabaletta/baritone")
                }
            }
        }
    }
}