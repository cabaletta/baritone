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
import baritone.api.cache.IRememberedInventory;
import baritone.api.cache.IWaypoint;
import baritone.api.event.events.ChatEvent;
import baritone.api.pathing.goals.*;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.SettingsUtil;
import baritone.behavior.Behavior;
import baritone.behavior.PathingBehavior;
import baritone.cache.ChunkPacker;
import baritone.cache.Waypoint;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.Moves;
import baritone.process.CustomGoalProcess;
import baritone.utils.pathing.SegmentedCalculator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExampleBaritoneControl extends Behavior implements Helper {

    private static final String HELP_MSG =
            "baritone - Output settings into chat\n" +
                    "settings - Same as baritone\n" +
                    "goal - Create a goal (one number is '<Y>', two is '<X> <Z>', three is '<X> <Y> <Z>, 'clear' to clear)\n" +
                    "path - Go towards goal\n" +
                    "repack - (debug) Repacks chunk cache\n" +
                    "rescan - (debug) Same as repack\n" +
                    "axis - Paths towards the closest axis or diagonal axis, at y=120\n" +
                    "cancel - Cancels current path\n" +
                    "forcecancel - sudo cancel (only use if very glitched, try toggling 'pause' first)\n" +
                    "gc - Calls System.gc();\n" +
                    "invert - Runs away from the goal instead of towards it\n" +
                    "follow - Follows a player 'follow username'\n" +
                    "reloadall - (debug) Reloads chunk cache\n" +
                    "saveall - (debug) Saves chunk cache\n" +
                    "find - (debug) outputs how many blocks of a certain type are within the cache\n" +
                    "mine - Paths to and mines specified blocks 'mine x_ore y_ore ...'\n" +
                    "thisway - Creates a goal X blocks where you're facing\n" +
                    "list - Lists waypoints under a category\n" +
                    "get - Same as list\n" +
                    "show - Same as list\n" +
                    "save - Saves a waypoint (works but don't try to make sense of it)\n" +
                    "goto - Paths towards specified block or waypoint\n" +
                    "spawn - Paths towards world spawn or your most recent bed right-click\n" +
                    "sethome - Sets \"home\"\n" +
                    "home - Paths towards \"home\" \n" +
                    "costs - (debug) all movement costs from current location\n" +
                    "damn - Daniel\n" +
                    "Go to https://github.com/cabaletta/baritone/blob/master/USAGE.md for more information";

    private static final String COMMAND_PREFIX = "#";

    public ExampleBaritoneControl(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        String msg = event.getMessage();
        if (Baritone.settings().prefixControl.get() && msg.startsWith(COMMAND_PREFIX)) {
            if (!runCommand(msg.substring(COMMAND_PREFIX.length()))) {
                logDirect("Invalid command");
            }
            event.cancel(); // always cancel if using prefixControl
            return;
        }
        if (!Baritone.settings().chatControl.get() && !Baritone.settings().removePrefix.get()) {
            return;
        }
        if (runCommand(msg)) {
            event.cancel();
        }
    }

    public boolean runCommand(String msg0) { // you may think this can be private, but impcat calls it from .b =)
        String msg = msg0.toLowerCase(Locale.US).trim(); // don't reassign the argument LOL
        PathingBehavior pathingBehavior = baritone.getPathingBehavior();
        CustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        List<Settings.Setting<Boolean>> toggleable = Baritone.settings().getAllValuesByType(Boolean.class);
        for (Settings.Setting<Boolean> setting : toggleable) {
            if (msg.equalsIgnoreCase(setting.getName())) {
                setting.value ^= true;
                logDirect("Toggled " + setting.getName() + " to " + setting.value);
                SettingsUtil.save(Baritone.settings());
                return true;
            }
        }
        if (msg.equals("baritone") || msg.equals("modifiedsettings") || msg.startsWith("settings m") || msg.equals("modified")) {
            logDirect("All settings that have been modified from their default values:");
            for (Settings.Setting<?> setting : SettingsUtil.modifiedSettings(Baritone.settings())) {
                logDirect(setting.toString());
            }
            return true;
        }
        if (msg.startsWith("settings")) {
            String rest = msg.substring("settings".length());
            try {
                int page = Integer.parseInt(rest.trim());
                int min = page * 10;
                int max = Math.min(Baritone.settings().allSettings.size(), (page + 1) * 10);
                logDirect("Settings " + min + " to " + (max - 1) + ":");
                for (int i = min; i < max; i++) {
                    logDirect(Baritone.settings().allSettings.get(i).toString());
                }
            } catch (Exception ex) { // NumberFormatException | ArrayIndexOutOfBoundsException and probably some others I'm forgetting lol
                ex.printStackTrace();
                logDirect("All settings:");
                for (Settings.Setting<?> setting : Baritone.settings().allSettings) {
                    logDirect(setting.toString());
                }
                logDirect("To get one page of ten settings at a time, do settings <num>");
            }
            return true;
        }
        if (msg.equals("") || msg.equals("help") || msg.equals("?")) {
            for (String line : HELP_MSG.split("\n")) {
                logDirect(line);
            }
            return true;
        }
        if (msg.contains(" ")) {
            String settingName = msg.substring(0, msg.indexOf(' '));
            String settingValue = msg.substring(msg.indexOf(' ') + 1);
            Settings.Setting setting = Baritone.settings().byLowerName.get(settingName);
            if (setting != null) {
                if (settingValue.equals("reset")) {
                    logDirect("Resetting setting " + settingName + " to default value.");
                    setting.reset();
                } else {
                    try {
                        SettingsUtil.parseAndApply(Baritone.settings(), settingName, settingValue);
                    } catch (Exception ex) {
                        logDirect("Unable to parse setting");
                        return true;
                    }
                }
                SettingsUtil.save(Baritone.settings());
                logDirect(setting.toString());
                return true;
            }
        }
        if (Baritone.settings().byLowerName.containsKey(msg)) {
            Settings.Setting<?> setting = Baritone.settings().byLowerName.get(msg);
            logDirect(setting.toString());
            return true;
        }

        if (msg.startsWith("goal")) {
            String rest = msg.substring(4).trim();
            Goal goal;
            if (rest.equals("clear") || rest.equals("none")) {
                goal = null;
            } else {
                String[] params = rest.split(" ");
                if (params[0].equals("")) {
                    params = new String[]{};
                }
                goal = parseGoal(params);
                if (goal == null) {
                    return true;
                }
            }
            customGoalProcess.setGoal(goal);
            logDirect("Goal: " + goal);
            return true;
        }
        if (msg.equals("path")) {
            if (pathingBehavior.getGoal() == null) {
                logDirect("No goal.");
            } else if (pathingBehavior.getGoal().isInGoal(ctx.playerFeet())) {
                logDirect("Already in goal");
            } else if (pathingBehavior.isPathing()) {
                logDirect("Currently executing a path. Please cancel it first.");
            } else {
                customGoalProcess.setGoalAndPath(pathingBehavior.getGoal());
            }
            return true;
        }
        if (msg.equals("fullpath")) {
            if (pathingBehavior.getGoal() == null) {
                logDirect("No goal.");
            } else {
                logDirect("Started segmented calculator");
                SegmentedCalculator.calculateSegmentsThreaded(pathingBehavior.pathStart(), pathingBehavior.getGoal(), new CalculationContext(baritone, true), ipath -> {
                    logDirect("Found a path");
                    logDirect("Ends at " + ipath.getDest());
                    logDirect("Length " + ipath.length());
                    logDirect("Estimated time " + ipath.ticksRemainingFrom(0));
                    pathingBehavior.secretCursedFunctionDoNotCall(ipath); // it's okay when *I* do it
                }, () -> {
                    logDirect("Path calculation failed, no path");
                });
            }
            return true;
        }
        if (msg.equals("version")) {
            String version = ExampleBaritoneControl.class.getPackage().getImplementationVersion();
            if (version == null) {
                logDirect("No version detected. Either dev environment or broken install.");
            } else {
                logDirect("You are using Baritone v" + version);
            }
            return true;
        }
        if (msg.equals("repack") || msg.equals("rescan")) {
            ChunkProviderClient cli = (ChunkProviderClient) ctx.world().getChunkProvider();
            int playerChunkX = ctx.playerFeet().getX() >> 4;
            int playerChunkZ = ctx.playerFeet().getZ() >> 4;
            int count = 0;
            for (int x = playerChunkX - 40; x <= playerChunkX + 40; x++) {
                for (int z = playerChunkZ - 40; z <= playerChunkZ + 40; z++) {
                    Chunk chunk = cli.getLoadedChunk(x, z);
                    if (chunk != null) {
                        count++;
                        baritone.getWorldProvider().getCurrentWorld().getCachedWorld().queueForPacking(chunk);
                    }
                }
            }
            logDirect("Queued " + count + " chunks for repacking");
            return true;
        }
        if (msg.equals("come")) {
            customGoalProcess.setGoalAndPath(new GoalBlock(new BlockPos(mc.getRenderViewEntity())));
            logDirect("Coming");
            return true;
        }
        if (msg.equals("axis") || msg.equals("highway")) {
            customGoalProcess.setGoalAndPath(new GoalAxis());
            return true;
        }
        if (msg.equals("cancel") || msg.equals("stop")) {
            pathingBehavior.cancelEverything();
            logDirect("ok canceled");
            return true;
        }
        if (msg.equals("forcecancel")) {
            pathingBehavior.cancelEverything();
            pathingBehavior.forceCancel();
            logDirect("ok force canceled");
            return true;
        }
        if (msg.equals("gc")) {
            System.gc();
            logDirect("Called System.gc();");
            return true;
        }
        if (msg.equals("invert")) {
            Goal goal = pathingBehavior.getGoal();
            BlockPos runAwayFrom;
            if (goal instanceof GoalXZ) {
                runAwayFrom = new BlockPos(((GoalXZ) goal).getX(), 0, ((GoalXZ) goal).getZ());
            } else if (goal instanceof GoalBlock) {
                runAwayFrom = ((GoalBlock) goal).getGoalPos();
            } else {
                logDirect("Goal must be GoalXZ or GoalBlock to invert");
                logDirect("Inverting goal of player feet");
                runAwayFrom = ctx.playerFeet();
            }
            customGoalProcess.setGoalAndPath(new GoalRunAway(1, runAwayFrom) {
                @Override
                public boolean isInGoal(int x, int y, int z) {
                    return false;
                }
            });
            return true;
        }
        if (msg.equals("reset")) {
            for (Settings.Setting setting : Baritone.settings().allSettings) {
                setting.reset();
            }
            SettingsUtil.save(Baritone.settings());
            logDirect("Baritone settings reset");
            return true;
        }
        if (msg.equals("render")) {
            BetterBlockPos pf = ctx.playerFeet();
            Minecraft.getMinecraft().renderGlobal.markBlockRangeForRenderUpdate(pf.x - 500, pf.y - 500, pf.z - 500, pf.x + 500, pf.y + 500, pf.z + 500);
            logDirect("okay");
            return true;
        }
        if (msg.equals("echest")) {
            Optional<List<ItemStack>> contents = baritone.getMemoryBehavior().echest();
            if (contents.isPresent()) {
                logDirect("echest contents:");
                log(contents.get());
            } else {
                logDirect("echest contents unknown");
            }
            return true;
        }
        if (msg.equals("chests")) {
            System.out.println(baritone.getWorldProvider());
            System.out.println(baritone.getWorldProvider().getCurrentWorld());

            System.out.println(baritone.getWorldProvider().getCurrentWorld().getContainerMemory());

            System.out.println(baritone.getWorldProvider().getCurrentWorld().getContainerMemory().getRememberedInventories());

            System.out.println(baritone.getWorldProvider().getCurrentWorld().getContainerMemory().getRememberedInventories().entrySet());

            System.out.println(baritone.getWorldProvider().getCurrentWorld().getContainerMemory().getRememberedInventories().entrySet());
            for (Map.Entry<BlockPos, IRememberedInventory> entry : baritone.getWorldProvider().getCurrentWorld().getContainerMemory().getRememberedInventories().entrySet()) {
                logDirect(entry.getKey() + "");
                log(entry.getValue().getContents());
            }
            return true;
        }
        if (msg.startsWith("followplayers")) {
            baritone.getFollowProcess().follow(EntityPlayer.class::isInstance); // O P P A
            logDirect("Following any players");
            return true;
        }
        if (msg.startsWith("follow")) {
            String name = msg.substring(6).trim();
            Optional<Entity> toFollow = Optional.empty();
            if (name.length() == 0) {
                toFollow = ctx.getSelectedEntity();
            } else {
                for (EntityPlayer pl : ctx.world().playerEntities) {
                    String theirName = pl.getName().trim().toLowerCase();
                    if (!theirName.equals(ctx.player().getName().trim().toLowerCase()) && (theirName.contains(name) || name.contains(theirName))) { // don't follow ourselves lol
                        toFollow = Optional.of(pl);
                    }
                }
            }
            if (!toFollow.isPresent()) {
                logDirect("Not found");
                return true;
            }
            Entity effectivelyFinal = toFollow.get();
            baritone.getFollowProcess().follow(x -> effectivelyFinal.equals(x));
            logDirect("Following " + toFollow.get());
            return true;
        }
        if (msg.equals("reloadall")) {
            baritone.getWorldProvider().getCurrentWorld().getCachedWorld().reloadAllFromDisk();
            logDirect("ok");
            return true;
        }
        if (msg.equals("saveall")) {
            baritone.getWorldProvider().getCurrentWorld().getCachedWorld().save();
            logDirect("ok");
            return true;
        }
        if (msg.startsWith("find")) {
            String blockType = msg.substring(4).trim();
            ArrayList<BlockPos> locs = baritone.getWorldProvider().getCurrentWorld().getCachedWorld().getLocationsOf(blockType, 1, ctx.playerFeet().getX(), ctx.playerFeet().getZ(), 4);
            logDirect("Have " + locs.size() + " locations");
            for (BlockPos pos : locs) {
                Block actually = BlockStateInterface.get(ctx, pos).getBlock();
                if (!ChunkPacker.blockToString(actually).equalsIgnoreCase(blockType)) {
                    System.out.println("Was looking for " + blockType + " but actually found " + actually + " " + ChunkPacker.blockToString(actually));
                }
            }
            return true;
        }
        if (msg.startsWith("mine")) {
            String[] blockTypes = msg.substring(4).trim().split(" ");
            try {
                int quantity = Integer.parseInt(blockTypes[1]);
                Block block = ChunkPacker.stringToBlock(blockTypes[0]);
                Objects.requireNonNull(block);
                baritone.getMineProcess().mine(quantity, block);
                logDirect("Will mine " + quantity + " " + blockTypes[0]);
                return true;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException ex) {}
            for (String s : blockTypes) {
                if (ChunkPacker.stringToBlock(s) == null) {
                    logDirect(s + " isn't a valid block name");
                    return true;
                }

            }
            baritone.getMineProcess().mineByName(0, blockTypes);
            logDirect("Started mining blocks of type " + Arrays.toString(blockTypes));
            return true;
        }
        if (msg.equals("click")) {
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiClickMeme()));
                } catch (Exception ignored) {}
            }).start();
            logDirect("aight dude");
            return true;
        }
        if (msg.startsWith("thisway") || msg.startsWith("forward")) {
            try {
                Goal goal = GoalXZ.fromDirection(ctx.playerFeetAsVec(), ctx.player().rotationYaw, Double.parseDouble(msg.substring(7).trim()));
                customGoalProcess.setGoal(goal);
                logDirect("Goal: " + goal);
            } catch (NumberFormatException ex) {
                logDirect("Error unable to parse '" + msg.substring(7).trim() + "' to a double.");
            }
            return true;
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
                return true;
            }
            Set<IWaypoint> waypoints = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getByTag(tag);
            // might as well show them from oldest to newest
            List<IWaypoint> sorted = new ArrayList<>(waypoints);
            sorted.sort(Comparator.comparingLong(IWaypoint::getCreationTimestamp));
            logDirect("Waypoints under tag " + tag + ":");
            for (IWaypoint waypoint : sorted) {
                logDirect(waypoint.toString());
            }
            return true;
        }
        if (msg.startsWith("save")) {
            String name = msg.substring(4).trim();
            BlockPos pos = ctx.playerFeet();
            if (name.contains(" ")) {
                logDirect("Name contains a space, assuming it's in the format 'save waypointName X Y Z'");
                String[] parts = name.split(" ");
                if (parts.length != 4) {
                    logDirect("Unable to parse, expected four things");
                    return true;
                }
                try {
                    pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                } catch (NumberFormatException ex) {
                    logDirect("Unable to parse coordinate integers");
                    return true;
                }
                name = parts[0];
            }
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint(name, Waypoint.Tag.USER, pos));
            logDirect("Saved user defined position " + pos + " under name '" + name + "'. Say 'goto " + name + "' to set goal, say 'list user' to list custom waypoints.");
            return true;
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
                if (block == null) {
                    waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getAllWaypoints().stream().filter(w -> w.getName().equalsIgnoreCase(mining)).max(Comparator.comparingLong(IWaypoint::getCreationTimestamp)).orElse(null);
                    if (waypoint == null) {
                        Goal goal = parseGoal(waypointType.split(" "));
                        if (goal != null) {
                            logDirect("Going to " + goal);
                            customGoalProcess.setGoalAndPath(goal);
                        }
                        return true;
                    }
                } else {
                    baritone.getGetToBlockProcess().getToBlock(block);
                    return true;
                }
            } else {
                waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(tag);
                if (waypoint == null) {
                    logDirect("None saved for tag " + tag);
                    return true;
                }
            }
            Goal goal = new GoalBlock(waypoint.getLocation());
            customGoalProcess.setGoalAndPath(goal);
            return true;
        }
        if (msg.equals("spawn") || msg.equals("bed")) {
            IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(Waypoint.Tag.BED);
            if (waypoint == null) {
                BlockPos spawnPoint = ctx.player().getBedLocation();
                // for some reason the default spawnpoint is underground sometimes
                Goal goal = new GoalXZ(spawnPoint.getX(), spawnPoint.getZ());
                logDirect("spawn not saved, defaulting to world spawn. set goal to " + goal);
                customGoalProcess.setGoalAndPath(goal);
            } else {
                Goal goal = new GoalGetToBlock(waypoint.getLocation());
                customGoalProcess.setGoalAndPath(goal);
                logDirect("Set goal to most recent bed " + goal);
            }
            return true;
        }
        if (msg.equals("sethome")) {
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("", Waypoint.Tag.HOME, ctx.playerFeet()));
            logDirect("Saved. Say home to set goal.");
            return true;
        }
        if (msg.equals("home")) {
            IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(Waypoint.Tag.HOME);
            if (waypoint == null) {
                logDirect("home not saved");
            } else {
                Goal goal = new GoalBlock(waypoint.getLocation());
                customGoalProcess.setGoalAndPath(goal);
                logDirect("Going to saved home " + goal);
            }
            return true;
        }
        if (msg.equals("costs")) {
            List<Movement> moves = Stream.of(Moves.values()).map(x -> x.apply0(new CalculationContext(baritone), ctx.playerFeet())).collect(Collectors.toCollection(ArrayList::new));
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
            return true;
        }
        if (msg.equals("damn")) {
            logDirect("daniel");
        }
        return false;
    }

    private void log(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                logDirect(stack.getCount() + "x " + stack.getDisplayName() + "@" + stack.getItemDamage());
            }
        }
    }

    private Goal parseGoal(String[] params) {
        Goal goal;
        try {
            switch (params.length) {
                case 0:
                    goal = new GoalBlock(ctx.playerFeet());
                    break;
                case 1:
                    goal = new GoalYLevel(Integer.parseInt(params[0]));
                    break;
                case 2:
                    goal = new GoalXZ(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
                    break;
                case 3:
                    goal = new GoalBlock(new BlockPos(Integer.parseInt(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2])));
                    break;
                default:
                    logDirect("unable to understand lol");
                    return null;
            }
        } catch (NumberFormatException ex) {
            logDirect("unable to parse integer " + ex);
            return null;
        }
        return goal;
    }
}
