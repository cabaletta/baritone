package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Finds the closest reachable block and runs a task on that block.
 */
public class DoToClosestBlockTask extends AbstractDoToClosestObjectTask<BlockPos> {

    private final Block[] _targetBlocks;

    private final Supplier<Vec3d> _getOriginPos;
    private final Function<Vec3d, Optional<BlockPos>> _getClosest;

    private final Function<BlockPos, Task> _getTargetTask;

    private final Predicate<BlockPos> _isValid;

    public DoToClosestBlockTask(Supplier<Vec3d> getOriginSupplier, Function<BlockPos, Task> getTargetTask, Function<Vec3d, Optional<BlockPos>> getClosestBlock, Predicate<BlockPos> isValid, Block... blocks) {
        _getOriginPos = getOriginSupplier;
        _getTargetTask = getTargetTask;
        _getClosest = getClosestBlock;
        _isValid = isValid;
        _targetBlocks = blocks;
    }

    public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Function<Vec3d, Optional<BlockPos>> getClosestBlock, Predicate<BlockPos> isValid, Block... blocks) {
        this(null, getTargetTask, getClosestBlock, isValid, blocks);
    }

    public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Predicate<BlockPos> isValid, Block... blocks) {
        this(null, getTargetTask, null, isValid, blocks);
    }

    public DoToClosestBlockTask(Function<BlockPos, Task> getTargetTask, Block... blocks) {
        this(getTargetTask, null, blockPos -> true, blocks);
    }

    @Override
    protected Vec3d getPos(AltoClef mod, BlockPos obj) {
        return WorldHelper.toVec3d(obj);
    }

    @Override
    protected Optional<BlockPos> getClosestTo(AltoClef mod, Vec3d pos) {
        if (_getClosest != null) {
            return _getClosest.apply(pos);
        }
        return mod.getBlockTracker().getNearestTracking(pos, _isValid, _targetBlocks);
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        if (_getOriginPos != null) {
            return _getOriginPos.get();
        }
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(BlockPos obj) {
        return _getTargetTask.apply(obj);
    }

    @Override
    protected boolean isValid(AltoClef mod, BlockPos obj) {
        // Assume we're valid since we're in the same chunk.
        if (!mod.getChunkTracker().isChunkLoaded(obj)) return true;
        // Our valid predicate
        if (_isValid != null && !_isValid.test(obj)) return false;
        // Correct block
        return mod.getBlockTracker().blockIsValid(obj, _targetBlocks);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(_targetBlocks);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_targetBlocks);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoToClosestBlockTask task) {
            return Arrays.equals(task._targetBlocks, _targetBlocks);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing something to closest block...";
    }
}