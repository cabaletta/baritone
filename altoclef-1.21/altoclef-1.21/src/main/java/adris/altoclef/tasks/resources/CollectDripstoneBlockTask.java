package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class CollectDripstoneBlockTask extends ResourceTask {

    private final int _count;

    public CollectDripstoneBlockTask(int targetCount) {
        super(Items.DRIPSTONE_BLOCK, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (mod.getItemStorage().getItemCount(Items.POINTED_DRIPSTONE) >= 4) {
            int target = mod.getItemStorage().getItemCount(Items.DRIPSTONE_BLOCK) + 1;
            ItemTarget s = new ItemTarget(Items.POINTED_DRIPSTONE, 1);
            return new CraftInInventoryTask(new RecipeTarget(Items.DRIPSTONE_BLOCK, target, CraftingRecipe.newShapedRecipe("dri", new ItemTarget[]{s, s, s, s}, 1)));
        }
        return new MineAndCollectTask(new ItemTarget(new Item[]{Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE}), new Block[]{Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE}, MiningRequirement.WOOD).forceDimension(Dimension.OVERWORLD);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectDripstoneBlockTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " Dripstone Blocks.";
    }
}
