/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
        ItemStack[] inv = p.inventory.mainInventory;
        tools = new ArrayList<Item>();
        slots = new ArrayList<Byte>();
        //Out.log("inv: " + Arrays.toString(inv));
        boolean fnull = false;
        for (byte i = 0; i < 9; i++) {
            if (!fnull || (inv[i] != null && inv[i].getItem().isItemTool(null))) {
                tools.add(inv[i] != null ? inv[i].getItem() : null);
                slots.add(i);
                fnull |= inv[i] == null || (!inv[i].getItem().isDamageable());
            }
        }
    }
    public Item getBestTool(Block b) {
        if (cache.get(b) != null) {
            return tools.get(cache.get(b));
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
        cache.put(b, best);
        return tools.get(best);
    }
    public byte getBestSlot(Block b) {
        if (cache.get(b) != null) {
            return slots.get(cache.get(b));
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
        cache.put(b, best);
        return slots.get(best);
    }
    public double getStrVsBlock(Block b, BlockPos pos) {
        Item item = this.getBestTool(b);
        if (item == null) {
            item = Item.getByNameOrId("minecraft:apple");
        }
        float f = b.getBlockHardness(Minecraft.getMinecraft().world, pos);
        return f < 0.0F ? 0.0F : (!canHarvest(b, item) ? item.getStrVsBlock(new ItemStack(item), b) / f / 100.0F : item.getStrVsBlock(new ItemStack(item), b) / f / 30.0F);
    }
    public boolean canHarvest(Block blockIn, Item item) {
        if (blockIn.getMaterial().isToolNotRequired()) {
            return true;
        } else {
            return new ItemStack(item).canHarvestBlock(blockIn);
        }
    }
}
