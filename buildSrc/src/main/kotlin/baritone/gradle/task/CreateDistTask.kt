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

import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.io.path.*

/**
 * @author Brady
 * @since 10/12/2018
 */
abstract class CreateDistTask @Inject constructor() : BaritoneGradleTask() {

    companion object {
        @Volatile
        private var SHA1_DIGEST: MessageDigest? = null

        private val HEX_ARRAY = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)

        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = ByteArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = HEX_ARRAY[v ushr 4]
                hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
            }
            return String(hexChars, StandardCharsets.UTF_8)
        }

        @Synchronized
        private fun sha1(path: Path): String {
            try {
                if (SHA1_DIGEST == null) {
                    SHA1_DIGEST = MessageDigest.getInstance("SHA-1")
                }
                return bytesToHex(SHA1_DIGEST!!.digest(path.readBytes())).lowercase()
            } catch (e: Exception) {
                // haha no thanks
                throw IllegalStateException(e)
            }
        }

        private fun getFileName(p: Path): String {
            return p.fileName.toString()
        }
    }

    init {
        group = "build"
        description = "Creates the distribution artifacts and generates checksums"
    }

    @TaskAction
    fun exec() {
        // Initialize artifacts first
        artifactName = project.rootProject.extensions.getByType(BasePluginExtension::class.java).archivesName.get()
        initializeArtifacts()
        verifyArtifacts()

        // Define the distribution file paths
        val api = getRootRelativeFile("dist/${getFileName(artifactApiPath)}")
        val standalone = getRootRelativeFile("dist/${getFileName(artifactStandalonePath)}")
        val unoptimized = getRootRelativeFile("dist/${getFileName(artifactUnoptimizedPath)}")

        // NIO will not automatically create directories
        val dir = getRootRelativeFile("dist/")
        if (!dir.exists()) {
            Files.createDirectory(dir)
        }

        // Copy build jars to dist/
        // TODO: dont copy files that dont exist
        if (artifactApiPath.exists()) {
            Files.copy(artifactApiPath, api, REPLACE_EXISTING)
        }
        if (artifactStandalonePath.exists()) {
            Files.copy(artifactStandalonePath, standalone, REPLACE_EXISTING)
        }
        if (artifactUnoptimizedPath.exists()) {
            Files.copy(artifactUnoptimizedPath, unoptimized, REPLACE_EXISTING)
        }

        // Calculate all checksums and format them like "shasum"
        val shasum = Files.list(getRootRelativeFile("dist/"))
            .filter { e -> e.fileName.toString().endsWith(".jar") }
            .map { path -> "${sha1(path)}  ${path.fileName}" }
            .toList()

        shasum.forEach { println(it) }

        // Write the checksums to a file
        Files.write(getRootRelativeFile("dist/checksums.txt"), shasum)
    }
}