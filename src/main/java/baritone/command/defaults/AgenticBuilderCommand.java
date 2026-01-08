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
import baritone.api.process.IAgenticBuilderProcess;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AgenticBuilderCommand extends Command {

    public AgenticBuilderCommand(IBaritone baritone) {
        super(baritone, "agenticbuild", "agenticbuilder", "abuild");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);

        String subcommand = args.getString();

        switch (subcommand.toLowerCase()) {
            case "start":
                handleStart(args);
                break;

            case "litematic":
            case "litematica":
                handleLitematic(args);
                break;

            case "stop":
                baritone.getAgenticBuilderProcess().stopAgenticBuild();
                logDirect("Agentic build stopped");
                break;

            case "pause":
                baritone.getAgenticBuilderProcess().pauseAgenticBuild();
                logDirect("Agentic build paused");
                break;

            case "resume":
                baritone.getAgenticBuilderProcess().resumeAgenticBuild();
                logDirect("Agentic build resumed");
                break;

            case "status":
                displayStatus();
                break;

            case "materials":
            case "progress":
                displayMaterials();
                break;

            case "skip":
                baritone.getAgenticBuilderProcess().skipCurrentTask();
                logDirect("Skipped current gathering task");
                break;

            case "unavailable":
                if (args.hasExactly(1)) {
                    String materialName = args.getString();
                    baritone.getAgenticBuilderProcess().markMaterialUnavailable(materialName);
                    logDirect("Marked material as unavailable: " + materialName);
                } else {
                    logDirect("Usage: #agenticbuild unavailable <material_name>");
                }
                break;

            default:
                logDirect("Unknown subcommand: " + subcommand);
                logDirect("Usage: #agenticbuild <start|litematic|stop|pause|resume|status|materials|skip|unavailable>");
                break;
        }
    }

    private void handleStart(IArgConsumer args) throws CommandException {
        if (args.hasExactly(4)) {
            // #agenticbuild start <schematic_file> <x> <y> <z>
            String schematicName = args.getString();
            int x = args.getAs(Integer.class);
            int y = args.getAs(Integer.class);
            int z = args.getAs(Integer.class);

            File schematicFile = new File(new File(Minecraft.getInstance().gameDir, "schematics"), schematicName);
            if (!schematicFile.exists()) {
                schematicFile = new File(schematicName);
            }

            if (!schematicFile.exists()) {
                throw new CommandInvalidStateException("Schematic file not found: " + schematicName);
            }

            Vector3i origin = new Vector3i(x, y, z);
            boolean success = baritone.getAgenticBuilderProcess().startAgenticBuild(
                schematicName, 
                schematicFile, 
                origin
            );

            if (success) {
                logDirect("Started agentic build: " + schematicName);
            } else {
                logDirect("Failed to start agentic build");
            }
        } else if (args.hasExactly(1)) {
            // #agenticbuild start <schematic_file> (use player position)
            String schematicName = args.getString();
            File schematicFile = new File(new File(Minecraft.getInstance().gameDir, "schematics"), schematicName);
            if (!schematicFile.exists()) {
                schematicFile = new File(schematicName);
            }

            if (!schematicFile.exists()) {
                throw new CommandInvalidStateException("Schematic file not found: " + schematicName);
            }

            BlockPos playerPos = ctx.playerFeet();
            Vector3i origin = new Vector3i(playerPos.getX(), playerPos.getY(), playerPos.getZ());
            
            boolean success = baritone.getAgenticBuilderProcess().startAgenticBuild(
                schematicName, 
                schematicFile, 
                origin
            );

            if (success) {
                logDirect("Started agentic build: " + schematicName + " at player position");
            } else {
                logDirect("Failed to start agentic build");
            }
        } else {
            logDirect("Usage: #agenticbuild start <schematic_file> [x] [y] [z]");
        }
    }

    private void handleLitematic(IArgConsumer args) throws CommandException {
        int index = 0;
        if (args.hasExactly(1)) {
            index = args.getAs(Integer.class);
        }

        boolean success = baritone.getAgenticBuilderProcess().startAgenticBuildOpenLitematic(index);
        if (success) {
            logDirect("Started agentic build from open Litematica schematic");
        } else {
            logDirect("Failed to start agentic build from Litematica");
        }
    }

    private void displayStatus() {
        IAgenticBuilderProcess process = baritone.getAgenticBuilderProcess();
        
        logDirect("=== Agentic Builder Status ===");
        logDirect("State: " + process.getState());
        logDirect("Status: " + process.getStatusMessage());
        logDirect("Progress: " + process.getCompletionPercentage() + "%");
        logDirect("Active: " + (process.isActive() ? "Yes" : "No"));
    }

    private void displayMaterials() {
        IAgenticBuilderProcess process = baritone.getAgenticBuilderProcess();
        Map<String, String> materials = process.getMaterialProgress();

        if (materials.isEmpty()) {
            logDirect("No materials tracked (not currently building)");
            return;
        }

        logDirect("=== Material Progress ===");
        for (Map.Entry<String, String> entry : materials.entrySet()) {
            logDirect(entry.getKey() + ": " + entry.getValue());
        }
        logDirect("Overall progress: " + process.getCompletionPercentage() + "%");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return Stream.of(
                "start", 
                "litematic", 
                "stop", 
                "pause", 
                "resume", 
                "status", 
                "materials", 
                "skip", 
                "unavailable"
            );
        }

        String subcommand = args.getString();
        if (subcommand.equalsIgnoreCase("start") && args.hasExactlyOne()) {
            // Tab complete schematic files
            File schematicsDir = new File(Minecraft.getInstance().gameDir, "schematics");
            if (schematicsDir.exists() && schematicsDir.isDirectory()) {
                File[] files = schematicsDir.listFiles((dir, name) -> 
                    name.endsWith(".litematic") || name.endsWith(".schematic")
                );
                if (files != null) {
                    return Arrays.stream(files).map(File::getName);
                }
            }
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Autonomous builder with material gathering";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
            "The agentic builder autonomously gathers materials and builds schematics.",
            "",
            "Usage:",
            "> agenticbuild start <file> [x] [y] [z] - Start build from schematic file",
            "> agenticbuild litematic [index] - Start build from open Litematica",
            "> agenticbuild stop - Stop the current build",
            "> agenticbuild pause - Pause the current build",
            "> agenticbuild resume - Resume a paused build",
            "> agenticbuild status - Show current status",
            "> agenticbuild materials - Show material progress",
            "> agenticbuild skip - Skip current gathering task",
            "> agenticbuild unavailable <material> - Mark material as unavailable",
            "",
            "The builder will analyze the schematic, determine required materials,",
            "gather them automatically (mining, chopping, crafting, smelting),",
            "and then build the structure.",
            "",
            "Configure with settings like:",
            "> set agenticGatherMaterials true/false",
            "> set agenticCraftMaterials true/false",
            "> set agenticGatherRadius <blocks>"
        );
    }
}
