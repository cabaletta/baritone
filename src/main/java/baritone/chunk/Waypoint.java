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

package baritone.chunk;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A single waypoint
 *
 * @author leijurv
 */
public class Waypoint {
    public final String name;
    public final Tag tag;
    public final long creationTimestamp;
    public final BlockPos location;

    public Waypoint(String name, Tag tag, BlockPos location) {
        this(name, tag, location, System.currentTimeMillis());
    }

    Waypoint(String name, Tag tag, BlockPos location, long creationTimestamp) { // read from disk
        this.name = name;
        this.tag = tag;
        this.location = location;
        this.creationTimestamp = creationTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Waypoint)) {
            return false;
        }
        Waypoint w = (Waypoint) o;
        return name.equals(w.name) && tag == w.tag && creationTimestamp == w.creationTimestamp && location.equals(w.location);
    }

    @Override
    public int hashCode() {
        return name.hashCode() + tag.hashCode() + (int) creationTimestamp + location.hashCode(); //lol
    }

    public long creationTimestamp() {
        return creationTimestamp;
    }

    public String toString() {
        return name + " " + location.toString() + " " + new Date(creationTimestamp).toString();
    }

    public enum Tag {
        HOME, DEATH, BED, USER;

    }

    public static final Map<String, Tag> TAG_MAP;

    static {
        HashMap<String, Tag> map = new HashMap<>();
        map.put("home", Tag.HOME);
        map.put("base", Tag.HOME);
        map.put("bed", Tag.BED);
        map.put("spawn", Tag.BED);
        map.put("death", Tag.DEATH);
        TAG_MAP = Collections.unmodifiableMap(map);
    }
}
