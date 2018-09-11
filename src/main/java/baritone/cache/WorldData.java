/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.cache;

import java.nio.file.Path;

/**
 * Data about a world, from baritone's point of view. Includes cached chunks, waypoints, and map data.
 *
 * @author leijurv
 */
public class WorldData {
    public final CachedWorld cache;
    public final Waypoints waypoints;
    //public final MapData map;
    public final Path directory;

    WorldData(Path directory) {
        this.directory = directory;
        this.cache = new CachedWorld(directory.resolve("cache"));
        this.waypoints = new Waypoints(directory.resolve("waypoints"));
    }

    void onClose() {
        new Thread() {
            public void run() {
                System.out.println("Started saving the world in a new thread");
                cache.save();
            }
        }.start();
    }
}
