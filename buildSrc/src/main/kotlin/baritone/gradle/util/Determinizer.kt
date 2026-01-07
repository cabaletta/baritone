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

package baritone.gradle.util

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Make a .jar file deterministic by sorting all entries by name, and setting all the last modified times to a fixed value.
 * This makes the build 100% reproducible since the timestamp when you built it no longer affects the final file.
 *
 * @author leijurv
 * Ported to Kotlin for the new build system
 */
object Determinizer {

    /**
     * Fixed timestamp used for all JAR entries to ensure deterministic builds.
     * The value 42069 is arbitrary but consistent across all builds.
     */
    private const val DETERMINISTIC_TIMESTAMP = 42069L

    @JvmStatic
    @JvmOverloads
    fun determinize(
        inputPath: String,
        outputPath: String,
        toInclude: List<File> = emptyList(),
        doForgeReplacementOfMetaInf: Boolean = false
    ) {
        println("Running Determinizer")
        println(" Input path: $inputPath")
        println(" Output path: $outputPath")
        println(" Shade: $toInclude")

        JarFile(File(inputPath)).use { jarFile ->
            JarOutputStream(FileOutputStream(outputPath)).use { jos ->
                val entries = jarFile.entries().asSequence()
                    .sortedBy { it.name }
                    .toList()

                for (entry in entries) {
                    // Skip forge cache files
                    if (entry.name == "META-INF/fml_cache_annotation.json" ||
                        entry.name == "META-INF/fml_cache_class_versions.json") {
                        continue
                    }

                    val clone = JarEntry(entry.name)
                    clone.time = DETERMINISTIC_TIMESTAMP
                    jos.putNextEntry(clone)

                    when {
                        entry.name.endsWith(".refmap.json") -> {
                            val json = JsonParser.parseReader(InputStreamReader(jarFile.getInputStream(entry)))
                            jos.write(writeSorted(json).toByteArray())
                        }
                        else -> {
                            jarFile.getInputStream(entry).copyTo(jos)
                        }
                    }
                }

                // Include additional files
                for (file in toInclude) {
                    JarFile(file).use { mixin ->
                        val mixinEntries = mixin.entries().asSequence()
                            .sortedBy { it.name }
                            .toList()

                        for (entry in mixinEntries) {
                            if (entry.name.startsWith("META-INF") && !entry.name.startsWith("META-INF/services")) {
                                continue
                            }
                            jos.putNextEntry(entry)
                            mixin.getInputStream(entry).copyTo(jos)
                        }
                    }
                }

                jos.finish()
            }
        }

        println("Done with determinizer")
    }

    private fun writeSorted(element: JsonElement): String {
        val writer = StringWriter()
        val jsonWriter = JsonWriter(writer)
        ORDERED_JSON_WRITER.write(jsonWriter, element)
        return writer.toString() + "\n"
    }

    /**
     * All credits go to GSON and its contributors. GSON is licensed under the Apache 2.0 License.
     * This implementation has been modified to write JsonObject keys in order.
     */
    private val ORDERED_JSON_WRITER = object : TypeAdapter<JsonElement>() {

        override fun read(reader: JsonReader): JsonElement? = null

        override fun write(out: JsonWriter, value: JsonElement?) {
            when {
                value == null || value.isJsonNull -> out.nullValue()
                value.isJsonPrimitive -> {
                    val primitive = value.asJsonPrimitive
                    when {
                        primitive.isNumber -> out.value(primitive.asNumber)
                        primitive.isBoolean -> out.value(primitive.asBoolean)
                        else -> out.value(primitive.asString)
                    }
                }
                value.isJsonArray -> {
                    out.beginArray()
                    for (element in value.asJsonArray) {
                        write(out, element)
                    }
                    out.endArray()
                }
                value.isJsonObject -> {
                    out.beginObject()
                    val entries = value.asJsonObject.entrySet()
                        .sortedBy { it.key }

                    for ((key, element) in entries) {
                        out.name(key)
                        write(out, element)
                    }
                    out.endObject()
                }
                else -> throw IllegalArgumentException("Couldn't write ${value::class.java}")
            }
        }
    }
}