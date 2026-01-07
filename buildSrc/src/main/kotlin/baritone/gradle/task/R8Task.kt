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

import baritone.gradle.util.Determinizer
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.R8
import com.android.tools.r8.R8Command
import com.android.tools.r8.OutputMode
import com.android.tools.r8.JdkClassFileProvider
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import xyz.wagyourtail.unimined.api.UniminedExtension
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import kotlin.io.path.*

/**
 * R8 task for obfuscating Baritone builds
 * Uses R8 for code shrinking, optimization and obfuscation
 */
abstract class R8Task @Inject constructor() : BaritoneGradleTask() {

    companion object {
        // R8 configuration file names
        const val R8_API_CONFIG_TEMPLATE = "scripts/r8-api.pro"
        const val R8_STANDALONE_CONFIG_TEMPLATE = "scripts/r8-standalone.pro"
        const val R8_MAPPING_DIR = "mapping"
    }

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputApiJar: RegularFileProperty

    @get:OutputFile
    abstract val outputStandaloneJar: RegularFileProperty

    @get:OutputFile
    abstract val outputUnoptimizedJar: RegularFileProperty

    init {
        group = "build"
        description = "Runs R8 obfuscation on the JAR file"
    }

    @TaskAction
    fun execute() {
        // Initialize base class properties first
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        artifactName = libs.findVersion("archives-base-name").get().toString()
        initializeArtifacts()

        // Set paths from the configured properties
        initializePaths()
        verifyArtifacts()

        // Execute R8 pipeline
        generateConfigs()
        processArtifact()
        r8Api()
        r8Standalone()
    }

    private fun initializePaths() {
        // Use the input/output properties to set the artifact paths
        artifactPath = inputJar.get().asFile.toPath()
        artifactUnoptimizedPath = outputUnoptimizedJar.get().asFile.toPath()
        artifactApiPath = outputApiJar.get().asFile.toPath()
        artifactStandalonePath = outputStandaloneJar.get().asFile.toPath()
    }

    // verifyArtifacts is inherited from BaritoneGradleTask

    private fun generateConfigs() {
        logger.lifecycle("Preparing R8 configurations...")

        // Create mapping directory
        Files.createDirectories(getRootRelativeFile(R8_MAPPING_DIR))

        val compTypePrefix = compType.orElse("").get()

        // Prepare API config (keeps API package untouched)
        val apiConfigSource = getRootRelativeFile(R8_API_CONFIG_TEMPLATE)
        if (!apiConfigSource.exists()) {
            throw IllegalStateException("R8 API configuration file not found! Expected $apiConfigSource")
        }
        val apiConfig = Files.readAllLines(apiConfigSource).toMutableList()
        val apiMappingName = if (compTypePrefix.isEmpty()) "mappings-api.txt" else "$compTypePrefix-mappings-api.txt"
        apiConfig.add(0, "-printmapping ${getRootRelativeFile(R8_MAPPING_DIR).resolve(apiMappingName)}")
        val apiConfigPath = getTemporaryFile("${compTypePrefix}api-r8.pro")
        Files.write(apiConfigPath, apiConfig)

        // Prepare Standalone config (obfuscates everything)
        val standaloneConfigSource = getRootRelativeFile(R8_STANDALONE_CONFIG_TEMPLATE)
        if (!standaloneConfigSource.exists()) {
            throw IllegalStateException("R8 Standalone configuration file not found! Expected $standaloneConfigSource")
        }
        val standaloneConfig = Files.readAllLines(standaloneConfigSource).toMutableList()
        val standaloneMappingName = if (compTypePrefix.isEmpty()) "mappings-standalone.txt" else "$compTypePrefix-mappings-standalone.txt"
        standaloneConfig.add(0, "-printmapping ${getRootRelativeFile(R8_MAPPING_DIR).resolve(standaloneMappingName)}")
        val standaloneConfigPath = getTemporaryFile("${compTypePrefix}standalone-r8.pro")
        Files.write(standaloneConfigPath, standaloneConfig)
    }


    private fun processArtifact() {
        logger.lifecycle("Creating unobfuscated artifact (no R8 processing)...")
        if (artifactUnoptimizedPath.exists()) {
            artifactUnoptimizedPath.deleteIfExists()
        }
        // Just determinize the original JAR without any obfuscation
        Determinizer.determinize(
            artifactPath.toString(),
            artifactUnoptimizedPath.toString(),
            emptyList(),
            false
        )
    }

