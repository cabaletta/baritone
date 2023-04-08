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

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.ForBlockOptionalMeta;
import baritone.api.command.datatypes.ForWaypoints;
import baritone.api.command.datatypes.RelativeCoordinate;
import baritone.api.command.datatypes.RelativeGoal;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalPatrol;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.process.CustomGoalProcess;
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
        Action action;
        if (args.hasAny()) {
            action = Action.getByName(args.getString());
        } else { //todo should argumetnt less case be list or execute?
            action = Action.LIST;
        }
        if(action == null) {
            getLongDesc().forEach(this::logDirect);
        }

        switch (action) {
            case ADD:
                //logDirect("case add");
                if (args.has(3)) { //expect a <x y z> blockpos
                    int x = args.peekString().equals("~") ? getPlayerPosAndUseArg(args, baritone.getPlayerContext().playerFeet().x) : args.getAs(Integer.class);
                    int y = args.peekString().equals("~") ? getPlayerPosAndUseArg(args, baritone.getPlayerContext().playerFeet().y) : args.getAs(Integer.class);
                    int z = args.peekString().equals("~") ? getPlayerPosAndUseArg(args, baritone.getPlayerContext().playerFeet().z) : args.getAs(Integer.class);
                    GoalBlock goal = new GoalBlock(x, y, z);
                    logDirect("Waypoint added: " + goal.toString());
                    goalList.add(goal);
                } else if (args.has(1)) { //expect a waypoint name
                    //IWaypoint.Tag tag = IWaypoint.Tag.getByName(args.getString());
                    String name = args.getString();
                    IWaypoint[] waypoints = ForWaypoints.getWaypoints(this.baritone);
                    for (IWaypoint wp : waypoints) {
                        if (wp.getName().equals(name)) {
                            logDirect(String.format("Waypoint added: " + wp.getLocation().toString()));
                            goalList.add(new GoalBlock(wp.getLocation()));
                        }
                    }
                } else {
                    logDirect("Expecting a block-pos or a waypoint name.");
                }
                break;
            case REMOVE:
                //logDirect("case remove");
                if (args.hasAny()) {
                    int removeMe = args.getAs(Integer.class);
                    if (removeMe < 0 || removeMe >= goalList.size()) {
                        throw new IllegalArgumentException("Index out of range.");
                    } else{
                        logDirect(String.format("Waypoint %s removed: " + goalList.get(removeMe).toString(), removeMe));
                        goalList.remove(removeMe);
                    }
                } else {
                    logDirect("Expecting a index to remove a waypoint.");
                }
                break;
            case CLEAR:
                //logDirect("case clear");
                goalList.clear();
                break;
            case LIST:
                //logDirect("case list");
                if (goalList.isEmpty()) {
                    logDirect("No waypoints on patrol route");
                } else {
                    for (int i = 0; i < goalList.size(); i++) {
                        logDirect(String.format("%s: "+goalList.get(i).toString(), i));
                    }
                }
                break;
            case EXECUTE:
                //logDirect("case execute");
                verifyGoalList();
                if (goalList.isEmpty()) {
                    logDirect("No waypoints on patrol route");
                } else {
                    GoalPatrol.Mode mode = GoalPatrol.Mode.ONEWAY;
                    if (args.has(1)) {
                        mode = GoalPatrol.Mode.getByNameOrDefault(args.getString());
                    }
                    logDirect("Now patrolling. Mode: " + mode.name());
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalPatrol(new ArrayList<>(goalList), mode, 0));
                }
                break;
            /*case MODE:
                if (args.hasAny()) {
                    try {
                        GoalPatrol goal = (GoalPatrol) baritone.getCustomGoalProcess().getGoal();
                        logDirect("Previus Mode: " + goal.getMode().name());
                        goal.setMode(args.getString());
                        logDirect("New Mode: " + goal.getMode().name());
                    } catch (Exception e) {
                        logDirect("something went wrong");
                        logDirect(e.toString());
                    }
                }
                break;/**/
            default:
                logDirect("fallback to default case. no action taken.");
                logDirect(action.name());
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        TabCompleteHelper helper = new TabCompleteHelper();
        if (args.hasExactlyOne()) {
            helper.append("add", "remove", "clear", "list", "execute", "patrol");
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
                "> patrol <add/a> <x> <y> <z> - add a single new waypoint.",
                "> patrol <add/a> <waypoint name> - add all waypoints with that name.",
                "> patrol <remove/rem/r> <#> - removes the # waypoint from the list.",
                "> patrol <clear/c> - clears all waypoints.",
                "> patrol <list/l> - lists all waypoints on the patrol route.",
                "> patrol <execute/exe/e/patrol> - starts patrolling along the waypoints.",
                "> patrol <execute> <mode> - starts patrolling in a specific mode.",
                "available modes are : oneway, circle, pendulum and random"
        );
    }

    private int getPlayerPosAndUseArg(IArgConsumer args, int pos) throws CommandNotEnoughArgumentsException {
        args.get();
        return pos;
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

    //todo this does not work correctly
    //the same block pos can be present multiple times but not in succession or at the start and the end
    private void verifyGoalList() {
        Iterator<Goal> iterator = goalList.listIterator();
        GoalBlock first = (GoalBlock) iterator.next();
        GoalBlock previus = first;
        GoalBlock next;
        while (iterator.hasNext()) {
            next = (GoalBlock)iterator.next();
            if (first.getGoalPos().equals(next.getGoalPos())) {
                iterator.remove();
            } else {
                previus = next;
            }
        }
        if (previus.getGoalPos().equals(first.getGoalPos()) && previus != first) {
            goalList.remove(previus);
        }
    }

    enum Action {
        ADD("add", "a"),
        REMOVE("remove", "rem", "r"),
        CLEAR("clear", "c"),
        LIST("list","l"),
        EXECUTE("execute", "exe", "e", "start", "patrol"),
        MODE("mode","m"),
        HELP("help", "hlp", "?", "h"),
        MOVE("move", "mv", "m"),
        EDIT("edit");

        private final String[] names;

        Action(String... names) {
            this.names = names;
        }

        public static Action getByName(String name) {
            for (Action action : Action.values()) {
                for (String alias : action.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return action;
                    }
                }
            }
            return null;
        }

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();
            for (PatrolCommand.Action action : PatrolCommand.Action.values()) {
                names.addAll(Arrays.asList(action.names));
            }
            return names.toArray(new String[0]);
        }
    }
}
