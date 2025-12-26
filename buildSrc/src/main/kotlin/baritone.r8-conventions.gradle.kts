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
 * R8 obfuscation conventions plugin for Baritone builds.
 *
 * This convention plugin provides:
 * - R8 obfuscation task configuration
 * - Three build variants:
 *   - API: Partial obfuscation (keeps public API)
 *   - Standalone: Full obfuscation (maximum size reduction)
 *   - Unoptimized: No optimization (for debugging)
 * - Integration with remapJar for API variant processing
 * - Automatic variant generation during build
 *
 * Applied to: Forge and Fabric subprojects
 * Dependencies: baritone.loader-conventions
 *
 * The R8 task uses ProGuard configuration files from the project root
 * to control the obfuscation process for each variant.
 */

import baritone.gradle.task.R8Task
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("baritone.loader-conventions")
}

// Access the version catalog
val libs = the<VersionCatalogsExtension>().named("libs")

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
        // Use base.archivesName which is already configured from version catalog in base-conventions
        val baseArchivesName = project.base.archivesName.get()
        val versionString = providers.provider { "${project.name}-${project.version}" }

        outputApiJar.set(layout.buildDirectory.file(
            versionString.map { version ->
                "libs/$baseArchivesName-api-$version.jar"
            }
        ))
        outputStandaloneJar.set(layout.buildDirectory.file(
            versionString.map { version ->
                "libs/$baseArchivesName-standalone-$version.jar"
            }
        ))
        outputUnoptimizedJar.set(layout.buildDirectory.file(
            versionString.map { version ->
                "libs/$baseArchivesName-unoptimized-$version.jar"
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