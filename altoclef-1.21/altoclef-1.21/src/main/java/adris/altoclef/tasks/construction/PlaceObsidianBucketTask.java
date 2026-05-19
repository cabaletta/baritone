package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.BlockTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Places obsidian at a position using buckets and a cast.
 */
public class PlaceObsidianBucketTask extends Task {

    public static final Vec3i[] CAST_FRAME = new Vec3i[]{
            new Vec3i(0, -1, 0),
            new Vec3i(0, -1, -1),
            new Vec3i(0, -1, 1),
            new Vec3i(-1, -1, 0),
            new Vec3i(1, -1, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(0, 0, 1),
            new Vec3i(-1, 0, 0),
            new Vec3i(1, 0, 0),
            new Vec3i(1, 1, 0)
    };
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final BlockPos _pos;

    private BlockPos _currentCastTarget;
    private BlockPos _currentDestroyTarget;

    public PlaceObsidianBucketTask(BlockPos pos) {
        _pos = pos;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        // Don't break cast
        mod.getBehaviour().avoidBlockBreaking(block -> {
            for (Vec3i castPosRelativeToLava : PlaceObsidianBucketTask.CAST_FRAME) {
                BlockPos castPos = _pos.add(castPosRelativeToLava);
                if (block.equals(castPos)) {
                    return true;
                }
            }
            return false;
        });
        // Don't place blocks inside our cast water/lava
        mod.getBehaviour().avoidBlockPlacing(block -> {
            BlockPos waterTarget = _pos.up();
            return block.equals(_pos) || block.equals(waterTarget);
        });

        _progressChecker.reset();
    }

    /**
     * This method is called periodically to perform a specific task.
     * It handles the logic for casting a spell using lava and water buckets.
     *
     * @param mod The mod instance
     * @return The next task to be executed
     */
    @Override
    protected Task onTick(AltoClef mod) {
        // Reset progress if pathing
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _progressChecker.reset();
        }

        // Clear leftover water
        if (mod.getBlockTracker().blockIsValid(_pos, Blocks.OBSIDIAN) && mod.getBlockTracker().blockIsValid(_pos.up(), Blocks.WATER)) {
            return new ClearLiquidTask(_pos.up());
        }

        // Make sure we have a water bucket
        if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
            _progressChecker.reset();
            return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
        }

        // Make sure we have a lava bucket
        if (!mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
            // The only excuse is that we have lava at our position.
            if (!mod.getBlockTracker().blockIsValid(_pos, Blocks.LAVA)) {
                _progressChecker.reset();
                return TaskCatalogue.getItemTask(Items.LAVA_BUCKET, 1);
            }
        }

        // Check progress
        if (!_progressChecker.check(mod)) {
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getBlockTracker().requestBlockUnreachable(_pos);
            _progressChecker.reset();
            return new TimeoutWanderTask(5);
        }

        // Build cast frame if not already built
        if (_currentCastTarget != null) {
            if (WorldHelper.isSolid(mod, _currentCastTarget)) {
                _currentCastTarget = null;
            } else {
                return new PlaceStructureBlockTask(_currentCastTarget);
            }
        }

        // Destroy block if needed
        if (_currentDestroyTarget != null) {
            if (!WorldHelper.isSolid(mod, _currentDestroyTarget)) {
                _currentDestroyTarget = null;
            } else {
                return new DestroyBlockTask(_currentDestroyTarget);
            }
        }

        // Build the cast frame if not already built
        if (_currentCastTarget != null && WorldHelper.isSolid(mod, _currentCastTarget)) {
            // Current cast frame already built.
            _currentCastTarget = null;
        }
        for (Vec3i castPosRelative : CAST_FRAME) {
            BlockPos castPos = _pos.add(castPosRelative);
            if (!WorldHelper.isSolid(mod, castPos)) {
                _currentCastTarget = castPos;
                return null;
            }
        }

