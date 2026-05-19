package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CollectCoarseDirtTask extends ResourceTask {

    private static final float CLOSE_ENOUGH_COARSE_DIRT = 128;
    private final int _count;

    public CollectCoarseDirtTask(int targetCount) {
        super(Items.COARSE_DIRT, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.COARSE_DIRT);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        double c = Math.ceil((double) (_count - mod.getItemStorage().getItemCount(Items.COARSE_DIRT)) / 4) * 2; // Minimum number of dirt / gravel needed to complete the recipe, accounting for coarse dirt already collected.
        Optional<BlockPos> closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.COARSE_DIRT);

        // If not enough dirt and gravel for the recipe, and coarse dirt within a certain distance, collect coarse dirt
        if (!(mod.getItemStorage().getItemCount(Items.DIRT) >= c &&
                mod.getItemStorage().getItemCount(Items.GRAVEL) >= c) &&
                closest.isPresent() && closest.get().isWithinDistance(mod.getPlayer().getPos(), CLOSE_ENOUGH_COARSE_DIRT)) {
            return new MineAndCollectTask(new ItemTarget(Items.COARSE_DIRT), new Block[]{Blocks.COARSE_DIRT}, MiningRequirement.HAND).forceDimension(Dimension.OVERWORLD);
        } else {
            int target = _count;
            ItemTarget d = new ItemTarget(Items.DIRT, 1);
            ItemTarget g = new ItemTarget(Items.GRAVEL, 1);
            return new CraftInInventoryTask(new RecipeTarget(Items.COARSE_DIRT, target, CraftingRecipe.newShapedRecipe("coarse_dirt", new ItemTarget[]{d, g, g, d}, 4)));
        }
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.COARSE_DIRT);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectCoarseDirtTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " Coarse Dirt.";
    }
}
