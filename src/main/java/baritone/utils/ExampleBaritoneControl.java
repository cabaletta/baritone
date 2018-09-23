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

package baritone.utils;

import baritone.Baritone;
import baritone.Settings;
import baritone.api.behavior.Behavior;
import baritone.api.event.events.ChatEvent;
import baritone.behavior.FollowBehavior;
import baritone.behavior.MineBehavior;
import baritone.behavior.PathingBehavior;
import baritone.cache.ChunkPacker;
import baritone.cache.Waypoint;
import baritone.cache.WorldProvider;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.goals.*;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class ExampleBaritoneControl extends Behavior implements Helper {

    public static ExampleBaritoneControl INSTANCE = new ExampleBaritoneControl();

    private ExampleBaritoneControl() {

    }

    public void initAndRegister() {
        Baritone.INSTANCE.registerBehavior(this);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        if (!Baritone.settings().chatControl.get()) {
            if (!Baritone.settings().removePrefix.get()) {
                return;
            }
        }
        String msg = event.getMessage().toLowerCase(Locale.US);
        if (Baritone.settings().prefix.get()) {
            if (!msg.startsWith("#")) {
                return;
            }
            msg = msg.substring(1);
        }

        List<Settings.Setting<Boolean>> toggleable = Baritone.settings().getAllValuesByType(Boolean.class);
        for (Settings.Setting<Boolean> setting : toggleable) {
            if (msg.equalsIgnoreCase(setting.getName())) {
                setting.value ^= true;
                event.cancel();
                logDirect("Toggled " + setting.getName() + " to " + setting.value);
                return;
            }
        }
        if (msg.equals("baritone") || msg.equals("settings")) {
            for (Settings.Setting<?> setting : Baritone.settings().allSettings) {
                logDirect(setting.toString());
            }
            event.cancel();
            return;
        }
        if (msg.contains(" ")) {
            String[] data = msg.split(" ");
            if (data.length == 2) {
                Settings.Setting setting = Baritone.settings().byLowerName.get(data[0]);
                if (setting != null) {
                    try {
                        if (setting.value.getClass() == Long.class) {
                            setting.value = Long.parseLong(data[1]);
                        }
                        if (setting.value.getClass() == Integer.class) {
                            setting.value = Integer.parseInt(data[1]);
                        }
                        if (setting.value.getClass() == Double.class) {
                            setting.value = Double.parseDouble(data[1]);
                        }
                        if (setting.value.getClass() == Float.class) {
                            setting.value = Float.parseFloat(data[1]);
                        }
                    } catch (NumberFormatException e) {
                        logDirect("Unable to parse " + data[1]);
                        event.cancel();
                        return;
                    }
                    logDirect(setting.toString());
                    event.cancel();
                    return;
                }
            }
        }
        if (Baritone.settings().byLowerName.containsKey(msg)) {
            Settings.Setting<?> setting = Baritone.settings().byLowerName.get(msg);
            logDirect(setting.toString());
            event.cancel();
            return;
        }

        if (msg.startsWith("goal")) {
            event.cancel();
            String[] params = msg.substring(4).trim().split(" ");
            if (params[0].equals("")) {
                params = new String[]{};
            }
            Goal goal;
            try {
                switch (params.length) {
                    case 0:
                        goal = new GoalBlock(playerFeet());
                        break;
                    case 1:
                        if (params[0].equals("clear") || params[0].equals("none")) {
                            goal = null;
                        } else {
                            goal = new GoalYLevel(Integer.parseInt(params[0]));
                        }
                        break;
                    case 2:
                        goal = new GoalXZ(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
                        break;
                    case 3:
                        goal = new GoalBlock(new BlockPos(Integer.parseInt(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2])));
                        break;
                    default:
                        logDirect("unable to understand lol");
                        return;
                }
            } catch (NumberFormatException ex) {
                logDirect("unable to parse integer " + ex);
                return;
            }
            PathingBehavior.INSTANCE.setGoal(goal);
            logDirect("Goal: " + goal);
            return;
        }
        if (msg.equals("path")) {
            if (!PathingBehavior.INSTANCE.path()) {
                if (PathingBehavior.INSTANCE.getGoal() == null) {
                    logDirect("No goal.");
                } else {
                    if (PathingBehavior.INSTANCE.getGoal().isInGoal(playerFeet())) {
                        logDirect("Already in goal");
                    } else {
                        logDirect("Currently executing a path. Please cancel it first.");
                    }
                }
            }
            event.cancel();
            return;
        }
        if (msg.equals("repack") || msg.equals("rescan")) {
            ChunkProviderClient cli = world().getChunkProvider();
            int playerChunkX = playerFeet().getX() >> 4;
            int playerChunkZ = playerFeet().getZ() >> 4;
            int count = 0;
            for (int x = playerChunkX - 40; x <= playerChunkX + 40; x++) {
                for (int z = playerChunkZ - 40; z <= playerChunkZ + 40; z++) {
                    Chunk chunk = cli.getLoadedChunk(x, z);
                    if (chunk != null) {
                        count++;
                        WorldProvider.INSTANCE.getCurrentWorld().cache.queueForPacking(chunk);
                    }
                }
            }
            logDirect("Queued " + count + " chunks for repacking");
            event.cancel();
            return;
        }
        if (msg.equals("axis")) {
            PathingBehavior.INSTANCE.setGoal(new GoalAxis());
            PathingBehavior.INSTANCE.path();
            event.cancel();
            return;
        }
        if (msg.equals("cancel")) {
            MineBehavior.INSTANCE.cancel();
            FollowBehavior.INSTANCE.cancel();
            PathingBehavior.INSTANCE.cancel();
            event.cancel();
            logDirect("ok canceled");
            return;
        }
        if (msg.equals("forcecancel")) {
            AbstractNodeCostSearch.forceCancel();
            event.cancel();
            logDirect("ok force canceled");
            return;
        }
        if (msg.equals("gc")) {
            System.gc();
            event.cancel();
            logDirect("Called System.gc();");
            return;
        }
        if (msg.equals("invert")) {
            Goal goal = PathingBehavior.INSTANCE.getGoal();
            BlockPos runAwayFrom;
            if (goal instanceof GoalXZ) {
                runAwayFrom = new BlockPos(((GoalXZ) goal).getX(), 0, ((GoalXZ) goal).getZ());
            } else if (goal instanceof GoalBlock) {
                runAwayFrom = ((GoalBlock) goal).getGoalPos();
            } else {
                logDirect("Goal must be GoalXZ or GoalBlock to invert");
                logDirect("Inverting goal of player feet");
                runAwayFrom = playerFeet();
            }
            PathingBehavior.INSTANCE.setGoal(new GoalRunAway(1, runAwayFrom) {
                @Override
                public boolean isInGoal(BlockPos pos) {
                    return false;
                }
            });
            if (!PathingBehavior.INSTANCE.path()) {
                logDirect("Currently executing a path. Please cancel it first.");
            }
            event.cancel();
            return;
        }
        if (msg.startsWith("follow")) {
            String name = msg.substring(6).trim();
            Optional<Entity> toFollow = Optional.empty();
            if (name.length() == 0) {
                toFollow = MovementHelper.whatEntityAmILookingAt();
            } else {
                for (EntityPlayer pl : world().playerEntities) {
                    String theirName = pl.getName().trim().toLowerCase();
                    if (!theirName.equals(player().getName().trim().toLowerCase())) { // don't follow ourselves lol
                        if (theirName.contains(name) || name.contains(theirName)) {
                            toFollow = Optional.of(pl);
                        }
                    }
                }
            }
            if (!toFollow.isPresent()) {
                logDirect("Not found");
                event.cancel();
                return;
            }
            FollowBehavior.INSTANCE.follow(toFollow.get());
            logDirect("Following " + toFollow.get());
            event.cancel();
            return;
        }
        if (msg.equals("reloadall")) {
            WorldProvider.INSTANCE.getCurrentWorld().cache.reloadAllFromDisk();
            logDirect("ok");
            event.cancel();
            return;
        }
        if (msg.equals("saveall")) {
            WorldProvider.INSTANCE.getCurrentWorld().cache.save();
            logDirect("ok");
            event.cancel();
            return;
        }
        if (msg.startsWith("find")) {
            String blockType = msg.substring(4).trim();
            LinkedList<BlockPos> locs = WorldProvider.INSTANCE.getCurrentWorld().cache.getLocationsOf(blockType, 1, 4);
            logDirect("Have " + locs.size() + " locations");
            for (BlockPos pos : locs) {
                Block actually = BlockStateInterface.get(pos).getBlock();
                if (!ChunkPacker.blockToString(actually).equalsIgnoreCase(blockType)) {
                    System.out.println("Was looking for " + blockType + " but actually found " + actually + " " + ChunkPacker.blockToString(actually));
                }
            }
            event.cancel();
            return;
        }
        if (msg.startsWith("mine")) {
            String[] blockTypes = msg.substring(4).trim().split(" ");
            try {
                int quantity = Integer.parseInt(blockTypes[1]);
                ChunkPacker.stringToBlock(blockTypes[0]).hashCode();
                MineBehavior.INSTANCE.mine(quantity, blockTypes[0]);
                logDirect("Will mine " + quantity + " " + blockTypes[0]);
                event.cancel();
                return;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException ex) {}
            for (String s : blockTypes) {
                if (ChunkPacker.stringToBlock(s) == null) {
                    logDirect(s + " isn't a valid block name");
                    event.cancel();
                    return;
                }

            }
            MineBehavior.INSTANCE.mine(0, blockTypes);
            logDirect("Started mining blocks of type " + Arrays.toString(blockTypes));
            event.cancel();
            return;
        }
        if (msg.startsWith("thisway")) {
            try {
                Goal goal = GoalXZ.fromDirection(playerFeetAsVec(), player().rotationYaw, Double.parseDouble(msg.substring(7).trim()));
                PathingBehavior.INSTANCE.setGoal(goal);
                logDirect("Goal: " + goal);
            } catch (NumberFormatException ex) {
                logDirect("Error unable to parse '" + msg.substring(7).trim() + "' to a double.");
            }
            event.cancel();
            return;
        }
        if (msg.startsWith("list") || msg.startsWith("get ") || msg.startsWith("show")) {
            String waypointType = msg.substring(4).trim();
            if (waypointType.endsWith("s")) {
                // for example, "show deaths"
                waypointType = waypointType.substring(0, waypointType.length() - 1);
            }
            Waypoint.Tag tag = Waypoint.Tag.fromString(waypointType);
            if (tag == null) {
                logDirect("Not a valid tag. Tags are: " + Arrays.asList(Waypoint.Tag.values()).toString().toLowerCase());
                event.cancel();
                return;
            }
            Set<Waypoint> waypoints = WorldProvider.INSTANCE.getCurrentWorld().waypoints.getByTag(tag);
            // might as well show them from oldest to newest
            List<Waypoint> sorted = new ArrayList<>(waypoints);
            sorted.sort(Comparator.comparingLong(Waypoint::creationTimestamp));
            logDirect("Waypoints under tag " + tag + ":");
            for (Waypoint waypoint : sorted) {
                logDirect(waypoint.toString());
            }
            event.cancel();
            return;
        }
        if (msg.startsWith("save")) {
            String name = msg.substring(4).trim();
            WorldProvider.INSTANCE.getCurrentWorld().waypoints.addWaypoint(new Waypoint(name, Waypoint.Tag.USER, playerFeet()));
            logDirect("Saved user defined tag under name '" + name + "'. Say 'goto user' to set goal, say 'list user' to list.");
            event.cancel();
            return;
        }
        if (msg.startsWith("goto")) {
            String waypointType = msg.substring(4).trim();
            if (waypointType.endsWith("s") && Waypoint.Tag.fromString(waypointType.substring(0, waypointType.length() - 1)) != null) {
                // for example, "show deaths"
                waypointType = waypointType.substring(0, waypointType.length() - 1);
            }
            Waypoint.Tag tag = Waypoint.Tag.fromString(waypointType);
            if (tag == null) {
                String mining = waypointType;
                Block block = ChunkPacker.stringToBlock(mining);
                //logDirect("Not a valid tag. Tags are: " + Arrays.asList(Waypoint.Tag.values()).toString().toLowerCase());
                event.cancel();
                if (block == null) {
                    logDirect("No locations for " + mining + " known, cancelling");
                    return;
                }
                List<BlockPos> locs = MineBehavior.scanFor(Collections.singletonList(block), 64);
                if (locs.isEmpty()) {
                    logDirect("No locations for " + mining + " known, cancelling");
                    return;
                }
                PathingBehavior.INSTANCE.setGoal(new GoalComposite(locs.stream().map(GoalGetToBlock::new).toArray(Goal[]::new)));
                PathingBehavior.INSTANCE.path();
                return;
            }
            Waypoint waypoint = WorldProvider.INSTANCE.getCurrentWorld().waypoints.getMostRecentByTag(tag);
            if (waypoint == null) {
                logDirect("None saved for tag " + tag);
                event.cancel();
                return;
            }
            Goal goal = new GoalBlock(waypoint.location);
            PathingBehavior.INSTANCE.setGoal(goal);
            if (!PathingBehavior.INSTANCE.path()) {
                if (!goal.isInGoal(playerFeet())) {
                    logDirect("Currently executing a path. Please cancel it first.");
                }
            }
            event.cancel();
            return;
        }
        if (msg.equals("spawn") || msg.equals("bed")) {
            Waypoint waypoint = WorldProvider.INSTANCE.getCurrentWorld().waypoints.getMostRecentByTag(Waypoint.Tag.BED);
            if (waypoint == null) {
                BlockPos spawnPoint = player().getBedLocation();
                // for some reason the default spawnpoint is underground sometimes
                Goal goal = new GoalXZ(spawnPoint.getX(), spawnPoint.getZ());
                logDirect("spawn not saved, defaulting to world spawn. set goal to " + goal);
                PathingBehavior.INSTANCE.setGoal(goal);
            } else {
                Goal goal = new GoalBlock(waypoint.location);
                PathingBehavior.INSTANCE.setGoal(goal);
                logDirect("Set goal to most recent bed " + goal);
            }
            event.cancel();
            return;
        }
        if (msg.equals("sethome")) {
            WorldProvider.INSTANCE.getCurrentWorld().waypoints.addWaypoint(new Waypoint("", Waypoint.Tag.HOME, playerFeet()));
            logDirect("Saved. Say home to set goal.");
            event.cancel();
            return;
        }
        if (msg.equals("home")) {
            Waypoint waypoint = WorldProvider.INSTANCE.getCurrentWorld().waypoints.getMostRecentByTag(Waypoint.Tag.HOME);
            if (waypoint == null) {
                logDirect("home not saved");
            } else {
                Goal goal = new GoalBlock(waypoint.location);
                PathingBehavior.INSTANCE.setGoal(goal);
                PathingBehavior.INSTANCE.path();
                logDirect("Going to saved home " + goal);
            }
            event.cancel();
            return;
        }
        // TODO
        /*if (msg.equals("costs")) {
            Movement[] movements = AStarPathFinder.getConnectedPositions(new BetterBlockPos(playerFeet()), new CalculationContext());
            List<Movement> moves = new ArrayList<>(Arrays.asList(movements));
            while (moves.contains(null)) {
                moves.remove(null);
            }
            moves.sort(Comparator.comparingDouble(movement -> movement.getCost(new CalculationContext())));
            for (Movement move : moves) {
                String[] parts = move.getClass().toString().split("\\.");
                double cost = move.getCost(new CalculationContext());
                String strCost = cost + "";
                if (cost >= ActionCosts.COST_INF) {
                    strCost = "IMPOSSIBLE";
                }
                logDirect(parts[parts.length - 1] + " " + move.getDest().getX() + "," + move.getDest().getY() + "," + move.getDest().getZ() + " " + strCost);
            }
            event.cancel();
            return;
        }*/
    }
}
