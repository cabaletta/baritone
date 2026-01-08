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

import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;

/**
 * Utility class to determine how materials can be obtained
 */
public class MaterialSourceAnalyzer {

    public enum MaterialSource {
        INVENTORY,      // Already in inventory
        MINE,          // Needs to be mined from the world
        CHOP,          // Wood that needs to be chopped
        CRAFT,         // Can be crafted from other materials
        SMELT,         // Can be smelted from other materials
        UNAVAILABLE    // Cannot be obtained automatically
    }

    public static class SourceInfo {
        public final MaterialSource primarySource;
        public final Block sourceBlock;  // For smelting/crafting: what to obtain
        public final boolean canMine;
        public final boolean canChop;
        public final boolean canCraft;
        public final boolean canSmelt;

        public SourceInfo(MaterialSource primarySource, Block sourceBlock, 
                         boolean canMine, boolean canChop, boolean canCraft, boolean canSmelt) {
            this.primarySource = primarySource;
            this.sourceBlock = sourceBlock;
            this.canMine = canMine;
            this.canChop = canChop;
            this.canCraft = canCraft;
            this.canSmelt = canSmelt;
        }
    }

    /**
     * Determine how to obtain a specific block
     */
    public static SourceInfo analyze(Block block) {
        // Check if it's a wood type
        if (isWood(block)) {
            return new SourceInfo(MaterialSource.CHOP, block, false, true, false, false);
        }

        // Check if it can be smelted
        Block smeltSource = getSmeltingSource(block);
        if (smeltSource != null) {
            boolean canAlsoMine = canBeMined(block);
            return new SourceInfo(
                canAlsoMine ? MaterialSource.MINE : MaterialSource.SMELT, 
                smeltSource, 
                canAlsoMine, 
                false, 
                false, 
                true
            );
        }

        // Check if it can be crafted
        if (canBeCrafted(block)) {
            boolean canAlsoMine = canBeMined(block);
            return new SourceInfo(
                canAlsoMine ? MaterialSource.MINE : MaterialSource.CRAFT,
                block,
                canAlsoMine,
                false,
                true,
                false
            );
        }

        // Check if it can be mined
        if (canBeMined(block)) {
            return new SourceInfo(MaterialSource.MINE, block, true, false, false, false);
        }

        // Cannot obtain automatically
        return new SourceInfo(MaterialSource.UNAVAILABLE, block, false, false, false, false);
    }

    /**
     * Check if a block is a wood type
     */
    private static boolean isWood(Block block) {
        // Check for logs
        if (block instanceof RotatedPillarBlock) {
            String name = block.getTranslationKey();
            if (name.contains("log") || name.contains("wood")) {
                return true;
            }
        }

        // Specific wood blocks
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || 
               block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG ||
               block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG ||
               block == Blocks.CRIMSON_STEM || block == Blocks.WARPED_STEM ||
               block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_SPRUCE_LOG ||
               block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_JUNGLE_LOG ||
               block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_DARK_OAK_LOG ||
               block == Blocks.OAK_PLANKS || block == Blocks.SPRUCE_PLANKS ||
               block == Blocks.BIRCH_PLANKS || block == Blocks.JUNGLE_PLANKS ||
               block == Blocks.ACACIA_PLANKS || block == Blocks.DARK_OAK_PLANKS ||
               block == Blocks.CRIMSON_PLANKS || block == Blocks.WARPED_PLANKS;
    }

    /**
     * Get what needs to be smelted to obtain this block
     */
    private static Block getSmeltingSource(Block block) {
        // Stone blocks
        if (block == Blocks.STONE) return Blocks.COBBLESTONE;
        if (block == Blocks.SMOOTH_STONE) return Blocks.STONE;
        if (block == Blocks.GLASS) return Blocks.SAND;
        
        // Ores to ingots (blocks)
        if (block == Blocks.IRON_BLOCK) return Blocks.IRON_ORE;
        if (block == Blocks.GOLD_BLOCK) return Blocks.GOLD_ORE;
        
        // Bricks and such
        if (block == Blocks.BRICKS) return Blocks.CLAY;
        if (block == Blocks.NETHER_BRICKS || block == Blocks.NETHER_BRICK_FENCE || 
            block == Blocks.NETHER_BRICK_STAIRS) return Blocks.NETHERRACK;
        
        // Terracotta
        if (block instanceof GlazedTerracottaBlock || block == Blocks.TERRACOTTA) {
            return Blocks.CLAY;
        }

        return null;
    }

