package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CollectFlintTask extends ResourceTask {
    private static final float CLOSE_ENOUGH_FLINT = 10;

    private final int _count;

    public CollectFlintTask(int targetCount) {
        super(Items.FLINT, targetCount);
        _count = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.GRAVEL);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        // We might just want to mine the closest gravel.
        Optional<BlockPos> closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), validGravel -> WorldHelper.fallingBlockSafeToBreak(validGravel) && WorldHelper.canBreak(mod, validGravel), Blocks.GRAVEL);
        if (closest.isPresent() && closest.get().isWithinDistance(mod.getPlayer().getPos(), CLOSE_ENOUGH_FLINT)) {
            return new DoToClosestBlockTask(DestroyBlockTask::new, Blocks.GRAVEL);
        }

        // If we have gravel, place it.
        if (mod.getItemStorage().hasItem(Items.GRAVEL)) {
            // Place it
            return new PlaceBlockNearbyTask(Blocks.GRAVEL);
        }

        // We don't have gravel and we need to search for flint. Grab some!
        return TaskCatalogue.getItemTask(Items.GRAVEL, 1);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.GRAVEL);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CollectFlintTask task) {
            return task._count == _count;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " flint";
    }


}
