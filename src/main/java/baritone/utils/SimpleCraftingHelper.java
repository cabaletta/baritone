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

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for simple crafting recipes (without REI integration)
 */
public class SimpleCraftingHelper {

    public static class Recipe {
        public final Item input;
        public final int inputCount;
        public final Item output;
        public final int outputCount;
        public final boolean needsCraftingTable;

        public Recipe(Item input, int inputCount, Item output, int outputCount, boolean needsCraftingTable) {
            this.input = input;
            this.inputCount = inputCount;
            this.output = output;
            this.outputCount = outputCount;
            this.needsCraftingTable = needsCraftingTable;
        }
    }

    private static final Map<Item, Recipe> recipes = new HashMap<>();

    static {
        // Initialize simple recipes
        
        // Planks from logs (4 planks per log, no table needed)
        addRecipe(Item.getItemFromBlock(Blocks.OAK_LOG), 1, 
                 Item.getItemFromBlock(Blocks.OAK_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.SPRUCE_LOG), 1, 
                 Item.getItemFromBlock(Blocks.SPRUCE_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.BIRCH_LOG), 1, 
                 Item.getItemFromBlock(Blocks.BIRCH_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.JUNGLE_LOG), 1, 
                 Item.getItemFromBlock(Blocks.JUNGLE_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.ACACIA_LOG), 1, 
                 Item.getItemFromBlock(Blocks.ACACIA_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.DARK_OAK_LOG), 1, 
                 Item.getItemFromBlock(Blocks.DARK_OAK_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.CRIMSON_STEM), 1, 
                 Item.getItemFromBlock(Blocks.CRIMSON_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.WARPED_STEM), 1, 
                 Item.getItemFromBlock(Blocks.WARPED_PLANKS), 4, false);

        // Stripped logs to planks
        addRecipe(Item.getItemFromBlock(Blocks.STRIPPED_OAK_LOG), 1, 
                 Item.getItemFromBlock(Blocks.OAK_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.STRIPPED_SPRUCE_LOG), 1, 
                 Item.getItemFromBlock(Blocks.SPRUCE_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.STRIPPED_BIRCH_LOG), 1, 
                 Item.getItemFromBlock(Blocks.BIRCH_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.STRIPPED_JUNGLE_LOG), 1, 
                 Item.getItemFromBlock(Blocks.JUNGLE_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.STRIPPED_ACACIA_LOG), 1, 
                 Item.getItemFromBlock(Blocks.ACACIA_PLANKS), 4, false);
        addRecipe(Item.getItemFromBlock(Blocks.STRIPPED_DARK_OAK_LOG), 1, 
                 Item.getItemFromBlock(Blocks.DARK_OAK_PLANKS), 4, false);

        // Crafting table from planks (needs no table - made in inventory)
        addRecipe(Item.getItemFromBlock(Blocks.OAK_PLANKS), 4, 
                 Item.getItemFromBlock(Blocks.CRAFTING_TABLE), 1, false);

        // Storage blocks (9 items -> 1 block, needs table)
        addRecipe(Items.IRON_INGOT, 9, 
                 Item.getItemFromBlock(Blocks.IRON_BLOCK), 1, true);
        addRecipe(Items.GOLD_INGOT, 9, 
                 Item.getItemFromBlock(Blocks.GOLD_BLOCK), 1, true);
        addRecipe(Items.DIAMOND, 9, 
                 Item.getItemFromBlock(Blocks.DIAMOND_BLOCK), 1, true);
        addRecipe(Items.EMERALD, 9, 
                 Item.getItemFromBlock(Blocks.EMERALD_BLOCK), 1, true);
        addRecipe(Items.COAL, 9, 
                 Item.getItemFromBlock(Blocks.COAL_BLOCK), 1, true);
        addRecipe(Items.REDSTONE, 9, 
                 Item.getItemFromBlock(Blocks.REDSTONE_BLOCK), 1, true);
        addRecipe(Items.LAPIS_LAZULI, 9, 
                 Item.getItemFromBlock(Blocks.LAPIS_BLOCK), 1, true);
    }

    private static void addRecipe(Item input, int inputCount, Item output, int outputCount, boolean needsTable) {
        recipes.put(output, new Recipe(input, inputCount, output, outputCount, needsTable));
    }

    /**
     * Get recipe for an item
     */
    public static Recipe getRecipe(Item item) {
        return recipes.get(item);
    }

    /**
     * Get recipe for a block
     */
    public static Recipe getRecipe(Block block) {
        return getRecipe(Item.getItemFromBlock(block));
    }

    /**
     * Check if an item can be crafted
     */
    public static boolean canCraft(Item item) {
        return recipes.containsKey(item);
    }

    /**
     * Check if a block can be crafted
     */
    public static boolean canCraft(Block block) {
        return canCraft(Item.getItemFromBlock(block));
    }

    /**
     * Calculate how many times we can craft with available materials
     */
    public static int calculateCraftableCount(Item output, int availableInput) {
        Recipe recipe = getRecipe(output);
        if (recipe == null) {
            return 0;
        }
        int batches = availableInput / recipe.inputCount;
        return batches * recipe.outputCount;
    }
}
