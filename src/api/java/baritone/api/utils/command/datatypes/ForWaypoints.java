package baritone.api.utils.command.datatypes;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.IWaypointCollection;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class ForWaypoints implements IDatatypeFor<IWaypoint[]> {
    private final IWaypoint[] waypoints;

    public ForWaypoints() {
        waypoints = null;
    }

    public ForWaypoints(String arg) {
        IWaypoint.Tag tag = getTagByName(arg);
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
            .prepend(getTagNames())
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

    public static String[] getTagNames() {
        Set<String> names = new HashSet<>();

        for (IWaypoint.Tag tag : IWaypoint.Tag.values()) {
            names.addAll(asList(tag.names));
        }

        return names.toArray(new String[0]);
    }

    public static IWaypoint.Tag getTagByName(String name) {
        for (IWaypoint.Tag tag : IWaypoint.Tag.values()) {
            for (String alias : tag.names) {
                if (alias.equalsIgnoreCase(name)) {
                    return tag;
                }
            }
        }

        return null;
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
        Set<IWaypoint> found = new HashSet<>();

        for (IWaypoint waypoint : getWaypoints()) {
            if (waypoint.getName().equalsIgnoreCase(name)) {
                found.add(waypoint);
            }
        }

        return found.toArray(new IWaypoint[0]);
    }
}
