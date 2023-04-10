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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.cache.IWaypoint;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.ForWaypoints;
import baritone.api.command.datatypes.RelativeCoordinate;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalPatrol;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Stream;

public class PatrolCommand extends Command {

    private static List<Goal> goalList = new ArrayList<>();

    public PatrolCommand(IBaritone baritone) {
        super(baritone, "patrol");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        Action action = Action.LIST;
        if (args.hasAny()) {
            action = Action.getByNameOrDefault(args.getString());
        }

        switch (action) {
            case ADD:
                addWaypointFromArgs(args);
                break;
            case REMOVE:
                removeWaypoint(args);
                break;
            case CLEAR:
                goalList.clear();
                break;
            case LIST:
                listWaypoints();
                break;
            case EXECUTE:
                verifyGoalList();
                startPatrolling(getMode(args));
                break;
            case VERIFY:
                verifyGoalList();
                listWaypoints();
                break;
            case HELP:
                printHelp();
            default:
                logDirect("fallback to default case. no action taken.");
                logDirect(action.name());
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        TabCompleteHelper helper = new TabCompleteHelper();
        if (args.hasExactlyOne()) {
            helper.append(Action.getNames());
        } else {
            if (args.hasAtMost(4) && (args.peekString().equalsIgnoreCase("a") || args.peekString().equalsIgnoreCase("add"))) {
                while (args.has(2)) {
                    args.get();
                    if (args.peekDatatypeOrNull(RelativeCoordinate.INSTANCE) == null) {
                        IWaypoint[] waypoints = ForWaypoints.getWaypoints(this.baritone);
                        for (IWaypoint wp : waypoints) {
                            if (wp.getName().startsWith(args.peekString())) {
                                helper.append(wp.getName());
                            }
                        }
                        break;
                    }
                    if (!args.has(2)) {
                        helper.append("~");
                    }
                }
            } else if (args.hasAtMost(2) && isExecuteAction(args.peekString())) {
                while (args.has(2)) {
                    args.get();
                    for (GoalPatrol.Mode mode : GoalPatrol.Mode.values()) {
                        if (mode.getName().startsWith(args.peekString())) {
                            helper.append(mode.getName());
                        }
                    }
                }
            }
        }
        return helper.filterPrefix(args.getString()).stream();
    }

    @Override
    public String getShortDesc() {
        return "Path to multiple consecutive Goals";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The patrol command allows you to set multiple waypoints and path to them.",
                "",
                "Wherever a coordinate is expected, you can use ~ just like in regular Minecraft commands. Or, you can just use regular numbers.",
                "",
                "Usage:",
                "> patrol - lists your current waypoints",
                "> patrol <add/a> - add the player position as a waypoint.",
                "> patrol <add/a> <x> <y> <z> - add a single new waypoint.",
                "> patrol <add/a> <waypoint name> - add all waypoints with that name.",
                "> patrol <remove/rem/r> - removes the last added waypoint.",
                "> patrol <remove/rem/r> <#> - removes the # waypoint from the list.",
                "> patrol <clear/c> - clears all waypoints.",
                "> patrol <list/l> - lists all waypoints on the patrol route.",
                "> patrol <execute/exe/e/patrol> - starts patrolling along the waypoints.",
                "> patrol <execute> <mode> - starts patrolling in a specific mode.",
                "available modes are : oneway, circle, pendulum and random"
        );
    }

    private void addWaypointFromArgs(IArgConsumer args) throws CommandNotEnoughArgumentsException, CommandInvalidTypeException {
        if (args.has(3)) { //expect a <x y z> blockpos
            int x = args.peekString().equals("~") ? getPlayerPosAndUseArg(args, baritone.getPlayerContext().playerFeet().x) : args.getAs(Integer.class);
            int y = args.peekString().equals("~") ? getPlayerPosAndUseArg(args, baritone.getPlayerContext().playerFeet().y) : args.getAs(Integer.class);
            int z = args.peekString().equals("~") ? getPlayerPosAndUseArg(args, baritone.getPlayerContext().playerFeet().z) : args.getAs(Integer.class);
            GoalBlock goal = new GoalBlock(x, y, z);
            logDirect("Waypoint added: " + goal);
            goalList.add(goal);
        } else if (args.has(1)) { //expect a waypoint name
            String name = args.getString();
            IWaypoint[] waypoints = ForWaypoints.getWaypoints(this.baritone);
            for (IWaypoint wp : waypoints) {
                if (wp.getName().equals(name)) {
                    logDirect(String.format("Waypoint added: " + wp.getLocation().toString()));
                    goalList.add(new GoalBlock(wp.getLocation()));
                }
            }
        } else {
            GoalBlock goal = new GoalBlock(baritone.getPlayerContext().playerFeet());
            logDirect("Waypoint added: " + goal);
            goalList.add(goal);
        }
    }

