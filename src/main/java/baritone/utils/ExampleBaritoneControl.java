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
import baritone.api.Settings;
import baritone.api.cache.IWaypoint;
import baritone.api.event.events.ChatEvent;
import baritone.api.pathing.goals.*;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.SettingsUtil;
import baritone.behavior.Behavior;
import baritone.behavior.FollowBehavior;
import baritone.behavior.MineBehavior;
import baritone.behavior.PathingBehavior;
import baritone.cache.ChunkPacker;
import baritone.cache.Waypoint;
import baritone.cache.WorldProvider;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.Moves;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                SettingsUtil.save(Baritone.settings());
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
                    SettingsUtil.save(Baritone.settings());
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
                        WorldProvider.INSTANCE.getCurrentWorld().getCachedWorld().queueForPacking(chunk);
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
        if (msg.equals("cancel") || msg.equals("stop")) {
            MineBehavior.INSTANCE.cancel();
            FollowBehavior.INSTANCE.cancel();
            PathingBehavior.INSTANCE.cancel();
            event.cancel();
            logDirect("ok canceled");
            return;
        }
        if (msg.equals("forcecancel")) {
            MineBehavior.INSTANCE.cancel();
            FollowBehavior.INSTANCE.cancel();
            PathingBehavior.INSTANCE.cancel();
            AbstractNodeCostSearch.forceCancel();
            PathingBehavior.INSTANCE.forceCancel();
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
            WorldProvider.INSTANCE.getCurrentWorld().getCachedWorld().reloadAllFromDisk();
            logDirect("ok");
            event.cancel();
            return;
        }
        if (msg.equals("saveall")) {
            WorldProvider.INSTANCE.getCurrentWorld().getCachedWorld().save();
            logDirect("ok");
            event.cancel();
            return;
        }
        if (msg.startsWith("find")) {
            String blockType = msg.substring(4).trim();
            LinkedList<BlockPos> locs = WorldProvider.INSTANCE.getCurrentWorld().getCachedWorld().getLocationsOf(blockType, 1, 4);
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
                Block block = ChunkPacker.stringToBlock(blockTypes[0]);
                Objects.requireNonNull(block);
                MineBehavior.INSTANCE.mine(quantity, block);
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
            Set<IWaypoint> waypoints = WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().getByTag(tag);
            // might as well show them from oldest to newest
            List<IWaypoint> sorted = new ArrayList<>(waypoints);
            sorted.sort(Comparator.comparingLong(IWaypoint::getCreationTimestamp));
            logDirect("Waypoints under tag " + tag + ":");
            for (IWaypoint waypoint : sorted) {
                logDirect(waypoint.toString());
            }
            event.cancel();
            return;
        }
        if (msg.startsWith("save")) {
            event.cancel();
            String name = msg.substring(4).trim();
            BlockPos pos = playerFeet();
            if (name.contains(" ")) {
                logDirect("Name contains a space, assuming it's in the format 'save waypointName X Y Z'");
                String[] parts = name.split(" ");
                if (parts.length != 4) {
                    logDirect("Unable to parse, expected four things");
                    return;
                }
                try {
                    pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                } catch (NumberFormatException ex) {
                    logDirect("Unable to parse coordinate integers");
                    return;
                }
                name = parts[0];
            }
            WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().addWaypoint(new Waypoint(name, Waypoint.Tag.USER, pos));
            logDirect("Saved user defined position " + pos + " under name '" + name + "'. Say 'goto user' to set goal, say 'list user' to list.");
            return;
        }
        if (msg.startsWith("goto")) {
            String waypointType = msg.substring(4).trim();
            if (waypointType.endsWith("s") && Waypoint.Tag.fromString(waypointType.substring(0, waypointType.length() - 1)) != null) {
                // for example, "show deaths"
                waypointType = waypointType.substring(0, waypointType.length() - 1);
            }
            Waypoint.Tag tag = Waypoint.Tag.fromString(waypointType);
            IWaypoint waypoint;
            if (tag == null) {
                String mining = waypointType;
                Block block = ChunkPacker.stringToBlock(mining);
                //logDirect("Not a valid tag. Tags are: " + Arrays.asList(Waypoint.Tag.values()).toString().toLowerCase());
                event.cancel();
                if (block == null) {
                    waypoint = WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().getAllWaypoints().stream().filter(w -> w.getName().equalsIgnoreCase(mining)).max(Comparator.comparingLong(IWaypoint::getCreationTimestamp)).orElse(null);
                    if (waypoint == null) {
                        logDirect("No locations for " + mining + " known, cancelling");
                        return;
                    }
                } else {
                    List<BlockPos> locs = MineBehavior.INSTANCE.scanFor(Collections.singletonList(block), 64);
                    if (locs.isEmpty()) {
                        logDirect("No locations for " + mining + " known, cancelling");
                        return;
                    }
                    PathingBehavior.INSTANCE.setGoal(new GoalComposite(locs.stream().map(GoalGetToBlock::new).toArray(Goal[]::new)));
                    PathingBehavior.INSTANCE.path();
                    return;
                }
            } else {
                waypoint = WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().getMostRecentByTag(tag);
                if (waypoint == null) {
                    logDirect("None saved for tag " + tag);
                    event.cancel();
                    return;
                }
            }
            Goal goal = new GoalBlock(waypoint.getLocation());
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
            IWaypoint waypoint = WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().getMostRecentByTag(Waypoint.Tag.BED);
            if (waypoint == null) {
                BlockPos spawnPoint = player().getBedLocation();
                // for some reason the default spawnpoint is underground sometimes
                Goal goal = new GoalXZ(spawnPoint.getX(), spawnPoint.getZ());
                logDirect("spawn not saved, defaulting to world spawn. set goal to " + goal);
                PathingBehavior.INSTANCE.setGoal(goal);
            } else {
                Goal goal = new GoalBlock(waypoint.getLocation());
                PathingBehavior.INSTANCE.setGoal(goal);
                logDirect("Set goal to most recent bed " + goal);
            }
            event.cancel();
            return;
        }
        if (msg.equals("sethome")) {
            WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("", Waypoint.Tag.HOME, playerFeet()));
            logDirect("Saved. Say home to set goal.");
            event.cancel();
            return;
        }
        if (msg.equals("home")) {
            IWaypoint waypoint = WorldProvider.INSTANCE.getCurrentWorld().getWaypoints().getMostRecentByTag(Waypoint.Tag.HOME);
            if (waypoint == null) {
                logDirect("home not saved");
            } else {
                Goal goal = new GoalBlock(waypoint.getLocation());
                PathingBehavior.INSTANCE.setGoal(goal);
                PathingBehavior.INSTANCE.path();
                logDirect("Going to saved home " + goal);
            }
            event.cancel();
            return;
        }
        if (msg.equals("costs")) {
            List<Movement> moves = Stream.of(Moves.values()).map(x -> x.apply0(playerFeet())).collect(Collectors.toCollection(ArrayList::new));
            while (moves.contains(null)) {
                moves.remove(null);
            }
            moves.sort(Comparator.comparingDouble(Movement::getCost));
            for (Movement move : moves) {
                String[] parts = move.getClass().toString().split("\\.");
                double cost = move.getCost();
                String strCost = cost + "";
                if (cost >= ActionCosts.COST_INF) {
                    strCost = "IMPOSSIBLE";
                }
                logDirect(parts[parts.length - 1] + " " + move.getDest().getX() + "," + move.getDest().getY() + "," + move.getDest().getZ() + " " + strCost);
            }
            event.cancel();
            return;
        }
        if (msg.equals("pause")) {
            boolean enabled = PathingBehavior.INSTANCE.toggle();
            logDirect("Pathing Behavior has " + (enabled ? "resumed" : "paused") + ".");
            event.cancel();
            return;
        }
        if (msg.equals("damn")) {
            logDirect("daniel");
            return;
        }
    }
}
