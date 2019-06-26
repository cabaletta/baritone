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

package baritone.api.utils;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.cache.IRememberedInventory;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.*;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IGetToBlockProcess;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.nio.file.Path;
import java.util.*;

public class ExampleBaritoneControl implements Helper, AbstractGameEventListener {

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
                    "delete - Deletes a waypoint\n" +
                    "goto - Paths towards specified block or waypoint\n" +
                    "spawn - Paths towards world spawn or your most recent bed right-click\n" +
                    "sethome - Sets \"home\"\n" +
                    "home - Paths towards \"home\" \n" +
                    "costs - (debug) all movement costs from current location\n" +
                    "damn - Daniel\n" +
                    "Go to https://github.com/cabaletta/baritone/blob/master/USAGE.md for more information";

    private static final String COMMAND_PREFIX = "#";

    public final IBaritone baritone;
    public final IPlayerContext ctx;

    public ExampleBaritoneControl(IBaritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        baritone.getGameEventHandler().registerEventListener(this);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        String msg = event.getMessage();
        if (BaritoneAPI.getSettings().prefixControl.value && msg.startsWith(COMMAND_PREFIX)) {
            if (!runCommand(msg.substring(COMMAND_PREFIX.length()))) {
                logDirect("Invalid command");
            }
            event.cancel(); // always cancel if using prefixControl
            return;
        }
        if (!BaritoneAPI.getSettings().chatControl.value && !BaritoneAPI.getSettings().removePrefix.value) {
            return;
        }
        if (runCommand(msg)) {
            event.cancel();
        }
    }

