/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author avecowa
 */
public class ToolSet {

    private static final Item FALLBACK_ITEM = Items.APPLE;

    /**
     * A list of tools on the hotbar that should be considered.
     * Note that if there are no tools on the hotbar this list will still have one (null) entry.
     */
    List<ItemTool> tools;
    /**
     * A mapping from the tools array to what hotbar slots the tool is actually in.
     * tools.get(i) will be on your hotbar in slot slots.get(i)
     */
    List<Byte> slots;
    /**
     * A mapping from a block to which tool index is best for it.
     * The values in this map are *not* hotbar slots indexes, they need to be looked up in slots
     * in order to be converted into hotbar slots.
     */
    Map<Block, Byte> cache = new HashMap<>();

    /**
     * Create a toolset from the current player's inventory (but don't calculate any hardness values just yet)
     */
    public ToolSet() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        tools = new ArrayList<>();
        slots = new ArrayList<>();
        boolean fnull = false;
        for (byte i = 0; i < 9; i++) {
            if (!fnull || ((!(inv.get(i).getItem() instanceof ItemAir)) && inv.get(i).getItem() instanceof ItemTool)) {
                tools.add(inv.get(i).getItem() instanceof ItemTool ? (ItemTool) inv.get(i).getItem() : null);
                slots.add(i);
                fnull |= (inv.get(i).getItem() instanceof ItemAir) || (!inv.get(i).getItem().isDamageable());
            }
        }
    }

    /**
     * A caching wrapper around getBestToolIndex
     *
     * @param b the blockstate to be mined
     * @return get which tool on the hotbar is best for mining it
     */
    public Item getBestTool(IBlockState b) {
        if (cache.get(b.getBlock()) != null) {
            return tools.get(cache.get(b.getBlock()));
        }
        return tools.get(getBestToolIndex(b));
    }

    /**
     * Calculate which tool on the hotbar is best for mining
     *
     * @param b the blockstate to be mined
     * @return a byte indicating the index in the tools array that worked best
     */
    private byte getBestToolIndex(IBlockState b) {
        byte best = 0;
        float value = -1;
        for (byte i = 0; i < tools.size(); i++) {
            Item item = tools.get(i);
            if (item == null) {
                item = FALLBACK_ITEM;
            }
            float v = item.getDestroySpeed(new ItemStack(item), b);
            if (v < value || value == -1) {
                value = v;
                best = i;
            }
        }
        cache.put(b.getBlock(), best);
        return best;
    }

    /**
     * Get which hotbar slot should be selected for fastest mining
     *
     * @param b the blockstate to be mined
     * @return a byte indicating which hotbar slot worked best
     */
    public byte getBestSlot(IBlockState b) {
        if (cache.get(b.getBlock()) != null) {
            return slots.get(cache.get(b.getBlock()));
        }
        return slots.get(getBestToolIndex(b));
    }

    /**
     * Using the best tool on the hotbar, how long would it take to mine this block
     *
     * @param b   the blockstate to be mined
     * @param pos the blockpos to be mined
     * @return how long it would take in ticks
     */
    public double getStrVsBlock(IBlockState b, BlockPos pos) {
        Item item = this.getBestTool(b);
        if (item == null) {
            item = FALLBACK_ITEM;
        }
        float f = b.getBlockHardness(Minecraft.getMinecraft().world, pos);
        return f < 0.0F ? 0.0F : (!canHarvest(b, item) ? item.getDestroySpeed(new ItemStack(item), b) / f / 100.0F : item.getDestroySpeed(new ItemStack(item), b) / f / 30.0F);
    }

    /**
     * Whether a tool can be efficiently used against a block
     *
     * @param blockIn the blockstate to be mined
     * @param item    the tool to be used
     * @return whether or not this tool would help
     */
    public boolean canHarvest(IBlockState blockIn, Item item) {
        if (blockIn.getMaterial().isToolNotRequired()) {
            return true;
        } else {
            return new ItemStack(item).canHarvestBlock(blockIn);
        }
    }
}