        // Place lava
        if (mod.getWorld().getBlockState(_pos).getBlock() != Blocks.LAVA) {
            // Don't place lava at our position!
            // Would lead to an embarrassing death.
            BlockPos targetPos = _pos.add(-1, 1, 0);
            if (!mod.getPlayer().getBlockPos().equals(targetPos) && mod.getItemStorage().hasItem(Items.LAVA_BUCKET)) {
                if (!mod.getClientBaritone().getPathingBehavior().isPathing()
                        && targetPos.isWithinDistance(mod.getPlayer().getBlockPos(), 2)) {
                    mod.getInputControls().hold(Input.SNEAK);
                    mod.getInputControls().hold(Input.MOVE_BACK);
                } else {
                    mod.getInputControls().release(Input.SNEAK);
                    mod.getInputControls().release(Input.MOVE_BACK);
                    mod.getInputControls().release(Input.MOVE_FORWARD);
                }
                return new GetToBlockTask(targetPos, false);
            }
            if (WorldHelper.isSolid(mod, _pos)) {
                _currentDestroyTarget = _pos;
                return null;
            }
            // Clear the upper two as well, to make placing more reliable.
            if (WorldHelper.isSolid(mod, _pos.up())) {
                _currentDestroyTarget = _pos.up();
                return null;
            }
            if (WorldHelper.isSolid(mod, _pos.up(2))) {
                _currentDestroyTarget = _pos.up(2);
                return null;
            }
            return new InteractWithBlockTask(new ItemTarget(Items.LAVA_BUCKET, 1), Direction.WEST, _pos.add(1, 0, 0), false);
        }
        // Lava placed, Now, place water.
        BlockPos waterCheck = _pos.up();
        if (mod.getWorld().getBlockState(waterCheck).getBlock() != Blocks.WATER) {
            // Get to position to avoid weird stuck scenario
            BlockPos targetPos = _pos.add(-1, 1, 0);
            if (!mod.getPlayer().getBlockPos().equals(targetPos) && mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                return new GetToBlockTask(targetPos, false);
            }
            if (WorldHelper.isSolid(mod, waterCheck)) {
                _currentDestroyTarget = waterCheck;
                return null;
            }
            if (WorldHelper.isSolid(mod, waterCheck.up())) {
                _currentDestroyTarget = waterCheck.up();
                return null;
            }
            return new InteractWithBlockTask(new ItemTarget(Items.WATER_BUCKET, 1), Direction.WEST, _pos.add(1, 1, 0), true);
        }
        return null;
    }

    /**
     * This method is called when the task is interrupted.
     *
     * @param mod           The instance of the AltoClef class.
     * @param interruptTask The task that caused the interruption.
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Check if the mod's behaviour is not null
        if (mod.getBehaviour() != null) {
            // Pop the behaviour from the stack
            mod.getBehaviour().pop();
        }
    }

    /**
     * Check if the current task is finished.
     * The task is considered finished if the block at the specified position is obsidian
     * and there is no water block above it.
     *
     * @param mod The AltoClef mod instance.
     * @return True if the task is finished, False otherwise.
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Get the BlockTracker instance from the mod
        BlockTracker blockTracker = mod.getBlockTracker();

        // Get the position of the block to check
        BlockPos pos = _pos;

        // Check if the block at the specified position is obsidian
        boolean isObsidian = blockTracker.blockIsValid(pos, Blocks.OBSIDIAN);

        // Check if there is no water block above the specified position
        boolean isNotWaterAbove = !blockTracker.blockIsValid(pos.up(), Blocks.WATER);

        // The task is considered finished if the block is obsidian and there is no water above
        boolean isFinished = isObsidian && isNotWaterAbove;

        return isFinished;
    }

    /**
     * Checks if the given task is equal to this PlaceObsidianBucketTask.
     * Two PlaceObsidianBucketTasks are considered equal if their positions are equal.
     * Overrides the isEqual() method from the parent class.
     *
     * @param other the task to compare with
     * @return true if the tasks are equal, false otherwise
     */
    @Override
    protected boolean isEqual(Task other) {
        // Check if the other task is an instance of PlaceObsidianBucketTask
        if (other instanceof PlaceObsidianBucketTask task) {
            // Check if the positions are equal
            boolean isEqual = task.getPos().equals(getPos());
            // Return the result
            return isEqual;
        }
        // Return false
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Placing obsidian at " + _pos + " with a cast";
    }

    /**
     * Retrieves the position of the object.
     *
     * @return The position of the object.
     */
    public BlockPos getPos() {

        return _pos;
    }
}