    public boolean runCommand(String msg0) { // you may think this can be private, but impcat calls it from .b =)
        String msg = msg0.toLowerCase(Locale.US).trim(); // don't reassign the argument LOL
        IPathingBehavior pathingBehavior = baritone.getPathingBehavior();
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        List<Settings.Setting<Boolean>> toggleable = BaritoneAPI.getSettings().getAllValuesByType(Boolean.class);
        for (Settings.Setting<Boolean> setting : toggleable) {
            if (msg.equalsIgnoreCase(setting.getName())) {
                setting.value ^= true;
                logDirect("Toggled " + setting.getName() + " to " + setting.value);
                SettingsUtil.save(BaritoneAPI.getSettings());
                return true;
            }
        }
        if (msg.equals("baritone") || msg.equals("modifiedsettings") || msg.startsWith("settings m") || msg.equals("modified")) {
            logDirect("All settings that have been modified from their default values:");
            for (Settings.Setting<?> setting : SettingsUtil.modifiedSettings(BaritoneAPI.getSettings())) {
                logDirect(setting.toString());
            }
            return true;
        }
        if (msg.startsWith("settings")) {
            String rest = msg.substring("settings".length());
            try {
                int page = Integer.parseInt(rest.trim());
                int min = page * 10;
                int max = Math.min(BaritoneAPI.getSettings().allSettings.size(), (page + 1) * 10);
                logDirect("Settings " + min + " to " + (max - 1) + ":");
                for (int i = min; i < max; i++) {
                    logDirect(BaritoneAPI.getSettings().allSettings.get(i).toString());
                }
            } catch (Exception ex) { // NumberFormatException | ArrayIndexOutOfBoundsException and probably some others I'm forgetting lol
                ex.printStackTrace();
                logDirect("All settings:");
                for (Settings.Setting<?> setting : BaritoneAPI.getSettings().allSettings) {
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
            Settings.Setting setting = BaritoneAPI.getSettings().byLowerName.get(settingName);
            if (setting != null) {
                if (settingValue.equals("reset")) {
                    logDirect("Resetting setting " + settingName + " to default value.");
                    setting.reset();
                } else {
                    try {
                        SettingsUtil.parseAndApply(BaritoneAPI.getSettings(), settingName, settingValue);
                    } catch (Exception ex) {
                        logDirect("Unable to parse setting");
                        return true;
                    }
                }
                SettingsUtil.save(BaritoneAPI.getSettings());
                logDirect(setting.toString());
                return true;
            }
        }
        if (BaritoneAPI.getSettings().byLowerName.containsKey(msg)) {
            Settings.Setting<?> setting = BaritoneAPI.getSettings().byLowerName.get(msg);
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
        if (msg.equals("crash")) {
            StringBuilder meme = new StringBuilder();
            CrashReport rep = new CrashReport("Manually triggered debug crash", new Throwable());
            mc.addGraphicsAndWorldToCrashReport(rep);
            new ReportedException(rep).printStackTrace();
            rep.getSectionsInStringBuilder(meme);
            System.out.println(meme);
            logDirect(meme.toString());
            logDirect("ok");
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
        /*if (msg.equals("fullpath")) {
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
        }*/
        if (msg.equals("proc")) {
            Optional<IBaritoneProcess> proc = baritone.getPathingControlManager().mostRecentInControl();
            if (!proc.isPresent()) {
                logDirect("No process is in control");
                return true;
            }
            IBaritoneProcess p = proc.get();
            logDirect("Class: " + p.getClass());
            logDirect("Priority: " + p.priority());
            logDirect("Temporary: " + p.isTemporary());
            logDirect("Display name: " + p.displayName());
            logDirect("Command: " + baritone.getPathingControlManager().mostRecentCommand());
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
        if (msg.startsWith("build")) {
            String file;
            BlockPos origin;
            try {
                String[] coords = msg.substring("build".length()).trim().split(" ");
                file = coords[0] + ".schematic";
                origin = new BlockPos(parseOrDefault(coords[1], ctx.playerFeet().x), parseOrDefault(coords[2], ctx.playerFeet().y), parseOrDefault(coords[3], ctx.playerFeet().z));
            } catch (Exception ex) {
                file = msg.substring(5).trim() + ".schematic";
                origin = ctx.playerFeet();
            }
            logDirect("Loading '" + file + "' to build from origin " + origin);
            boolean success = baritone.getBuilderProcess().build(file, origin);
            logDirect(success ? "Loaded" : "Unable to load");
            return true;
        }
        if (msg.equals("come")) {
            customGoalProcess.setGoalAndPath(new GoalBlock(new BlockPos(Helper.mc.getRenderViewEntity())));
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
        if (msg.startsWith("cleararea")) {
            String suffix = msg.substring("cleararea".length());
            BlockPos corner1;
            BlockPos corner2;
            if (suffix.isEmpty()) {
                // clear the area from the current goal to here
                Goal goal = baritone.getPathingBehavior().getGoal();
                if (!(goal instanceof GoalBlock)) {
                    logDirect("Need to specify goal of opposite corner");
                    return true;
                }
                corner1 = ((GoalBlock) goal).getGoalPos();
                corner2 = ctx.playerFeet();
            } else {
                try {
                    String[] spl = suffix.split(" ");
                    corner1 = ctx.playerFeet();
                    corner2 = new BlockPos(Integer.parseInt(spl[0]), Integer.parseInt(spl[1]), Integer.parseInt(spl[2]));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException ex) {
                    logDirect("unable to parse");
                    return true;
                }
            }
            baritone.getBuilderProcess().clearArea(corner1, corner2);
            return true;
        }
        if (msg.equals("resume")) {
            baritone.getBuilderProcess().resume();
            logDirect("resumed");
            return true;
        }
        if (msg.equals("pause")) {
            baritone.getBuilderProcess().pause();
            logDirect("paused");
            return true;
        }
        if (msg.equals("reset")) {
            for (Settings.Setting setting : BaritoneAPI.getSettings().allSettings) {
                setting.reset();
            }
            SettingsUtil.save(BaritoneAPI.getSettings());
            logDirect("Baritone settings reset");
            return true;
        }
        if (msg.equals("tunnel")) {
            customGoalProcess.setGoalAndPath(new GoalStrictDirection(ctx.playerFeet(), ctx.player().getHorizontalFacing()));
            logDirect("tunneling");
            return true;
        }
        if (msg.equals("render")) {
            BetterBlockPos pf = ctx.playerFeet();
            Minecraft.getMinecraft().renderGlobal.markBlockRangeForRenderUpdate(pf.x - 500, pf.y - 500, pf.z - 500, pf.x + 500, pf.y + 500, pf.z + 500);
            logDirect("okay");
            return true;
        }
        if (msg.equals("farm")) {
            baritone.getFarmProcess().farm();
            logDirect("farming");
            return true;
        }
        if (msg.equals("chests")) {
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
            baritone.getFollowProcess().follow(effectivelyFinal::equals);
            logDirect("Following " + toFollow.get());
            return true;
        }
        if (msg.startsWith("explorefilter")) {
            // explorefilter blah.json
            // means that entries in blah.json are already explored
            // explorefilter blah.json invert
            // means that entries in blah.json are NOT already explored
            String path = msg.substring("explorefilter".length()).trim();
            String[] parts = path.split(" ");
            Path path1 = Minecraft.getMinecraft().gameDir.toPath().resolve(parts[0]);
            boolean invert = parts.length > 1;
            try {
                baritone.getExploreProcess().applyJsonFilter(path1, invert);
                logDirect("Loaded filter. Inverted: " + invert);
                if (invert) {
                    logDirect("Chunks on this list will be treated as possibly unexplored, all others will be treated as certainly explored");
                } else {
                    logDirect("Chunks on this list will be treated as certainly explored, all others will be treated as possibly unexplored");
                }
            } catch (Exception e) {
                e.printStackTrace();
                logDirect("Unable to load " + path1);
            }
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
        if (msg.startsWith("explore")) {
            String rest = msg.substring("explore".length()).trim();
            int centerX;
            int centerZ;
            try {
                centerX = Integer.parseInt(rest.split(" ")[0]);
                centerZ = Integer.parseInt(rest.split(" ")[1]);
            } catch (Exception ex) {
                centerX = ctx.playerFeet().x;
                centerZ = ctx.playerFeet().z;
            }
            baritone.getExploreProcess().explore(centerX, centerZ);
            logDirect("Exploring from " + centerX + "," + centerZ);
            return true;
        }
        if (msg.equals("blacklist")) {
            IGetToBlockProcess proc = baritone.getGetToBlockProcess();
            if (!proc.isActive()) {
                logDirect("GetToBlockProcess is not currently active");
                return true;
            }
            if (proc.blacklistClosest()) {
                logDirect("Blacklisted closest instances");
            } else {
                logDirect("No known locations, unable to blacklist");
            }
            return true;
        }
        if (msg.startsWith("find")) {
            String blockType = msg.substring(4).trim();
            ArrayList<BlockPos> locs = baritone.getWorldProvider().getCurrentWorld().getCachedWorld().getLocationsOf(blockType, 1, ctx.playerFeet().getX(), ctx.playerFeet().getZ(), 4);
            logDirect("Have " + locs.size() + " locations");
            for (BlockPos pos : locs) {
                Block actually = ctx.world().getBlockState(pos).getBlock();
                if (!BlockUtils.blockToString(actually).equalsIgnoreCase(blockType)) {
                    logDebug("Was looking for " + blockType + " but actually found " + actually + " " + BlockUtils.blockToString(actually));
                }
            }
            return true;
        }
        if (msg.startsWith("mine")) {
            String[] blockTypes = msg.substring(4).trim().split(" ");
            try {
                int quantity = Integer.parseInt(blockTypes[1]);
                Block block = BlockUtils.stringToBlockRequired(blockTypes[0]);
                baritone.getMineProcess().mine(quantity, block);
                logDirect("Will mine " + quantity + " " + blockTypes[0]);
                return true;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException ex) {}
            for (String s : blockTypes) {
                if (BlockUtils.stringToBlockNullable(s) == null) {
                    logDirect(s + " isn't a valid block name");
                    return true;
                }

            }
            baritone.getMineProcess().mineByName(0, blockTypes);
            logDirect("Started mining blocks of type " + Arrays.toString(blockTypes));
            return true;
        }
        if (msg.equals("click")) {
            baritone.openClick();
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
            IWaypoint.Tag tag = IWaypoint.Tag.fromString(waypointType);
            if (tag == null) {
                logDirect("Not a valid tag. Tags are: " + Arrays.asList(IWaypoint.Tag.values()).toString().toLowerCase());
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
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint(name, IWaypoint.Tag.USER, pos));
            logDirect("Saved user defined position " + pos + " under name '" + name + "'. Say 'goto " + name + "' to set goal, say 'list user' to list custom waypoints.");
            return true;
        }
        if (msg.startsWith("delete")) {
            String name = msg.substring(6).trim();
            IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getAllWaypoints().stream().filter(w ->  w.getTag() == IWaypoint.Tag.USER && w.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (waypoint == null) {
                logDirect("No user defined position under the name '" + name + "' found.");
                return true;
            }
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().removeWaypoint(waypoint);
            logDirect("Deleted user defined position under name '" + name + "'.");
            return true;
        }
        if (msg.startsWith("goto")) {
            String waypointType = msg.substring(4).trim();
            if (waypointType.endsWith("s") && IWaypoint.Tag.fromString(waypointType.substring(0, waypointType.length() - 1)) != null) {
                // for example, "show deaths"
                waypointType = waypointType.substring(0, waypointType.length() - 1);
            }
            IWaypoint.Tag tag = IWaypoint.Tag.fromString(waypointType);
            IWaypoint waypoint;
            if (tag == null) {
                String mining = waypointType;
                Block block = BlockUtils.stringToBlockNullable(mining);
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
            Goal goal = waypoint.getTag() == IWaypoint.Tag.BED ? new GoalGetToBlock(waypoint.getLocation()) : new GoalBlock(waypoint.getLocation());
            customGoalProcess.setGoalAndPath(goal);
            return true;
        }
        if (msg.equals("spawn") || msg.equals("bed")) {
            IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(IWaypoint.Tag.BED);
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
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("", IWaypoint.Tag.HOME, ctx.playerFeet()));
            logDirect("Saved. Say home to set goal.");
            return true;
        }
        if (msg.equals("home")) {
            IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(IWaypoint.Tag.HOME);
            if (waypoint == null) {
                logDirect("home not saved");
            } else {
                Goal goal = new GoalBlock(waypoint.getLocation());
                customGoalProcess.setGoalAndPath(goal);
                logDirect("Going to saved home " + goal);
            }
            return true;
        }
        if (msg.equals("sort")) {
            baritone.getChestSortProcess().activate();
            logDirect("Enabled sort");
            return true;
        }
        if (msg.equals("damn")) {
            logDirect("daniel");
        }
        return false;
    }

    private int parseOrDefault(String str, int i) {
        return str.equals("~") ? i : str.startsWith("~") ? Integer.parseInt(str.substring(1)) + i : Integer.parseInt(str);
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
            BetterBlockPos playerFeet = ctx.playerFeet();
            switch (params.length) {
                case 0:
                    goal = new GoalBlock(playerFeet);
                    break;
                case 1:

                    goal = new GoalYLevel(parseOrDefault(params[0], playerFeet.y));
                    break;
                case 2:
                    goal = new GoalXZ(parseOrDefault(params[0], playerFeet.x), parseOrDefault(params[1], playerFeet.z));
                    break;
                case 3:
                    goal = new GoalBlock(new BlockPos(parseOrDefault(params[0], playerFeet.x), parseOrDefault(params[1], playerFeet.y), parseOrDefault(params[2], playerFeet.z)));
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
