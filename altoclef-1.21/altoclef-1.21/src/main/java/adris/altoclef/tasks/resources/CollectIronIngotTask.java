package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.SmeltInBlastFurnaceTask;
import adris.altoclef.tasks.container.SmeltInFurnaceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CollectIronIngotTask extends ResourceTask {

    private final int _count;

    public CollectIronIngotTask(int count) {
        super(Items.IRON_INGOT, count);
        _count = count;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBlockTracker().trackBlock(Blocks.FURNACE, Blocks.BLAST_FURNACE);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (mod.getModSettings().shouldUseBlastFurnace()) {
            if (mod.getItemStorage().hasItem(Items.BLAST_FURNACE) ||
                    mod.getBlockTracker().anyFound(Blocks.BLAST_FURNACE) ||
                    mod.getEntityTracker().itemDropped(Items.BLAST_FURNACE)) {
                return new SmeltInBlastFurnaceTask(new SmeltTarget(new ItemTarget(Items.IRON_INGOT, _count), new ItemTarget(Items.RAW_IRON, _count)));
            }
            if (_count < 5) {
                return new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(Items.IRON_INGOT, _count), new ItemTarget(Items.RAW_IRON, _count)));
            }
            Optional<BlockPos> furnacePos = mod.getBlockTracker().getNearestTracking(Blocks.FURNACE);
            furnacePos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
            if (mod.getItemStorage().getItemCount(Items.IRON_INGOT) >= 5) {
                return TaskCatalogue.getItemTask(Items.BLAST_FURNACE, 1);
            }
            return new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(Items.IRON_INGOT, 5), new ItemTarget(Items.RAW_IRON, 5)));
        }
        return new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(Items.IRON_INGOT, _count), new ItemTarget(Items.RAW_IRON, _count)));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        mod.getBlockTracker().stopTracking(Blocks.FURNACE, Blocks.BLAST_FURNACE);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectIronIngotTask && ((CollectIronIngotTask) other)._count == _count;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " iron.";
    }
}
