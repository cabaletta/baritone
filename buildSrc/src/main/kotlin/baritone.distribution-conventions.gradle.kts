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
 * Distribution conventions for creating final artifacts
 * Supports R8 obfuscation variants when enabled
 */

import org.gradle.kotlin.dsl.*
import org.gradle.api.artifacts.VersionCatalogsExtension

// R8 always produces all three variants

tasks {
    register<Copy>("createDist") {
        description = "Creates distribution artifacts"
        group = "distribution"

        // Include the remapped API variant
        from(tasks.named<Jar>("remapJar"))

        // Include the R8 Standalone and Unoptimized variants directly (they don't need remapping)
        val r8Task = tasks.named("r8")
        dependsOn(r8Task)
        dependsOn("remapJar")

        // Copy the standalone and unoptimized JARs from R8 output
        doFirst {
            // Get the R8 outputs and copy them with proper names
            val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
            val baseArchivesName = libs.findVersion("archives-base-name").get().toString()
            val projectName = project.name
            val compTypeValue = when (projectName) {
                "fabric", "forge", "tweaker", "neoforge" -> projectName
                else -> null
            }
            val versionString = if (compTypeValue != null) "$compTypeValue-${project.version}" else project.version.toString()

            // Ensure dist directory exists
            val distDir = layout.buildDirectory.dir("dist").get().asFile
            if (!distDir.exists()) {
                distDir.mkdirs()
            }

            // Copy standalone JAR with error handling
            val standaloneSource = layout.buildDirectory.file("libs/$baseArchivesName-standalone-$versionString.jar").get().asFile
            val standaloneDest = distDir.resolve("$baseArchivesName-standalone-$versionString.jar")
            if (standaloneSource.exists()) {
                try {
                    standaloneSource.copyTo(standaloneDest, overwrite = true)
                } catch (e: Exception) {
                    logger.warn("Failed to copy standalone JAR: ${e.message}")
                }
            } else {
                logger.debug("Standalone JAR not found: $standaloneSource")
            }

            // Copy unoptimized JAR with error handling
            val unoptimizedSource = layout.buildDirectory.file("libs/$baseArchivesName-unoptimized-$versionString.jar").get().asFile
            val unoptimizedDest = distDir.resolve("$baseArchivesName-unoptimized-$versionString.jar")
            if (unoptimizedSource.exists()) {
                try {
                    unoptimizedSource.copyTo(unoptimizedDest, overwrite = true)
                } catch (e: Exception) {
                    logger.warn("Failed to copy unoptimized JAR: ${e.message}")
                }
            } else {
                logger.debug("Unoptimized JAR not found: $unoptimizedSource")
            }
        }

        into(layout.buildDirectory.dir("dist"))

        doLast {
            val distDir = layout.buildDirectory.dir("dist").get()
            println("Distribution created in: $distDir")

            // List the created artifacts with null safety
            val files = distDir.asFile.listFiles()
            if (files != null && files.isNotEmpty()) {
                files.forEach { file ->
                    println("  - ${file.name}")
                }
            } else {
                logger.warn("No distribution files found in: $distDir")
            }
        }
    }

    named("build") {
        finalizedBy("createDist")
    }
}