    private int getPlayerPosAndUseArg(IArgConsumer args, int pos) throws CommandNotEnoughArgumentsException {
        args.get();
        return pos;
    }

    private void removeWaypoint(IArgConsumer args) throws CommandInvalidTypeException, CommandNotEnoughArgumentsException {
        if (args.hasAny()) {
            int removeMe = args.getAs(Integer.class);
            if (removeMe < 0 || removeMe >= goalList.size()) {
                throw new IllegalArgumentException("Index out of range.");
            } else {
                logDirect(String.format("Waypoint %s removed: " + goalList.get(removeMe).toString(), removeMe));
                goalList.remove(removeMe);
            }
        } else {
            if (goalList.isEmpty()) {
                logDirect(listEmptyMSG());
            } else {
                goalList.remove(goalList.size() - 1);
            }
        }
    }

    private void listWaypoints() {
        if (goalList.isEmpty()) {
            logDirect(listEmptyMSG());
        } else {
            for (int i = 0; i < goalList.size(); i++) {
                logDirect(String.format("%s: "+goalList.get(i).toString(), i));
            }
        }
    }

    private void verifyGoalList() {
        if (!goalList.isEmpty()) {
            List<Goal> helperList = new ArrayList<>();
            helperList.add(goalList.get(0));
            for (Goal goal : goalList) {
                BlockPos posA = ((GoalBlock) helperList.get(helperList.size() - 1)).getGoalPos();
                BlockPos posB = ((GoalBlock) goal).getGoalPos();
                if (posA.getX() != posB.getX() || posA.getY() != posB.getY() || posA.getZ() != posB.getZ()) {
                    helperList.add(goal);
                }
            }
            goalList = helperList;
        }
    }

    private void startPatrolling(GoalPatrol.Mode mode) {
        if (goalList.isEmpty()) {
            logDirect(listEmptyMSG());
        } else {
            logDirect("Now patrolling. Mode: " + mode.getName());
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalPatrol(new ArrayList<>(goalList), mode, 0));
        }
    }
    private GoalPatrol.Mode getMode(IArgConsumer args) throws CommandNotEnoughArgumentsException {
        GoalPatrol.Mode mode = GoalPatrol.Mode.ONEWAY;
        if (args.has(1)) {
            mode = GoalPatrol.Mode.getByNameOrDefault(args.getString());
        }
        return mode;
    }

    private void printHelp() {
        getLongDesc().forEach(this::logDirect);
    }

    private String listEmptyMSG() {
        return "No waypoints on patrol route.";
    }

    private boolean isExecuteAction(String s) {
        return (s.equalsIgnoreCase("execute") ||
                s.equalsIgnoreCase("exe") ||
                s.equalsIgnoreCase("e") ||
                s.equalsIgnoreCase("patrol"));
    }

    public static void addWaypoint(Goal goal) {
        goalList.add(goal);
    }

    enum Action {
        ADD("add", "a"),
        REMOVE("remove", "rem", "r"),
        CLEAR("clear", "c"),
        LIST("list","l"),
        EXECUTE("execute", "exe", "e", "start", "patrol"),
        HELP("help", "hlp", "?", "h"),
        VERIFY("verify","v");

        private final String[] names;

        Action(String... names) {
            this.names = names;
        }

        public static Action getByNameOrDefault(String name) {
            for (Action action : Action.values()) {
                for (String alias : action.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return action;
                    }
                }
            }
            return Action.HELP;
        }

        public static String[] getNames() {
            Set<String> names = new HashSet<>();
            for (PatrolCommand.Action action : PatrolCommand.Action.values()) {
                names.add(action.names[0]);
            }
            return names.toArray(new String[0]);
        }
    }
}
