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

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.IAgenticBuilderProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.IStaticSchematic;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.MaterialSourceAnalyzer;
import baritone.utils.MaterialSourceAnalyzer.SourceInfo;
import baritone.utils.SimpleCraftingHelper;
import baritone.utils.SimpleSmeltingHelper;
import baritone.utils.schematic.format.defaults.LitematicaSchematic;
import baritone.utils.schematic.litematica.LitematicaHelper;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Agentic builder that autonomously gathers materials and builds schematics
 */
public class AgenticBuilderProcess extends BaritoneProcessHelper implements IAgenticBuilderProcess {

    private BuilderState currentState;
    private String schematicName;
    private ISchematic schematic;
    private Vector3i origin;
    
    // Material tracking
    private Map<Block, Integer> requiredMaterials;
    private Map<Block, Integer> gatheredMaterials;
    private Map<Block, SourceInfo> materialSources;
    private Set<Block> unavailableMaterials;
    
    // Task tracking
    private Block currentGatherTarget;
    private int currentGatherQuantity;
    private long gatherStartTime;
    private String statusMessage;
    
    // Process tracking
    private boolean waitingForSubProcess;
    private long subProcessStartTime;
    
    // Statistics
    private int totalBlocksRequired;
    private int totalBlocksGathered;
    private int totalBlocksPlaced;

    public AgenticBuilderProcess(Baritone baritone) {
        super(baritone);
        this.currentState = BuilderState.IDLE;
        this.requiredMaterials = new HashMap<>();
        this.gatheredMaterials = new HashMap<>();
        this.materialSources = new HashMap<>();
        this.unavailableMaterials = new HashSet<>();
        this.statusMessage = "Idle";
        this.waitingForSubProcess = false;
    }

