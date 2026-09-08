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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class LocateSettingsTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void zeroSeedIsKnownAndWorldsStayIsolated() throws IOException {
        Path first = temporary.newFolder("server-one").toPath();
        Path second = temporary.newFolder("server-two").toPath();
        new LocateSettings(0L, "large_biomes").save(first);
        assertEquals(new LocateSettings(0L, "large_biomes"), LocateSettings.read(first));
        assertNull(LocateSettings.read(second).seed());
        new LocateSettings(null, "large_biomes").save(first);
        assertEquals(new LocateSettings(null, "large_biomes"), LocateSettings.read(first));
    }

    @Test
    public void signedSeedsRoundTripWithoutPrecisionLoss() throws IOException {
        Path directory = temporary.newFolder().toPath();
        for (long seed : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1234567890123456789L}) {
            new LocateSettings(seed, "default").save(directory);
            assertEquals(Long.valueOf(seed), LocateSettings.read(directory).seed());
        }
    }

    @Test
    public void corruptSettingsFailInsteadOfPredictingWithWrongSeed() throws IOException {
        Path directory = temporary.newFolder().toPath();
        Files.writeString(directory.resolve("locate.properties"), "seed=invalid\n");
        assertThrows(IOException.class, () -> LocateSettings.read(directory));
        Files.writeString(directory.resolve("locate.properties"), "seed=\\uZZZZ\n");
        assertThrows(IOException.class, () -> LocateSettings.read(directory));
    }

    @Test
    public void invalidPresetCannotReplaceSavedSettings() throws IOException {
        Path directory = temporary.newFolder().toPath();
        LocateSettings previous = new LocateSettings(42L, "default");
        previous.save(directory);
        assertThrows(IllegalArgumentException.class, () -> new LocateSettings(999L, "typo").save(directory));
        assertEquals(previous, LocateSettings.read(directory));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
}
