package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

public class CollectWheatSeedsTask extends ResourceTask {

    private final int _count;

    public CollectWheatSeedsTask(int count) {
        super(Items.WHEAT_SEEDS, count);
        _count = count;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.WHEAT);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // If wheat block found, collect wheat but don't pick up the wheat.
        if (mod.getBlockTracker().anyFound(Blocks.WHEAT)) {
            return new CollectCropTask(Items.AIR, 999, Blocks.WHEAT, Items.WHEAT_SEEDS);
        }
        // Otherwise, break grass blocks.
        return new MineAndCollectTask(Items.WHEAT_SEEDS, _count, new Block[]{Blocks.GRASS_BLOCK, Blocks.TALL_GRASS}, MiningRequirement.HAND);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectWheatSeedsTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " wheat seeds.";
    }
}
