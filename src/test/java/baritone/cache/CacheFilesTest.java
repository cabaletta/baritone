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

package baritone.cache;

import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.utils.BetterBlockPos;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class CacheFilesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void moveIntoPlaceReplacesExistingFile() throws Exception {
        Path target = folder.getRoot().toPath().resolve("r.0.0.bcr");
        Path temp = folder.getRoot().toPath().resolve("r.0.0.bcr.tmp");
        Files.write(target, "old".getBytes(StandardCharsets.UTF_8));
        Files.write(temp, "new".getBytes(StandardCharsets.UTF_8));

        CacheFiles.moveIntoPlace(temp, target);

        assertEquals("new", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        assertFalse(Files.exists(temp));
    }

    @Test
    public void moveIntoPlaceCreatesMissingFile() throws Exception {
        Path target = folder.getRoot().toPath().resolve("home.mp4");
        Path temp = folder.getRoot().toPath().resolve("home.mp4.tmp");
        Files.write(temp, "new".getBytes(StandardCharsets.UTF_8));

        CacheFiles.moveIntoPlace(temp, target);

        assertEquals("new", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        assertFalse(Files.exists(temp));
    }

    @Test
    public void waypointsSurviveSaveAndLoad() throws Exception {
        Path dir = folder.getRoot().toPath().resolve("waypoints");
        Waypoint home = new Waypoint("base", IWaypoint.Tag.HOME, new BetterBlockPos(1, 64, -2), 1234L);

        new WaypointCollection(dir).addWaypoint(home);
        Set<IWaypoint> loaded = new WaypointCollection(dir).getByTag(IWaypoint.Tag.HOME);

        assertEquals(1, loaded.size());
        IWaypoint waypoint = loaded.iterator().next();
        assertEquals("base", waypoint.getName());
        assertEquals(new BetterBlockPos(1, 64, -2), waypoint.getLocation());
        assertEquals(1234L, waypoint.getCreationTimestamp());
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue("temporary file left behind", files.noneMatch(p -> p.toString().endsWith(".tmp")));
        }
    }
}
