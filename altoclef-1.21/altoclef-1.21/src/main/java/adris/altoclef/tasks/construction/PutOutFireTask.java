package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * Given a block position with fire in it, extinguish the fire at that position
 */
public class PutOutFireTask extends Task {

    private final BlockPos _firePosition;

    public PutOutFireTask(BlockPos firePosition) {
        _firePosition = firePosition;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        return new InteractWithBlockTask(ItemTarget.EMPTY, null, _firePosition, Input.CLICK_LEFT, false, false);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    public boolean isFinished(AltoClef mod) {
        BlockState s = mod.getWorld().getBlockState(_firePosition);
        return (s.getBlock() != Blocks.FIRE && s.getBlock() != Blocks.SOUL_FIRE);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PutOutFireTask task) {
            return (task._firePosition.equals(_firePosition));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Putting out fire at " + _firePosition;
    }
}
