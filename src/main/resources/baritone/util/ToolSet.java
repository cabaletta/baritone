package baritone.util;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author avecowa
 */
public class ToolSet {

    public ArrayList<ItemTool> tools;
    public ArrayList<Byte> slots;
    public HashMap<Block, Byte> cache = new HashMap<Block, Byte>();

    public ToolSet(ArrayList<ItemTool> tools, ArrayList<Byte> slots) {
        this.tools = tools;
        this.slots = slots;
    }

    public ToolSet() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        tools = new ArrayList<ItemTool>();
        slots = new ArrayList<Byte>();
        //Out.log("inv: " + Arrays.toString(inv));
        boolean fnull = false;
        for (byte i = 0; i < 9; i++) {
            if (!fnull || ((!(inv.get(i).getItem() instanceof ItemAir)) && inv.get(i).getItem() instanceof ItemTool)) {
                tools.add((!(inv.get(i).getItem() instanceof ItemAir)) ? (ItemTool) inv.get(i).getItem() : null);
                slots.add(i);
                fnull |= (inv.get(i).getItem() instanceof ItemAir) || (!inv.get(i).getItem().isDamageable());
            }
        }
    }

    public Item getBestTool(IBlockState b) {
        if (cache.get(b.getBlock()) != null) {
            return tools.get(cache.get(b.getBlock()));
        }
        return tools.get(getBestToolIndex(b));
    }

    private byte getBestToolIndex(IBlockState b) {
        byte best = 0;
        //Out.log("best: " + best);
        float value = -1;
        for (byte i = 0; i < tools.size(); i++) {
            Item item = tools.get(i);
            if (item == null) {
                item = Item.getByNameOrId("minecraft:apple");
            }
            //Out.log(inv[i]);

            float v = item.getDestroySpeed(new ItemStack(item), b);
            //Out.log("v: " + v);
            if (v < value || value == -1) {
                value = v;
                best = i;
            }
        }
        //Out.log("best: " + best);
        cache.put(b.getBlock(), best);
        return best;
    }

    public byte getBestSlot(IBlockState b) {
        if (cache.get(b.getBlock()) != null) {
            return slots.get(cache.get(b.getBlock()));
        }
        return slots.get(getBestToolIndex(b));
    }

    public double getStrVsBlock(IBlockState b, BlockPos pos) {
        Item item = this.getBestTool(b);
        if (item == null) {
            item = Item.getByNameOrId("minecraft:apple");
        }
        float f = b.getBlockHardness(Minecraft.getMinecraft().world, pos);
        return f < 0.0F ? 0.0F : (!canHarvest(b, item) ? item.getDestroySpeed(new ItemStack(item), b) / f / 100.0F : item.getDestroySpeed(new ItemStack(item), b) / f / 30.0F);
    }

    public boolean canHarvest(IBlockState blockIn, Item item) {
        if (blockIn.getMaterial().isToolNotRequired()) {
            return true;
        } else {
            return new ItemStack(item).canHarvestBlock(blockIn);
        }
    }
}
