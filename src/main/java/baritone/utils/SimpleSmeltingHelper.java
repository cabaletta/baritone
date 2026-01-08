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
 * Helper class for simple smelting recipes
 */
public class SimpleSmeltingHelper {

    public static class SmeltingRecipe {
        public final Item input;
        public final Item output;
        public final Item fuel;  // What to use as fuel (coal, wood, etc.)

        public SmeltingRecipe(Item input, Item output, Item fuel) {
            this.input = input;
            this.output = output;
            this.fuel = fuel;
        }
    }

    private static final Map<Item, SmeltingRecipe> recipes = new HashMap<>();

    static {
        // Stone blocks
        addRecipe(Item.getItemFromBlock(Blocks.COBBLESTONE), 
                 Item.getItemFromBlock(Blocks.STONE), Items.COAL);
        addRecipe(Item.getItemFromBlock(Blocks.STONE), 
                 Item.getItemFromBlock(Blocks.SMOOTH_STONE), Items.COAL);
        
        // Glass
        addRecipe(Item.getItemFromBlock(Blocks.SAND), 
                 Item.getItemFromBlock(Blocks.GLASS), Items.COAL);
        
        // Ores to ingots
        addRecipe(Item.getItemFromBlock(Blocks.IRON_ORE), Items.IRON_INGOT, Items.COAL);
        addRecipe(Item.getItemFromBlock(Blocks.GOLD_ORE), Items.GOLD_INGOT, Items.COAL);
        addRecipe(Item.getItemFromBlock(Blocks.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, Items.COAL);
        
        // Clay/bricks
        addRecipe(Item.getItemFromBlock(Blocks.CLAY), 
                 Items.BRICK, Items.COAL);
        
        // Netherrack to nether brick
        addRecipe(Item.getItemFromBlock(Blocks.NETHERRACK), 
                 Items.NETHER_BRICK, Items.COAL);
        
        // Terracotta
        addRecipe(Item.getItemFromBlock(Blocks.CLAY), 
                 Item.getItemFromBlock(Blocks.TERRACOTTA), Items.COAL);
        
        // Food (bonus) - using 1.16.5 names
        addRecipe(Items.BEEF, Items.COOKED_BEEF, Items.COAL);
        addRecipe(Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.COAL);
        addRecipe(Items.CHICKEN, Items.COOKED_CHICKEN, Items.COAL);
        addRecipe(Items.MUTTON, Items.COOKED_MUTTON, Items.COAL);
        addRecipe(Items.POTATO, Items.BAKED_POTATO, Items.COAL);
    }

    private static void addRecipe(Item input, Item output, Item fuel) {
        recipes.put(output, new SmeltingRecipe(input, output, fuel));
    }

    /**
     * Get smelting recipe for an item
     */
    public static SmeltingRecipe getRecipe(Item item) {
        return recipes.get(item);
    }

    /**
     * Get smelting recipe for a block
     */
    public static SmeltingRecipe getRecipe(Block block) {
        return getRecipe(Item.getItemFromBlock(block));
    }

    /**
     * Check if an item can be smelted
     */
    public static boolean canSmelt(Item item) {
        return recipes.containsKey(item);
    }

    /**
     * Check if a block can be smelted
     */
    public static boolean canSmelt(Block block) {
        return canSmelt(Item.getItemFromBlock(block));
    }
}