    @Override
    public boolean startAgenticBuild(String name, File schematic, Vector3i origin) {
        if (!Baritone.settings().agenticBuilderEnabled.value) {
            logDirect("Agentic builder is disabled. Enable with #set agenticBuilderEnabled true");
            return false;
        }

        try {
            // Try to load as Litematica first, then other formats
            LitematicaSchematic litematic = new LitematicaSchematic(
                CompressedStreamTools.readCompressed(Files.newInputStream(schematic.toPath())), 
                false
            );
            startAgenticBuild(name, litematic, origin);
            return true;
        } catch (Exception e) {
            logDirect("Failed to load schematic: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void startAgenticBuild(String name, ISchematic schematic, Vector3i origin) {
        if (!Baritone.settings().agenticBuilderEnabled.value) {
            logDirect("Agentic builder is disabled. Enable with #set agenticBuilderEnabled true");
            return;
        }

        this.schematicName = name;
        this.schematic = schematic;
        this.origin = origin;
        this.currentState = BuilderState.ANALYZING;
        this.statusMessage = "Analyzing schematic...";
        
        // Reset tracking
        this.requiredMaterials.clear();
        this.gatheredMaterials.clear();
        this.unavailableMaterials.clear();
        this.totalBlocksRequired = 0;
        this.totalBlocksGathered = 0;
        this.totalBlocksPlaced = 0;
        
        logDirect("Starting agentic build: " + name);
        logDirect("Origin: " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ());
        
        // Start analysis
        analyzeSchematic();
    }

    @Override
    public boolean startAgenticBuildOpenLitematic(int index) {
        if (!Baritone.settings().agenticBuilderEnabled.value) {
            logDirect("Agentic builder is disabled. Enable with #set agenticBuilderEnabled true");
            return false;
        }

        if (!LitematicaHelper.isLitematicaPresent()) {
            logDirect("Litematica is not present");
            return false;
        }

        if (!LitematicaHelper.hasLoadedSchematic()) {
            logDirect("No schematic currently loaded");
            return false;
        }

        try {
            String name = LitematicaHelper.getName(index);
            LitematicaSchematic schematic1 = new LitematicaSchematic(
                CompressedStreamTools.readCompressed(Files.newInputStream(LitematicaHelper.getSchematicFile(index).toPath())), 
                false
            );
            Vector3i correctedOrigin = LitematicaHelper.getCorrectedOrigin(schematic1, index);
            ISchematic schematic2 = LitematicaHelper.blackMagicFuckery(schematic1, index);
            
            startAgenticBuild(name, schematic2, correctedOrigin);
            return true;
        } catch (Exception e) {
            logDirect("Failed to load Litematica schematic: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void stopAgenticBuild() {
        if (currentState != BuilderState.IDLE) {
            logDirect("Stopping agentic build: " + schematicName);
            
            // Cancel any active subprocesses
            if (baritone.getMineProcess().isActive()) {
                baritone.getMineProcess().cancel();
            }
            if (baritone.getBuilderProcess().isActive()) {
                baritone.getBuilderProcess().pause();
            }
            
            currentState = BuilderState.IDLE;
            statusMessage = "Stopped";
            waitingForSubProcess = false;
            schematic = null;
        }
    }

    @Override
    public void pauseAgenticBuild() {
        if (currentState != BuilderState.IDLE && currentState != BuilderState.PAUSED) {
            logDirect("Pausing agentic build");
            currentState = BuilderState.PAUSED;
            statusMessage = "Paused";
        }
    }

    @Override
    public void resumeAgenticBuild() {
        if (currentState == BuilderState.PAUSED) {
            logDirect("Resuming agentic build");
            currentState = BuilderState.PLANNING;
            statusMessage = "Resumed - Planning next action";
        }
    }

    @Override
    public BuilderState getState() {
        return currentState;
    }

    @Override
    public String getStatusMessage() {
        if (currentState == BuilderState.GATHERING && currentGatherTarget != null) {
            int have = gatheredMaterials.getOrDefault(currentGatherTarget, 0);
            int need = requiredMaterials.get(currentGatherTarget);
            int percent = need > 0 ? (have * 100 / need) : 0;
            return statusMessage + " [" + percent + "%]";
        }
        if (currentState == BuilderState.BUILDING) {
            return statusMessage + " [" + getCompletionPercentage() + "% overall]";
        }
        return statusMessage;
    }

    @Override
    public Map<String, String> getMaterialProgress() {
        Map<String, String> progress = new LinkedHashMap<>();
        for (Map.Entry<Block, Integer> entry : requiredMaterials.entrySet()) {
            Block block = entry.getKey();
            int required = entry.getValue();
            int gathered = gatheredMaterials.getOrDefault(block, 0);
            SourceInfo source = materialSources.get(block);
            
            String status = "";
            if (unavailableMaterials.contains(block)) {
                status = " [UNAVAILABLE]";
            } else if (gathered >= required) {
                status = " [COMPLETE]";
            } else if (block.equals(currentGatherTarget)) {
                status = " [GATHERING...]";
            } else if (source != null) {
                status = " (" + source.primarySource + ")";
            }
            
            progress.put(block.getTranslationKey(), gathered + "/" + required + status);
        }
        return progress;
    }

    @Override
    public int getCompletionPercentage() {
        if (totalBlocksRequired == 0) {
            return 0;
        }
        // Factor in both gathering and placing
        int gatherProgress = (totalBlocksGathered * 50) / totalBlocksRequired;
        int placeProgress = (totalBlocksPlaced * 50) / totalBlocksRequired;
        return Math.min(100, gatherProgress + placeProgress);
    }

    @Override
    public boolean isActive() {
        return currentState != BuilderState.IDLE && 
               currentState != BuilderState.COMPLETE && 
               currentState != BuilderState.ERROR;
    }

    @Override
    public void skipCurrentTask() {
        if (currentGatherTarget != null) {
            logDirect("Skipping gathering of: " + currentGatherTarget.getTranslationKey());
            markMaterialUnavailable(currentGatherTarget.getTranslationKey());
            currentGatherTarget = null;
        }
    }

    @Override
    public void markMaterialUnavailable(String materialName) {
        for (Block block : requiredMaterials.keySet()) {
            if (block.getTranslationKey().equals(materialName)) {
                unavailableMaterials.add(block);
                logDirect("Marked as unavailable: " + materialName);
                break;
            }
        }
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (!isActive()) {
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        // State machine execution
        switch (currentState) {
            case ANALYZING:
                // Analysis is done in startAgenticBuild, move to planning
                currentState = BuilderState.PLANNING;
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);

            case PLANNING:
                return handlePlanning();

            case GATHERING:
                return handleGathering();

            case CRAFTING:
                return handleCrafting();

            case SMELTING:
                return handleSmelting();

            case BUILDING:
                return handleBuilding();

            case PAUSED:
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);

            case ERROR:
                logDirect("Agentic builder encountered an error: " + statusMessage);
                currentState = BuilderState.IDLE;
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);

            default:
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
    }

    @Override
    public void onLostControl() {
        // Don't reset state when losing control temporarily
    }

    @Override
    public String displayName0() {
        return "Agentic Builder";
    }

    @Override
    public double priority() {
        // When waiting for subprocess, lower priority to let it run
        if (waitingForSubProcess) {
            return 1.0;
        }
        // When actively managing, higher priority
        return 6.0;
    }

    /**
     * Analyze the schematic to determine material requirements
     */
    private void analyzeSchematic() {
        logDirect("Analyzing schematic dimensions: " + 
            schematic.widthX() + "x" + schematic.heightY() + "x" + schematic.lengthZ());

        requiredMaterials.clear();
        materialSources.clear();
        totalBlocksRequired = 0;

        // Iterate through all blocks in the schematic
        for (int x = 0; x < schematic.widthX(); x++) {
            for (int y = 0; y < schematic.heightY(); y++) {
                for (int z = 0; z < schematic.lengthZ(); z++) {
                    BlockState state = schematic.desiredState(x, y, z, Blocks.AIR.getDefaultState(), new ArrayList<>());
                    if (state != null && !(state.getBlock() instanceof AirBlock)) {
                        Block block = state.getBlock();
                        requiredMaterials.put(block, requiredMaterials.getOrDefault(block, 0) + 1);
                        totalBlocksRequired++;
                        
                        // Analyze how to obtain this material
                        if (!materialSources.containsKey(block)) {
                            materialSources.put(block, MaterialSourceAnalyzer.analyze(block));
                        }
                    }
                }
            }
        }

        // Check current inventory
        for (Block block : requiredMaterials.keySet()) {
            int inInventory = countItemsInInventory(Item.getItemFromBlock(block));
            if (inInventory > 0) {
                gatheredMaterials.put(block, inInventory);
                totalBlocksGathered += inInventory;
            }
        }

        if (Baritone.settings().agenticVerboseLogging.value) {
            logDirect("Material requirements:");
            for (Map.Entry<Block, Integer> entry : requiredMaterials.entrySet()) {
                Block block = entry.getKey();
                int required = entry.getValue();
                int available = gatheredMaterials.getOrDefault(block, 0);
                SourceInfo source = materialSources.get(block);
                logDirect("  " + block.getTranslationKey() + ": " + available + "/" + required + 
                         " (Source: " + source.primarySource + ")");
            }
        }

        logDirect("Total blocks required: " + totalBlocksRequired);
        logDirect("Unique materials: " + requiredMaterials.size());
        logDirect("Already have: " + totalBlocksGathered + " blocks");
    }

    /**
     * Plan the next action based on current state
     */
    private PathingCommand handlePlanning() {
        statusMessage = "Planning next action...";

        // Check what materials we need
        if (!Baritone.settings().agenticGatherMaterials.value) {
            // Skip gathering, go straight to building
            logDirect("Material gathering disabled, starting build with available materials");
            currentState = BuilderState.BUILDING;
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        // Update inventory counts
        for (Block block : requiredMaterials.keySet()) {
            int inInventory = countItemsInInventory(Item.getItemFromBlock(block));
            gatheredMaterials.put(block, inInventory);
        }
        totalBlocksGathered = gatheredMaterials.values().stream().mapToInt(Integer::intValue).sum();

        // Find materials that need gathering (prioritize by source type)
        Block nextTarget = null;
        int highestPriority = -1;
        
        for (Map.Entry<Block, Integer> entry : requiredMaterials.entrySet()) {
            Block block = entry.getKey();
            int required = entry.getValue();
            
            if (unavailableMaterials.contains(block)) {
                continue; // Skip unavailable materials
            }

            int available = gatheredMaterials.getOrDefault(block, 0);
            
            if (available < required) {
                SourceInfo source = materialSources.get(block);
                int priority = getPriority(source.primarySource);
                
                if (priority > highestPriority) {
                    highestPriority = priority;
                    nextTarget = block;
                }
            }
        }

        if (nextTarget != null) {
            // Need to gather this material
            currentGatherTarget = nextTarget;
            currentGatherQuantity = requiredMaterials.get(nextTarget);
            currentState = BuilderState.GATHERING;
            gatherStartTime = System.currentTimeMillis();
            
            SourceInfo source = materialSources.get(nextTarget);
            statusMessage = "Gathering: " + nextTarget.getTranslationKey();
            logDirect("Next task: Gather " + nextTarget.getTranslationKey() + 
                " (need " + (currentGatherQuantity - gatheredMaterials.getOrDefault(nextTarget, 0)) + " more)" +
                " via " + source.primarySource);
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        // All materials gathered or unavailable, start building
        int totalAvailable = gatheredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        int percentAvailable = totalBlocksRequired > 0 ? (totalAvailable * 100 / totalBlocksRequired) : 0;
        
        logDirect("Material gathering complete!");
        logDirect("Have " + totalAvailable + "/" + totalBlocksRequired + " blocks (" + percentAvailable + "%)");
        
        if (percentAvailable < 50) {
            logDirect("WARNING: Only have " + percentAvailable + "% of required materials!");
        }
        
        currentState = BuilderState.BUILDING;
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    /**
     * Get priority for material source (higher = gather first)
     */
    private int getPriority(MaterialSourceAnalyzer.MaterialSource source) {
        switch (source) {
            case MINE: return 3;  // Mine first (easiest)
            case CHOP: return 2;  // Chop second
            case CRAFT: return 1; // Craft after having materials
            case SMELT: return 1; // Smelt after having materials
            default: return 0;
        }
    }

    /**
     * Handle material gathering
     */
    private PathingCommand handleGathering() {
        // Check if we're waiting for a subprocess to complete
        if (waitingForSubProcess) {
            // Check if MineProcess is still active
            if (baritone.getMineProcess().isActive()) {
                // Still mining, check timeout
                long elapsed = System.currentTimeMillis() - subProcessStartTime;
                if (elapsed > Baritone.settings().agenticGatherTimeout.value * 50) {
                    logDirect("Mining timeout for: " + currentGatherTarget.getTranslationKey());
                    baritone.getMineProcess().cancel();
                    waitingForSubProcess = false;
                    skipCurrentTask();
                    currentState = BuilderState.PLANNING;
                    return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
                }
                // Let MineProcess continue
                statusMessage = "Mining: " + currentGatherTarget.getTranslationKey() + 
                               " (" + gatheredMaterials.getOrDefault(currentGatherTarget, 0) + "/" + 
                               currentGatherQuantity + ")";
                return new PathingCommand(null, PathingCommandType.DEFER);
            } else {
                // MineProcess finished, check if we got enough
                waitingForSubProcess = false;
                int obtained = countItemsInInventory(Item.getItemFromBlock(currentGatherTarget));
                gatheredMaterials.put(currentGatherTarget, obtained);
                totalBlocksGathered = gatheredMaterials.values().stream().mapToInt(Integer::intValue).sum();
                
                int needed = requiredMaterials.get(currentGatherTarget);
                if (obtained >= needed) {
                    logDirect("Successfully gathered: " + currentGatherTarget.getTranslationKey() + 
                             " (" + obtained + "/" + needed + ")");
                } else if (obtained > 0) {
                    logDirect("Partially gathered: " + currentGatherTarget.getTranslationKey() + 
                             " (" + obtained + "/" + needed + ")");
                } else {
                    logDirect("Failed to gather: " + currentGatherTarget.getTranslationKey());
                    skipCurrentTask();
                }
                
                currentGatherTarget = null;
                currentState = BuilderState.PLANNING;
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        }

        // Start gathering if we have a target
        if (currentGatherTarget != null) {
            SourceInfo source = materialSources.get(currentGatherTarget);
            
            switch (source.primarySource) {
                case MINE:
                    return startMining(currentGatherTarget);
                    
                case CHOP:
                    return startChopping(currentGatherTarget);
                    
                case SMELT:
                    if (source.canMine) {
                        // Mine the source material first
                        logDirect("Need to smelt " + currentGatherTarget.getTranslationKey() + 
                                 ", but mining it directly instead");
                        return startMining(currentGatherTarget);
                    }
                    return startSmelting(currentGatherTarget, source.sourceBlock);
                    
                case CRAFT:
                    if (source.canMine) {
                        // Mine if possible
                        logDirect("Need to craft " + currentGatherTarget.getTranslationKey() + 
                                 ", but mining it directly instead");
                        return startMining(currentGatherTarget);
                    }
                    return startCrafting(currentGatherTarget);
                    
                default:
                    logDirect("Cannot gather: " + currentGatherTarget.getTranslationKey());
                    skipCurrentTask();
                    currentState = BuilderState.PLANNING;
                    return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        }

        // No target, back to planning
        currentState = BuilderState.PLANNING;
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    /**
     * Start mining a specific block type
     */
    private PathingCommand startMining(Block block) {
        int needed = requiredMaterials.get(block);
        int have = gatheredMaterials.getOrDefault(block, 0);
        int toMine = Math.max(1, needed - have);
        
        // Use batch size if configured
        if (Baritone.settings().agenticMinGatherBatch.value > 0) {
            toMine = Math.max(toMine, Baritone.settings().agenticMinGatherBatch.value);
        }
        
        logDirect("Starting to mine: " + block.getTranslationKey() + " (target: " + toMine + ")");
        
        // Start MineProcess
        BlockOptionalMeta[] blocks = new BlockOptionalMeta[]{new BlockOptionalMeta(block)};
        baritone.getMineProcess().mine(toMine, new BlockOptionalMetaLookup(blocks));
        
        waitingForSubProcess = true;
        subProcessStartTime = System.currentTimeMillis();
        statusMessage = "Mining: " + block.getTranslationKey();
        
        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    /**
     * Start chopping trees for wood
     */
    private PathingCommand startChopping(Block block) {
        // For now, treat wood chopping as mining
        logDirect("Chopping wood: " + block.getTranslationKey());
        return startMining(block);
    }

    /**
     * Start smelting materials
     */
    private PathingCommand startSmelting(Block target, Block source) {
        if (!Baritone.settings().agenticSmeltMaterials.value) {
            logDirect("Smelting disabled, marking as unavailable: " + target.getTranslationKey());
            skipCurrentTask();
            currentState = BuilderState.PLANNING;
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        SimpleSmeltingHelper.SmeltingRecipe recipe = SimpleSmeltingHelper.getRecipe(target);
        
        if (recipe != null) {
            // Check if we have the source material
            int have = countItemsInInventory(recipe.input);
            int need = requiredMaterials.get(target);
            
            if (have >= need) {
                // We have source material, but can't auto-smelt yet
                logDirect("Can smelt " + target.getTranslationKey() + " from " + 
                         recipe.input.getTranslationKey() + " but auto-smelting not implemented");
                logDirect("Please smelt manually: " + recipe.input.getTranslationKey() + 
                         " -> " + recipe.output.getTranslationKey());
                skipCurrentTask();
            } else {
                // Need to gather source material first
                logDirect("Need source material for smelting " + target.getTranslationKey() + 
                         ": gathering " + source.getTranslationKey());
                currentGatherTarget = source;
                return handleGathering();
            }
        } else {
            logDirect("No smelting recipe found for: " + target.getTranslationKey());
            skipCurrentTask();
        }
        
        currentState = BuilderState.PLANNING;
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    /**
     * Start crafting materials
     */
    private PathingCommand startCrafting(Block block) {
        if (!Baritone.settings().agenticCraftMaterials.value) {
            logDirect("Crafting disabled, marking as unavailable: " + block.getTranslationKey());
            skipCurrentTask();
            currentState = BuilderState.PLANNING;
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        SimpleCraftingHelper.Recipe recipe = SimpleCraftingHelper.getRecipe(block);
        
        if (recipe != null) {
            // Check if we have the ingredients
            int have = countItemsInInventory(recipe.input);
            int need = requiredMaterials.get(block) * recipe.inputCount / recipe.outputCount;
            
            if (have >= recipe.inputCount) {
                // We have ingredients, but can't auto-craft yet
                logDirect("Can craft " + block.getTranslationKey() + " from " + 
                         recipe.input.getTranslationKey() + " but auto-crafting not implemented");
                logDirect("Please craft manually: " + recipe.inputCount + "x " + 
                         recipe.input.getTranslationKey() + " -> " + 
                         recipe.outputCount + "x " + recipe.output.getTranslationKey());
                skipCurrentTask();
            } else {
                // Need to gather ingredients first
                Block ingredientBlock = MaterialSourceAnalyzer.getCraftingIngredient(block);
                logDirect("Need ingredients for " + block.getTranslationKey() + 
                         ": gathering " + ingredientBlock.getTranslationKey());
                currentGatherTarget = ingredientBlock;
                return handleGathering();
            }
        } else {
            logDirect("No recipe found for: " + block.getTranslationKey());
            skipCurrentTask();
        }
        
        currentState = BuilderState.PLANNING;
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    /**
     * Handle crafting materials
     */
    private PathingCommand handleCrafting() {
        // TODO: Implement actual crafting table interaction
        statusMessage = "Crafting (manual crafting required)";
        
        // For now, just log what needs to be crafted
        logDirect("Crafting phase - please craft materials manually");
        
        currentState = BuilderState.PLANNING;
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    /**
     * Handle smelting materials
     */
    private PathingCommand handleSmelting() {
        // TODO: Implement smelting logic
        statusMessage = "Smelting (not yet implemented)";
        currentState = BuilderState.PLANNING;
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    /**
     * Handle building the schematic
     */
    private PathingCommand handleBuilding() {
        statusMessage = "Building schematic...";
        
        // Check if BuilderProcess is already active
        if (baritone.getBuilderProcess().isActive()) {
            // BuilderProcess is running, let it continue
            statusMessage = "Building in progress...";
            return new PathingCommand(null, PathingCommandType.DEFER);
        }
        
        // Check if we just finished or need to start
        if (waitingForSubProcess) {
            // BuilderProcess finished
            logDirect("Building complete!");
            currentState = BuilderState.COMPLETE;
            waitingForSubProcess = false;
            statusMessage = "Build complete!";
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
        
        // Start BuilderProcess
        logDirect("Starting BuilderProcess...");
        baritone.getBuilderProcess().build(schematicName, schematic, origin);
        waitingForSubProcess = true;
        subProcessStartTime = System.currentTimeMillis();
        statusMessage = "Building...";
        
        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    /**
     * Count items of a specific type in inventory
     */
    private int countItemsInInventory(Item item) {
        int count = 0;
        for (int i = 0; i < ctx.player().inventory.getSizeInventory(); i++) {
            if (ctx.player().inventory.getStackInSlot(i).getItem() == item) {
                count += ctx.player().inventory.getStackInSlot(i).getCount();
            }
        }
        return count;
    }
}