    /**
     * Check if a block can be crafted
     */
    private static boolean canBeCrafted(Block block) {
        // Planks from logs
        if (block == Blocks.OAK_PLANKS || block == Blocks.SPRUCE_PLANKS ||
            block == Blocks.BIRCH_PLANKS || block == Blocks.JUNGLE_PLANKS ||
            block == Blocks.ACACIA_PLANKS || block == Blocks.DARK_OAK_PLANKS ||
            block == Blocks.CRIMSON_PLANKS || block == Blocks.WARPED_PLANKS) {
            return true;
        }

        // Crafting table
        if (block == Blocks.CRAFTING_TABLE) return true;

        // Storage blocks
        if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK || 
            block == Blocks.DIAMOND_BLOCK || block == Blocks.EMERALD_BLOCK ||
            block == Blocks.COAL_BLOCK || block == Blocks.REDSTONE_BLOCK ||
            block == Blocks.LAPIS_BLOCK || block == Blocks.QUARTZ_BLOCK) {
            return true;
        }

        // Stairs, slabs, fences, walls, etc. (crafted from base blocks)
        if (block instanceof StairsBlock || block instanceof SlabBlock ||
            block instanceof FenceBlock || block instanceof WallBlock ||
            block instanceof FenceGateBlock || block instanceof DoorBlock ||
            block instanceof TrapDoorBlock) {
            return true;
        }

        // Wool and carpet
        if (block instanceof AbstractButtonBlock || block instanceof PressurePlateBlock) {
            return true;
        }

        return false;
    }

    /**
     * Check if a block can be mined from the world
     */
    private static boolean canBeMined(Block block) {
        // Cannot mine air or liquids
        if (block instanceof AirBlock || block instanceof FlowingFluidBlock) {
            return false;
        }

        // Cannot mine bedrock or other unbreakable blocks
        if (block == Blocks.BEDROCK || block == Blocks.BARRIER || 
            block == Blocks.COMMAND_BLOCK || block == Blocks.END_PORTAL_FRAME ||
            block == Blocks.END_PORTAL || block == Blocks.NETHER_PORTAL) {
            return false;
        }

        // Most natural blocks can be mined
        if (block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK ||
            block == Blocks.COBBLESTONE || block == Blocks.SAND || block == Blocks.GRAVEL ||
            block == Blocks.NETHERRACK || block == Blocks.END_STONE) {
            return true;
        }

        // Ores
        if (block instanceof OreBlock || 
            block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || block == Blocks.GOLD_ORE ||
            block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE || block == Blocks.LAPIS_ORE ||
            block == Blocks.REDSTONE_ORE || block == Blocks.NETHER_QUARTZ_ORE ||
            block == Blocks.NETHER_GOLD_ORE || block == Blocks.ANCIENT_DEBRIS) {
            return true;
        }

        // Clay, ice, etc.
        if (block == Blocks.CLAY || block == Blocks.ICE || block == Blocks.PACKED_ICE ||
            block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
            return true;
        }

        // Assume most other solid blocks can be mined
        return !block.getDefaultState().isAir();
    }

    /**
     * Get the crafting ingredient for a block (simplified)
     */
    public static Block getCraftingIngredient(Block block) {
        // Planks come from logs
        if (block == Blocks.OAK_PLANKS) return Blocks.OAK_LOG;
        if (block == Blocks.SPRUCE_PLANKS) return Blocks.SPRUCE_LOG;
        if (block == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_LOG;
        if (block == Blocks.JUNGLE_PLANKS) return Blocks.JUNGLE_LOG;
        if (block == Blocks.ACACIA_PLANKS) return Blocks.ACACIA_LOG;
        if (block == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_LOG;

        // Stairs/slabs are made from their base block (simplified)
        if (block instanceof StairsBlock) {
            String name = block.getTranslationKey();
            if (name.contains("oak")) return Blocks.OAK_PLANKS;
            if (name.contains("stone")) return Blocks.STONE;
            if (name.contains("cobblestone")) return Blocks.COBBLESTONE;
            // Add more as needed
        }

        return block; // Default: same as output
    }
}
