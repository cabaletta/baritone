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
import java.security.MessageDigest

tasks {
    // Clean task for old distribution artifacts
    val cleanDist by registering(Delete::class) {
        description = "Removes old builds from dist directory"
        group = "distribution"
        delete(layout.buildDirectory.dir("dist"))
    }

    // Modern distribution task using Sync for better performance
    val createDist by registering(Sync::class) {
        description = "Creates distribution artifacts"
        group = "distribution"

        // Include the remapped API variant
        from(tasks.named<Jar>("remapJar"))

        // Depend on R8 task to ensure all variants are created
        val r8Task = tasks.named("r8")
        dependsOn(cleanDist, r8Task, "remapJar")

        // Use providers for lazy evaluation of file locations
        from(providers.provider {
            fileTree(layout.buildDirectory.dir("libs")) {
                include("*-standalone-*.jar")
                include("*-unoptimized-*.jar")
            }
        })

        into(layout.buildDirectory.dir("dist"))

        doLast {
            val distDir = layout.buildDirectory.dir("dist").get()
            logger.lifecycle("Distribution created in: $distDir")

            // List created artifacts with their sizes
            distDir.asFile.listFiles()?.forEach { file ->
                val sizeKb = file.length() / 1024
                logger.lifecycle("  - ${file.name} (${sizeKb}KB)")
            } ?: logger.warn("No distribution files found in: $distDir")
        }
    }

    // Add verification task to ensure all expected artifacts exist
    val verifyDist by registering {
        dependsOn(createDist)
        doLast {
            val expectedVariants = listOf("api", "standalone", "unoptimized")
            val distDir = layout.buildDirectory.dir("dist").get().asFile

            expectedVariants.forEach { variant ->
                val hasVariant = distDir.listFiles()?.any {
                    it.name.contains(variant)
                } == true

                if (!hasVariant) {
                    throw GradleException("Missing $variant artifact in distribution")
                }
            }

            logger.lifecycle("Distribution verification passed - all variants present")
        }
    }

    // Generate checksums for distribution artifacts
    val generateChecksums by registering {
        dependsOn(createDist)
        group = "distribution"
        description = "Generates SHA-256 and MD5 checksums for distribution artifacts"

        doLast {
            val distDir = layout.buildDirectory.dir("dist").get().asFile

            distDir.listFiles { file -> file.name.endsWith(".jar") }?.forEach { jarFile ->
                // Generate SHA-256
                val sha256 = MessageDigest.getInstance("SHA-256")
                jarFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        sha256.update(buffer, 0, read)
                    }
                }
                val sha256Hex = sha256.digest().joinToString("") { "%02x".format(it) }

                // Generate MD5
                val md5 = MessageDigest.getInstance("MD5")
                jarFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        md5.update(buffer, 0, read)
                    }
                }
                val md5Hex = md5.digest().joinToString("") { "%02x".format(it) }

                // Write checksum files
                File(distDir, "${jarFile.name}.sha256").writeText("$sha256Hex  ${jarFile.name}\n")
                File(distDir, "${jarFile.name}.md5").writeText("$md5Hex  ${jarFile.name}\n")

                logger.lifecycle("Generated checksums for ${jarFile.name}")
                logger.lifecycle("  SHA-256: $sha256Hex")
                logger.lifecycle("  MD5: $md5Hex")
            }
        }
    }

    named("build") {
        finalizedBy(createDist)
        finalizedBy(generateChecksums)
    }

    // Don't add to check task to avoid circular dependency
    // Instead, create a separate verification task that can be called explicitly
    register("verifyDistribution") {
        dependsOn("build", verifyDist)
        group = "verification"
        description = "Builds and verifies distribution artifacts"
    }
}