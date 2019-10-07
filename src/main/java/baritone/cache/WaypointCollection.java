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
import baritone.api.cache.IWaypointCollection;
import baritone.api.cache.Waypoint;
import baritone.api.utils.BetterBlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Waypoints for a world
 *
 * @author leijurv
 */
public class WaypointCollection implements IWaypointCollection {

    /**
     * Magic value to detect invalid waypoint files
     */
    private static final long WAYPOINT_MAGIC_VALUE = 121977993584L; // good value

    private final Path directory;
    private final Map<IWaypoint.Tag, Set<IWaypoint>> waypoints;

    WaypointCollection(Path directory) {
        this.directory = directory;
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ignored) {}
        }
        System.out.println("Would save waypoints to " + directory);
        this.waypoints = new HashMap<>();
        load();
    }

    private void load() {
        for (Waypoint.Tag tag : Waypoint.Tag.values()) {
            load(tag);
        }
    }

    private synchronized void load(Waypoint.Tag tag) {
        this.waypoints.put(tag, new HashSet<>());

        Path fileName = this.directory.resolve(tag.name().toLowerCase() + ".mp4");
        if (!Files.exists(fileName)) {
            return;
        }

        try (
                FileInputStream fileIn = new FileInputStream(fileName.toFile());
                BufferedInputStream bufIn = new BufferedInputStream(fileIn);
                DataInputStream in = new DataInputStream(bufIn)
        ) {
            long magic = in.readLong();
            if (magic != WAYPOINT_MAGIC_VALUE) {
                throw new IOException("Bad magic value " + magic);
            }

            long length = in.readLong(); // Yes I want 9,223,372,036,854,775,807 waypoints, do you not?
            while (length-- > 0) {
                String name = in.readUTF();
                long creationTimestamp = in.readLong();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                this.waypoints.get(tag).add(new Waypoint(name, tag, new BetterBlockPos(x, y, z), creationTimestamp));
            }
        } catch (IOException ignored) {}
    }

    private synchronized void save(Waypoint.Tag tag) {
        Path fileName = this.directory.resolve(tag.name().toLowerCase() + ".mp4");
        try (
                FileOutputStream fileOut = new FileOutputStream(fileName.toFile());
                BufferedOutputStream bufOut = new BufferedOutputStream(fileOut);
                DataOutputStream out = new DataOutputStream(bufOut)
        ) {
            out.writeLong(WAYPOINT_MAGIC_VALUE);
            out.writeLong(this.waypoints.get(tag).size());
            for (IWaypoint waypoint : this.waypoints.get(tag)) {
                out.writeUTF(waypoint.getName());
                out.writeLong(waypoint.getCreationTimestamp());
                out.writeInt(waypoint.getLocation().getX());
                out.writeInt(waypoint.getLocation().getY());
                out.writeInt(waypoint.getLocation().getZ());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addWaypoint(IWaypoint waypoint) {
        // no need to check for duplicate, because it's a Set not a List
        if (waypoints.get(waypoint.getTag()).add(waypoint)) {
            save(waypoint.getTag());
        }
    }

    @Override
    public void removeWaypoint(IWaypoint waypoint) {
        if (waypoints.get(waypoint.getTag()).remove(waypoint)) {
            save(waypoint.getTag());
        }
    }

    @Override
    public IWaypoint getMostRecentByTag(IWaypoint.Tag tag) {
        // Find a waypoint of the given tag which has the greatest timestamp value, indicating the most recent
        return this.waypoints.get(tag).stream().min(Comparator.comparingLong(w -> -w.getCreationTimestamp())).orElse(null);
    }

    @Override
    public Set<IWaypoint> getByTag(IWaypoint.Tag tag) {
        return Collections.unmodifiableSet(this.waypoints.get(tag));
    }

    @Override
    public Set<IWaypoint> getAllWaypoints() {
        return this.waypoints.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