    private fun r8Api() {
        logger.lifecycle("Running R8 for API variant (main obfuscated, API untouched)...")
        val compTypePrefix = compType.orElse("").get()
        val configPath = getTemporaryFile("${compTypePrefix}api-r8.pro")
        val outputPath = getTemporaryFile("api-temp.jar")

        runR8(configPath, artifactPath, outputPath, CompilationMode.RELEASE)

        // Determinize the output
        Determinizer.determinize(
            outputPath.toString(),
            artifactApiPath.toString(),
            emptyList(),
            false
        )

        // Clean up temp file
        outputPath.deleteIfExists()
    }

    private fun r8Standalone() {
        logger.lifecycle("Running R8 for Standalone variant (everything obfuscated)...")
        val compTypePrefix = compType.orElse("").get()
        val configPath = getTemporaryFile("${compTypePrefix}standalone-r8.pro")
        val outputPath = getTemporaryFile("standalone-temp.jar")

        runR8(configPath, artifactPath, outputPath, CompilationMode.RELEASE)

        // Determinize the output
        Determinizer.determinize(
            outputPath.toString(),
            artifactStandalonePath.toString(),
            emptyList(),
            false
        )

        // Clean up temp file
        outputPath.deleteIfExists()
    }

    private fun runR8(configPath: Path, inputJar: Path, outputJar: Path, mode: CompilationMode) {
        // Delete existing output
        outputJar.deleteIfExists()

        // Build R8 command
        val commandBuilder = R8Command.builder()
            .setMode(mode)
            .setOutput(outputJar, OutputMode.ClassFile)
            .addProgramFiles(inputJar)
            // Don't set minApiLevel for class file output

        // Add dependencies as library files
        val dependencies = acquireDependencies()
        dependencies.forEach { dep ->
            if (dep.exists() && dep.extension == "jar") {
                commandBuilder.addLibraryFiles(dep.toPath())
            }
        }

        // Add JDK libraries
        try {
            val javaHome = System.getProperty("java.home")
            commandBuilder.addLibraryResourceProvider(JdkClassFileProvider.fromJdkHome(Paths.get(javaHome)))
        } catch (e: Exception) {
            logger.warn("Failed to add JDK libraries from java.home: ${e.message}. Trying simple classpath...")
            // Fallback to simple method (unlikely to work for java 9+ if JdkClassFileProvider fails)
            getJdkLibraries().forEach { lib ->
                 commandBuilder.addLibraryFiles(lib)
            }
        }

        // Add R8 configuration
        commandBuilder.addProguardConfigurationFiles(configPath)

        // Disable DEX output (we want JVM bytecode)
        commandBuilder.disableDesugaring = true

        try {
            val command = commandBuilder.build()
            R8.run(command)
        } catch (e: Exception) {
            logger.error("R8 failed with error: ${e.message}")
            throw e
        }
    }

    private fun getJdkLibraries(): List<Path> {
        val javaHome = System.getProperty("java.home")
        val jreDir = File(javaHome)
        val rtJar = jreDir.resolve("lib/rt.jar")
        if (rtJar.exists()) {
             return listOf(rtJar.toPath())
        }
        return emptyList()
    }

    // Helper methods for UniMined integration
    private fun getMcJar(): File {
        val ext = project.extensions.getByType(UniminedExtension::class.java)
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mcc = ext.minecrafts[sourceSets.getByName("main")] ?: throw IllegalStateException("Minecraft configuration not found")
        return mcc.getMinecraft(mcc.mcPatcher.prodNamespace).toFile()
    }

    private fun isMcJar(file: File): Boolean {
        val ext = project.extensions.getByType(UniminedExtension::class.java)
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mcc = ext.minecrafts[sourceSets.getByName("main")] ?: return false
        return mcc.isMinecraftJar(file.toPath())
    }

    private fun acquireDependencies(): List<File> {
        return project.extensions
            .getByType(JavaPluginExtension::class.java)
            .sourceSets
            .getByName("main")
            .compileClasspath
            .files
            .filter { it.isFile }
            // Exclude nether-pathfinder since it's already included in the shadow JAR
            .filter { !it.name.contains("nether-pathfinder") }
    }

    // getTemporaryFile and getRootRelativeFile are inherited from BaritoneGradleTask
}