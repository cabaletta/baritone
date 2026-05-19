package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;

/**
 * Approaching a block can be done easily with baritone
 * but we have an issue: baritone requires that a goal be available.
 * <p>
 * For instance, say we want to approach the center of a lava pool.
 * That's not possible, so say we expect the bot to get as close as it can.
 * We have to specify the "radius", and this radius MUST be outside of the pool,
 * else baritone will get stuck and won't even try getting close.
 */
public class GetCloseToBlockTask extends Task {

    private final BlockPos _toApproach;
    private int _currentRange;

    public GetCloseToBlockTask(BlockPos toApproach) {
        _toApproach = toApproach;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _currentRange = Integer.MAX_VALUE;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Always bump the range down if we've met it.
        // We have a strictly decreasing range, which means we will eventualy get
        // as close as we can.
        if (inRange(mod)) {
            _currentRange = getCurrentDistance(mod) - 1;
        }
        return new GetWithinRangeOfBlockTask(_toApproach, _currentRange);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    private int getCurrentDistance(AltoClef mod) {
        return (int) Math.sqrt(mod.getPlayer().getBlockPos().getSquaredDistance(_toApproach));
    }

    private boolean inRange(AltoClef mod) {
        return mod.getPlayer().getBlockPos().getSquaredDistance(_toApproach) <= _currentRange * _currentRange;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetCloseToBlockTask task) {
            return task._toApproach.equals(_toApproach);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Approaching " + _toApproach.toShortString();
    }


}
