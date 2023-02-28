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

package baritone.gradle.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

/**
 * Make a .jar file deterministic by sorting all entries by name, and setting all the last modified times to 42069.
 * This makes the build 100% reproducible since the timestamp when you built it no longer affects the final file.
 *
 * @author leijurv
 */
public class Determinizer {

    public static void determinize(String inputPath, String outputPath) throws IOException {
        System.out.println("Running Determinizer");
        System.out.println(" Input path: " + inputPath);
        System.out.println(" Output path: " + outputPath);

        try (
                JarFile jarFile = new JarFile(new File(inputPath));
                JarOutputStream jos = new JarOutputStream(new FileOutputStream(new File(outputPath)))
        ) {

            List<JarEntry> entries = jarFile.stream()
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .collect(Collectors.toList());

            for (JarEntry entry : entries) {
                if (entry.getName().equals("META-INF/fml_cache_annotation.json")) {
                    continue;
                }
                if (entry.getName().equals("META-INF/fml_cache_class_versions.json")) {
                    continue;
                }
                JarEntry clone = new JarEntry(entry.getName());
                clone.setTime(42069);
                jos.putNextEntry(clone);
                if (entry.getName().endsWith(".refmap.json")) {
                    JsonObject object = new JsonParser().parse(new InputStreamReader(jarFile.getInputStream(entry))).getAsJsonObject();
                    jos.write(writeSorted(object).getBytes());
                } else {
                    copy(jarFile.getInputStream(entry), jos);
                }
            }
            jos.finish();
        }
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    private static String writeSorted(JsonObject in) throws IOException {
        StringWriter writer = new StringWriter();
        JsonWriter jw = new JsonWriter(writer);
        ORDERED_JSON_WRITER.write(jw, in);
        return writer.toString() + "\n";
    }

    /**
     * All credits go to GSON and its contributors. GSON is licensed under the Apache 2.0 License.
     * This implementation has been modified to write {@link JsonObject} keys in order.
     *
     * @see <a href="https://github.com/google/gson/blob/master/LICENSE">GSON License</a>
     * @see <a href="https://github.com/google/gson/blob/master/gson/src/main/java/com/google/gson/internal/bind/TypeAdapters.java#L698">Original Source</a>
     */
    private static final TypeAdapter<JsonElement> ORDERED_JSON_WRITER = new TypeAdapter<JsonElement>() {

        @Override
        public JsonElement read(JsonReader in) {
            return null;
        }

        @Override
        public void write(JsonWriter out, JsonElement value) throws IOException {
            if (value == null || value.isJsonNull()) {
                out.nullValue();
            } else if (value.isJsonPrimitive()) {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    out.value(primitive.getAsNumber());
                } else if (primitive.isBoolean()) {
                    out.value(primitive.getAsBoolean());
                } else {
                    out.value(primitive.getAsString());
                }

            } else if (value.isJsonArray()) {
                out.beginArray();
                for (JsonElement e : value.getAsJsonArray()) {
                    write(out, e);
                }
                out.endArray();

            } else if (value.isJsonObject()) {
                out.beginObject();

                List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(value.getAsJsonObject().entrySet());
                entries.sort(Comparator.comparing(Map.Entry::getKey));
                for (Map.Entry<String, JsonElement> e : entries) {
                    out.name(e.getKey());
                    write(out, e.getValue());
                }
                out.endObject();

            } else {
                throw new IllegalArgumentException("Couldn't write " + value.getClass());
            }
        }
    };
}
