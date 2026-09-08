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

package baritone.utils.locate;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Stored alongside the current world's dimension cache, never in global settings. */
public record LocateSettings(Long seed, String preset) {
    public LocateSettings {
        if (!validPreset(preset)) {
            throw new IllegalArgumentException("Unknown preset: " + preset);
        }
    }

    public static boolean validPreset(String preset) {
        return "default".equals(preset) || "large_biomes".equals(preset) || "amplified".equals(preset);
    }

    public static LocateSettings read(Path directory) throws IOException {
        Path file = directory.resolve("locate.properties");
        if (!Files.exists(file)) {
            return new LocateSettings(null, "default");
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid locate.properties: " + e.getMessage(), e);
        }
        try {
            String preset = properties.getProperty("preset", "default");
            if (!validPreset(preset)) {
                throw new IllegalArgumentException("Unknown preset");
            }
            String seed = properties.getProperty("seed");
            return new LocateSettings(seed == null ? null : Long.valueOf(seed), preset);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid locate.properties: " + e.getMessage(), e);
        }
    }

    public void save(Path directory) throws IOException {
        Files.createDirectories(directory);
        Properties properties = new Properties();
        if (seed != null) {
            properties.setProperty("seed", seed.toString());
        }
        properties.setProperty("preset", preset);
        Path temporary = Files.createTempFile(directory, "locate-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Baritone biome locator: this world and dimension only");
            }
            try {
                Files.move(temporary, directory.resolve("locate.properties"), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, directory.resolve("locate.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
