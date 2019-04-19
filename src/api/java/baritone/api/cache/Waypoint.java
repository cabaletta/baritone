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

package baritone.api.cache;

import net.minecraft.util.math.BlockPos;

import java.util.Date;

/**
 * Basic implementation of {@link IWaypoint}
 *
 * @author leijurv
 */
public class Waypoint implements IWaypoint {

    private final String name;
    private final Tag tag;
    private final long creationTimestamp;
    private final BlockPos location;

    public Waypoint(String name, Tag tag, BlockPos location) {
        this(name, tag, location, System.currentTimeMillis());
    }

    /**
     * Constructor called when a Waypoint is read from disk, adds the creationTimestamp
     * as a parameter so that it is reserved after a waypoint is wrote to the disk.
     *
     * @param name              The waypoint name
     * @param tag               The waypoint tag
     * @param location          The waypoint location
     * @param creationTimestamp When the waypoint was created
     */
    public Waypoint(String name, Tag tag, BlockPos location, long creationTimestamp) {
        this.name = name;
        this.tag = tag;
        this.location = location;
        this.creationTimestamp = creationTimestamp;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + tag.hashCode() + location.hashCode(); //lol
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Tag getTag() {
        return this.tag;
    }

    @Override
    public long getCreationTimestamp() {
        return this.creationTimestamp;
    }

    @Override
    public BlockPos getLocation() {
        return this.location;
    }

    @Override
    public String toString() {
        return name + " " + location.toString() + " " + new Date(creationTimestamp).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof IWaypoint)) {
            return false;
        }
        IWaypoint w = (IWaypoint) o;
        return name.equals(w.getName()) && tag == w.getTag() && location.equals(w.getLocation());
    }
}
