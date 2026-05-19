package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

// TODO: This can technically be removed, as it's a mine task followed by a collect task.
public class CollectHayBlockTask extends ResourceTask {

    private final int _count;

    public CollectHayBlockTask(int count) {
        super(Items.HAY_BLOCK, count);
        _count = count;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.HAY_BLOCK);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        if (mod.getBlockTracker().anyFound(Blocks.HAY_BLOCK)) {
            return new MineAndCollectTask(Items.HAY_BLOCK, _count, new Block[]{Blocks.HAY_BLOCK}, MiningRequirement.HAND);
        }

        ItemTarget w = new ItemTarget(Items.WHEAT, 1);
        return new CraftInTableTask(new RecipeTarget(Items.HAY_BLOCK, _count, CraftingRecipe.newShapedRecipe("hay_block", new ItemTarget[]{w, w, w, w, w, w, w, w, w}, 1)));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.HAY_BLOCK);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectHayBlockTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " hay blocks.";
    }
}
