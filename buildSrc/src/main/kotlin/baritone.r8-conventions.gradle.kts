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
 * R8 conventions for obfuscating Baritone builds
 * Creates three variants: API (partial obfuscation), Standalone (full obfuscation), and Unoptimized
 */

import baritone.gradle.task.R8Task
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("baritone.loader-conventions")
}

// R8 always runs to produce the three variants
// The unobfuscated variant doesn't use R8, just the Determinizer
logger.lifecycle("R8 task registered for ${project.name} - will produce API, Standalone, and Unobfuscated variants")

tasks {
    // Register R8 task
    val r8 by registering(R8Task::class) {
        description = "Runs R8 obfuscation to create API and Standalone variants"
        group = "build"

        // Set component type based on project
        val projectName = project.name
        when (projectName) {
            "fabric", "forge", "tweaker", "neoforge" -> compType.set(projectName)
        }

        // Input is the shadow JAR
        inputJar.set(named<ShadowJar>("shadowJar").flatMap { it.archiveFile })

        // Set output locations
        val baseArchivesName = project.rootProject.property("archives_base_name").toString()
        val compTypeValue = when (projectName) {
            "fabric", "forge", "tweaker", "neoforge" -> projectName
            else -> null
        }
        val versionString = if (compTypeValue != null) "$compTypeValue-${project.version}" else project.version.toString()

        outputApiJar.set(layout.buildDirectory.file("libs/$baseArchivesName-api-$versionString.jar"))
        outputStandaloneJar.set(layout.buildDirectory.file("libs/$baseArchivesName-standalone-$versionString.jar"))
        outputUnoptimizedJar.set(layout.buildDirectory.file("libs/$baseArchivesName-unoptimized-$versionString.jar"))

        dependsOn("shadowJar")
    }

    // Modify the remapJar task to use R8 output (API variant only)
    afterEvaluate {
        tasks.named<RemapJarTask>("remapJar") {
            dependsOn(r8)

            // Use the API variant as input for remapping
            inputFile.set(r8.get().outputApiJar)
        }
    }

    // Make build depend on R8 to ensure all variants are created
    named("build") {
        dependsOn(r8)
    }
}