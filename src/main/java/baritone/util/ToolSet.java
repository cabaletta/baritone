/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author avecowa
 */
public class ToolSet {

    public ArrayList<Item> tools;
    public ArrayList<Byte> slots;
    public HashMap<Block, Byte> cache = new HashMap<Block, Byte>();

    public ToolSet(ArrayList<Item> tools, ArrayList<Byte> slots) {
        this.tools = tools;
        this.slots = slots;
    }

    public ToolSet() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        tools = new ArrayList<Item>();
        slots = new ArrayList<Byte>();
        //Out.log("inv: " + Arrays.toString(inv));
        boolean fnull = false;
        for (byte i = 0; i < 9; i++) {
            if (!fnull || (inv.get(i) != null && inv.get(i).getItem().isItemTool(null))) {
                tools.add(inv.get(i) != null ? inv.get(i).getItem() : null);
                slots.add(i);
                fnull |= inv.get(i) == null || (!inv.get(i).getItem().isDamageable());
            }
        }
    }

    public Item getBestTool(IBlockState b) {
        if (cache.get(b.getBlock()) != null) {
            return tools.get(cache.get(b.getBlock()));
        }
        byte best = 0;
        //Out.log("best: " + best);
        float value = 0;
        for (byte i = 0; i < tools.size(); i++) {
            Item item = tools.get(i);
            if (item == null) {
                item = Item.getByNameOrId("minecraft:apple");
            }
            //Out.log(inv[i]);

            float v = item.getStrVsBlock(new ItemStack(item), b);
            //Out.log("v: " + v);
            if (v > value) {
                value = v;
                best = i;
            }
        }
        //Out.log("best: " + best);
        cache.put(b.getBlock(), best);
        return tools.get(best);
    }

    public byte getBestSlot(IBlockState b) {
        if (cache.get(b.getBlock()) != null) {
            return slots.get(cache.get(b.getBlock()));
        }
        byte best = 0;
        //Out.log("best: " + best);
        float value = 0;
        for (byte i = 0; i < tools.size(); i++) {
            Item item = tools.get(i);
            if (item == null) {
                item = Item.getByNameOrId("minecraft:apple");
            }
            //Out.log(inv[i]);
            float v = item.getStrVsBlock(new ItemStack(item), b);
            //Out.log("v: " + v);
            if (v > value) {
                value = v;
                best = i;
            }
        }
        //Out.log("best: " + best);
        cache.put(b.getBlock(), best);
        return slots.get(best);
    }

    public double getStrVsBlock(IBlockState b, BlockPos pos) {
        Item item = this.getBestTool(b);
        if (item == null) {
            item = Item.getByNameOrId("minecraft:apple");
        }
        float f = b.getBlockHardness(Minecraft.getMinecraft().world, pos);
        return f < 0.0F ? 0.0F : (!canHarvest(b, item) ? item.getStrVsBlock(new ItemStack(item), b) / f / 100.0F : item.getStrVsBlock(new ItemStack(item), b) / f / 30.0F);
    }

    public boolean canHarvest(IBlockState blockIn, Item item) {
        if (blockIn.getMaterial().isToolNotRequired()) {
            return true;
        } else {
            return new ItemStack(item).canHarvestBlock(blockIn);
        }
    }
}
