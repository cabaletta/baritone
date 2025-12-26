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

tasks {
    // Register R8 task with lazy configuration
    val r8 by registering(R8Task::class) {
        description = "Runs R8 obfuscation to create API and Standalone variants"
        group = "build"

        // Lazy property evaluation
        compType.set(providers.provider { project.name })

        // Input is the shadow JAR
        inputJar.set(named<ShadowJar>("shadowJar").flatMap { it.archiveFile })

        // Lazy output file configuration
        val baseArchivesName = providers.gradleProperty("archives_base_name")
        val versionString = providers.provider { "${project.name}-${project.version}" }

        outputApiJar.set(layout.buildDirectory.file(
            providers.zip(baseArchivesName, versionString) { base, version ->
                "libs/$base-api-$version.jar"
            }
        ))
        outputStandaloneJar.set(layout.buildDirectory.file(
            providers.zip(baseArchivesName, versionString) { base, version ->
                "libs/$base-standalone-$version.jar"
            }
        ))
        outputUnoptimizedJar.set(layout.buildDirectory.file(
            providers.zip(baseArchivesName, versionString) { base, version ->
                "libs/$base-unoptimized-$version.jar"
            }
        ))

        dependsOn("shadowJar")
    }

    // Configuration cache compatible - configure remapJar task lazily
    withType<RemapJarTask>().configureEach {
        dependsOn(r8)
        inputFile.set(r8.flatMap { it.outputApiJar })
    }

    // Make build depend on R8 to ensure all variants are created
    named("build") {
        dependsOn(r8)
    }
}