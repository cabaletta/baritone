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

package baritone.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * @author Brady
 * @since 10/12/2018
 */
abstract class BaritoneGradleTask @Inject constructor() : DefaultTask() {

    companion object {
        const val ARTIFACT_STANDARD = "%s-%s.jar"
        const val ARTIFACT_UNOPTIMIZED = "%s-unoptimized-%s.jar"
        const val ARTIFACT_API = "%s-api-%s.jar"
        const val ARTIFACT_STANDALONE = "%s-standalone-%s.jar"
    }

    @get:Input
    @get:Optional
    abstract val compType: Property<String>

    @get:Internal
    protected lateinit var artifactName: String
    @get:Internal
    protected lateinit var artifactVersion: String
    @get:Internal
    protected lateinit var artifactPath: Path
    @get:Internal
    protected lateinit var artifactUnoptimizedPath: Path
    @get:Internal
    protected lateinit var artifactApiPath: Path
    @get:Internal
    protected lateinit var artifactStandalonePath: Path

    init {
        // Initialize artifactName from project property
        doFirst {
            artifactName = project.rootProject.property("archives_base_name").toString()
            initializeArtifacts()
        }
    }

    protected fun initializeArtifacts() {
        artifactVersion = if (compType.isPresent && compType.get().isNotEmpty()) {
            "${compType.get()}-${project.version}"
        } else {
            project.version.toString()
        }

        artifactPath = getBuildFile(formatVersion(ARTIFACT_STANDARD))
        artifactUnoptimizedPath = getBuildFile(formatVersion(ARTIFACT_UNOPTIMIZED))
        artifactApiPath = getBuildFile(formatVersion(ARTIFACT_API))
        artifactStandalonePath = getBuildFile(formatVersion(ARTIFACT_STANDALONE))
    }

    @Throws(IllegalStateException::class)
    protected fun verifyArtifacts() {
        if (!artifactPath.exists()) {
            throw IllegalStateException("Artifact not found! Run build first! Missing file: $artifactPath")
        }
    }

    @Throws(IOException::class)
    protected fun write(stream: InputStream, file: Path) {
        file.deleteIfExists()
        Files.copy(stream, file)
    }

    protected fun formatVersion(string: String): String {
        return String.format(string, artifactName, artifactVersion)
    }

    protected fun getRelativeFile(file: String): Path {
        return project.layout.buildDirectory.file(file).get().asFile.toPath()
    }

    protected fun getRootRelativeFile(file: String): Path {
        return Paths.get(File(project.rootDir, file).absolutePath)
    }

    protected fun getTemporaryFile(file: String): Path {
        return Paths.get(File(temporaryDir, file).absolutePath)
    }

    protected fun getBuildFile(file: String): Path {
        return getRelativeFile("libs/$file")
    }

    protected fun addCompTypeFirst(string: String): String {
        return if (compType.isPresent && compType.get().isNotEmpty()) {
            "${compType.get()}-$string"
        } else {
            string
        }
    }
}