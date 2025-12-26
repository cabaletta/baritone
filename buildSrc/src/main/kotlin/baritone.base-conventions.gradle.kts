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

// Lazy evaluation for better performance - using providers API
val libs: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
val javaVersion: Int = providers.gradleProperty("java_version").map { it.toInt() }.get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

// Configuration cache compatible property access
group = providers.gradleProperty("maven_group").getOrElse("baritone")
version = providers.gradleProperty("mod_version").getOrElse("0.0.0")

base {
    archivesName.set(providers.gradleProperty("archives_base_name").getOrElse("baritone"))
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