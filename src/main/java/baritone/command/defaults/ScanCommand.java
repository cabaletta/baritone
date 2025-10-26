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
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.utils.BetterBlockPos;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ScanCommand extends Command {

    public ScanCommand(IBaritone baritone) {
        super(baritone, "scan", "blockscan", "search");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);

        // Check for utility commands first
        String firstArg = args.peekString();
        
        if (firstArg.equalsIgnoreCase("stop")) {
            args.get();
            baritone.getBlockScanProcess().forceStop();
            baritone.getPathingBehavior().cancelEverything();
            logDirect("Scan stopped and cancelled");
            return;
        } else if (firstArg.equalsIgnoreCase("clear")) {
            args.get();
            baritone.getBlockScanProcess().clearResults();
            logDirect("Scan results cleared");
            return;
        } else if (firstArg.equalsIgnoreCase("list")) {
            args.get();
            displayResults();
            return;
        } else if (firstArg.equalsIgnoreCase("progress")) {
            args.get();
            logDirect(baritone.getBlockScanProcess().getProgressString());
            return;
        }

        // Check if first argument is a mode flag
        boolean scanAll = true; // Default: scan entire area
        if (args.hasAny()) {
            if (firstArg.equalsIgnoreCase("-first") || firstArg.equalsIgnoreCase("-stop")) {
                scanAll = false;
                args.get(); // consume the flag
            } else if (firstArg.equalsIgnoreCase("-all") || firstArg.equalsIgnoreCase("-complete")) {
                scanAll = true;
                args.get(); // consume the flag
            }
        }

        // Parse blocks to search for
        List<Block> blocks = new ArrayList<>();
        
        // Check if it's a JSON array
        String potentialJson = args.peekString();
        if (potentialJson.startsWith("[")) {
            // Parse JSON array
            blocks = parseJsonBlockList(args.getString());
        } else {
            // Parse individual block names
            while (args.hasAny() && !isCoordinate(args.peekString())) {
                String blockName = args.getString();
                Block block = getBlockByName(blockName);
                if (block != null) {
                    blocks.add(block);
                } else {
                    logDirect("Warning: Unknown block '" + blockName + "', skipping");
                }
            }
        }

        if (blocks.isEmpty()) {
            throw new CommandInvalidTypeException(args.consumed(), "at least one valid block");
        }

        // Parse search area (radius or region)
        if (!args.hasAny()) {
            // Default: 64 block radius
            baritone.getBlockScanProcess().startScanRadius(blocks, 64, scanAll);
            logDirect("Scanning 64 block radius for " + blocks.size() + " block type(s)");
        } else if (args.hasExactly(1)) {
            // Radius mode
            int radius = args.getAs(Integer.class);
            baritone.getBlockScanProcess().startScanRadius(blocks, radius, scanAll);
            logDirect("Scanning " + radius + " block radius for " + blocks.size() + " block type(s)");
        } else if (args.has(6)) {
            // Region mode: x1 y1 z1 x2 y2 z2
            int x1 = args.getAs(Integer.class);
            int y1 = args.getAs(Integer.class);
            int z1 = args.getAs(Integer.class);
            int x2 = args.getAs(Integer.class);
            int y2 = args.getAs(Integer.class);
            int z2 = args.getAs(Integer.class);

            BlockPos pos1 = new BlockPos(x1, y1, z1);
            BlockPos pos2 = new BlockPos(x2, y2, z2);

            baritone.getBlockScanProcess().startScanRegion(blocks, pos1, pos2, scanAll);
            logDirect("Scanning region from " + pos1 + " to " + pos2);
        } else {
            throw new CommandInvalidTypeException(args.consumed(), 
                "radius (1 number) or region (6 numbers: x1 y1 z1 x2 y2 z2)");
        }
    }

    private void displayResults() {
        Map<Block, List<BetterBlockPos>> foundBlocks = baritone.getBlockScanProcess().getFoundBlocks();
        
        if (foundBlocks.isEmpty()) {
            logDirect("No scan results available");
            return;
        }

        logDirect("=== Scan Results ===");
        int totalFound = 0;
        for (Map.Entry<Block, List<BetterBlockPos>> entry : foundBlocks.entrySet()) {
            List<BetterBlockPos> positions = entry.getValue();
            if (!positions.isEmpty()) {
                logDirect(entry.getKey().getTranslationKey() + ": " + positions.size() + " found");
                for (BetterBlockPos pos : positions) {
                    logDirect("  - X: " + pos.x + " Y: " + pos.y + " Z: " + pos.z);
                }
                totalFound += positions.size();
            }
        }
        
        if (totalFound == 0) {
            logDirect("No blocks found yet");
        } else {
            logDirect("Total: " + totalFound + " blocks");
        }
    }

    private List<Block> parseJsonBlockList(String json) throws CommandException {
        List<Block> blocks = new ArrayList<>();
        try {
            JsonArray array = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement element : array) {
                String blockName = element.getAsString();
                Block block = getBlockByName(blockName);
                if (block != null) {
                    blocks.add(block);
                } else {
                    logDirect("Warning: Unknown block '" + blockName + "' in JSON, skipping");
                }
            }
        } catch (Exception e) {
            throw new CommandInvalidTypeException(null, "valid JSON array of block names");
        }
        return blocks;
    }

    private Block getBlockByName(String name) {
        // Try with minecraft: prefix
        ResourceLocation resourceLocation;
        if (name.contains(":")) {
            resourceLocation = new ResourceLocation(name);
        } else {
            resourceLocation = new ResourceLocation("minecraft", name);
        }

        // Try to get the block from registry
        if (Registry.BLOCK.containsKey(resourceLocation)) {
            return Registry.BLOCK.getOrDefault(resourceLocation);
        }

        // Try without namespace
        resourceLocation = new ResourceLocation("minecraft", name.toLowerCase().replace(" ", "_"));
        if (Registry.BLOCK.containsKey(resourceLocation)) {
            return Registry.BLOCK.getOrDefault(resourceLocation);
        }

        return null;
    }

    private boolean isCoordinate(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return str.equals("~") || str.startsWith("~");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        if (args.hasExactly(0)) {
            return Stream.of("stop", "clear", "list", "progress", "-first", "-all", "spawner", "diamond_ore", "ancient_debris");
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Scan area for specific blocks";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
            "The scan command searches for specific blocks in an area.",
            "",
            "Usage:",
            "> scan <block> [radius] - Scan radius around player (default 64)",
            "> scan <block1> <block2> ... [radius] - Scan for multiple blocks",
            "> scan [\"block1\",\"block2\",...] [radius] - Scan using JSON list",
            "> scan <block> <x1> <y1> <z1> <x2> <y2> <z2> - Scan rectangular region",
            "",
            "Control:",
            "> scan stop - Stop the current scan",
            "> scan clear - Clear scan results",
            "> scan list - Display found blocks",
            "> scan progress - Show scan progress",
            "",
            "Modes:",
            "> -first or -stop - Stop at first match",
            "> -all or -complete - Scan entire area (default)",
            "",
            "Examples:",
            "> scan spawner - Scan for spawners in 64 block radius",
            "> scan -first diamond_ore 100 - Find first diamond in 100 block radius",
            "> scan spawner ancient_debris 200 - Find spawners and ancient debris",
            "> scan [\"spawner\",\"diamond_ore\"] 150 - JSON format for multiple blocks",
            "> scan chest 0 60 0 100 70 100 - Scan rectangular region",
            "> scan progress - Check current progress",
            "> scan stop - Stop scanning",
            "",
            "The process will pathfind through the area systematically and report findings.",
            "Progress is automatically reported every 5 seconds during scanning."
        );
    }
}
