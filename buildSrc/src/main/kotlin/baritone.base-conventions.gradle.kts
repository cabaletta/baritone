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

plugins {
    java
    `maven-publish`
}

// Access the version catalog
val versionCatalog = extensions.findByType<VersionCatalogsExtension>()?.named("libs")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

// Common configuration
group = "baritone"
base.archivesName.set("baritone")

// Get version from gradle.properties or use default
version = project.findProperty("mod_version") ?: "0.0.0"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = base.archivesName.get()
        }
    }
}