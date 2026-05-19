package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

public class CollectCobbledDeepslateTask extends ResourceTask {

    private final int _count;

    public CollectCobbledDeepslateTask(int targetCount) {
        super(Items.COBBLED_DEEPSLATE, targetCount);
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
        return new MineAndCollectTask(Items.COBBLED_DEEPSLATE, 1, new Block[]{Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE}, MiningRequirement.WOOD);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CollectCobbledDeepslateTask task) {
            return task._count == _count;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect Cobbled Deepslate";
    }
}
