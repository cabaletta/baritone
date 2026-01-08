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

package baritone.api.process;

import baritone.api.schematic.ISchematic;
import net.minecraft.util.math.vector.Vector3i;

import java.io.File;
import java.util.Map;

/**
 * Agentic builder process that autonomously gathers materials and builds schematics
 */
public interface IAgenticBuilderProcess extends IBaritoneProcess {

    /**
     * State of the agentic builder
     */
    enum BuilderState {
        IDLE,              // Not building anything
        ANALYZING,         // Analyzing schematic for material requirements
        PLANNING,          // Planning material gathering strategy
        GATHERING,         // Gathering materials (mining, chopping, etc)
        CRAFTING,          // Crafting required items
        SMELTING,          // Smelting materials
        BUILDING,          // Building the schematic
        PAUSED,            // Paused (waiting for user or error)
        COMPLETE,          // Build completed
        ERROR              // Error state
    }

    /**
     * Start an agentic build from a schematic file
     *
     * @param name      A user-friendly name for the schematic
     * @param schematic The file path of the schematic
     * @param origin    The origin position of the schematic being built
     * @return Whether or not the build was able to start
     */
    boolean startAgenticBuild(String name, File schematic, Vector3i origin);

    /**
     * Start an agentic build from a schematic object
     *
     * @param name      A user-friendly name for the schematic
     * @param schematic The object representation of the schematic
     * @param origin    The origin position of the schematic being built
     */
    void startAgenticBuild(String name, ISchematic schematic, Vector3i origin);

    /**
     * Start an agentic build from the currently open Litematica schematic
     *
     * @param index The index of the Litematica schematic to build (usually 0)
     * @return Whether or not the build was able to start
     */
    boolean startAgenticBuildOpenLitematic(int index);

    /**
     * Stop the current agentic build
     */
    void stopAgenticBuild();

    /**
     * Pause the current agentic build
     */
    void pauseAgenticBuild();

    /**
     * Resume a paused agentic build
     */
    void resumeAgenticBuild();

    /**
     * Get the current state of the agentic builder
     *
     * @return The current BuilderState
     */
    BuilderState getState();

    /**
     * Get a human-readable status message
     *
     * @return Status message describing current activity
     */
    String getStatusMessage();

    /**
     * Get material requirements and gathering progress
     *
     * @return Map of material names to quantities (required/gathered)
     */
    Map<String, String> getMaterialProgress();

    /**
     * Get overall completion percentage
     *
     * @return Percentage complete (0-100)
     */
    int getCompletionPercentage();

    /**
     * Check if currently building
     *
     * @return True if actively building or gathering materials
     */
    boolean isActive();

    /**
     * Force skip the current gathering task and move to the next one
     * Useful if a material cannot be found or gathering is stuck
     */
    void skipCurrentTask();

    /**
     * Mark a specific material as unavailable (won't try to gather it)
     *
     * @param materialName The name of the material to skip
     */
    void markMaterialUnavailable(String materialName);
}
