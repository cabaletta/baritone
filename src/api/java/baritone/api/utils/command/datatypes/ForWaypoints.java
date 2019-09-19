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

package baritone.api.utils.command.datatypes;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.IWaypointCollection;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public class ForWaypoints implements IDatatypeFor<IWaypoint[]> {

    private final IWaypoint[] waypoints;

    public ForWaypoints() {
        waypoints = null;
    }

    public ForWaypoints(String arg) {
        IWaypoint.Tag tag = IWaypoint.Tag.getByName(arg);
        waypoints = tag == null ? getWaypointsByName(arg) : getWaypointsByTag(tag);
    }

    public ForWaypoints(ArgConsumer consumer) {
        this(consumer.getString());
    }

    @Override
    public IWaypoint[] get() {
        return waypoints;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return new TabCompleteHelper()
                .append(getWaypointNames())
                .sortAlphabetically()
                .prepend(IWaypoint.Tag.getAllNames())
                .filterPrefix(consumer.getString())
                .stream();
    }

    public static IWaypointCollection waypoints() {
        return BaritoneAPI.getProvider()
                .getPrimaryBaritone()
                .getWorldProvider()
                .getCurrentWorld()
                .getWaypoints();
    }

    public static IWaypoint[] getWaypoints() {
        return waypoints().getAllWaypoints().stream()
                .sorted(Comparator.comparingLong(IWaypoint::getCreationTimestamp).reversed())
                .toArray(IWaypoint[]::new);
    }

    public static String[] getWaypointNames() {
        return Arrays.stream(getWaypoints())
                .map(IWaypoint::getName)
                .filter(name -> !name.equals(""))
                .toArray(String[]::new);
    }

    public static IWaypoint[] getWaypointsByTag(IWaypoint.Tag tag) {
        return waypoints().getByTag(tag).stream()
                .sorted(Comparator.comparingLong(IWaypoint::getCreationTimestamp).reversed())
                .toArray(IWaypoint[]::new);
    }

    public static IWaypoint[] getWaypointsByName(String name) {
        return Arrays.stream(getWaypoints())
                .filter(waypoint -> waypoint.getName().equalsIgnoreCase(name))
                .toArray(IWaypoint[]::new);
    }
}
