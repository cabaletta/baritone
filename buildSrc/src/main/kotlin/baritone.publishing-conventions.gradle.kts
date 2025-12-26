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

/**
 * Publishing conventions for Baritone modules.
 * Centralizes all Maven publishing configuration to ensure consistency across modules.
 */
plugins {
    `maven-publish`
    java
}

// Access the version catalog
val libs = the<VersionCatalogsExtension>().named("libs")

// Publishing configuration with lazy evaluation
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            // Use the archives base name from the project
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

    repositories {
        // Add your repository configuration here if needed
        // For example:
        // maven {
        //     name = "GitHubPackages"
        //     url = uri("https://maven.pkg.github.com/cabaletta/baritone")
        //     credentials {
        //         username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
        //         password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        //     }
        // }
    }
}