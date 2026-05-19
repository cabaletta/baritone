package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.BlockRange;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class StoreInStashTask extends Task {

    // There's a lot of code duplication here...
    private static final Block[] TO_SCAN = Stream.concat(Arrays.stream(new Block[]{Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL}), Arrays.stream(ItemHelper.itemsToBlocks(ItemHelper.SHULKER_BOXES))).toArray(Block[]::new);
    private final ItemTarget[] _toStore;
    private final boolean _getIfNotPresent;
    private final BlockRange _stashRange;
    private ContainerStoredTracker _storedItems;

    public StoreInStashTask(boolean getIfNotPresent, BlockRange stashRange, ItemTarget... toStore) {
        _getIfNotPresent = getIfNotPresent;
        _stashRange = stashRange;
        _toStore = toStore;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(TO_SCAN);
        if (_storedItems == null) {
            _storedItems = new ContainerStoredTracker(slot -> {
                Optional<BlockPos> currentContainer = mod.getItemStorage().getLastBlockPosInteraction();
                return currentContainer.isPresent() && _stashRange.contains(currentContainer.get());
            });
        }
        _storedItems.startTracking();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Get more if we don't have & "get if not present" is true.
        if (_getIfNotPresent) {
            for (ItemTarget target : _toStore) {
                int inventoryNeed = target.getTargetCount() - _storedItems.getStoredCount(target.getMatches());
                if (inventoryNeed > mod.getItemStorage().getItemCount(target)) {
                    return TaskCatalogue.getItemTask(new ItemTarget(target, inventoryNeed));
                }
            }
        }

        Predicate<BlockPos> validContainer = blockPos -> {
            if (!_stashRange.contains(blockPos))
                return false;
            Optional<ContainerCache> container = mod.getItemStorage().getContainerAtPosition(blockPos);
            // We haven't opened this container OR it's opened and NOT full
            return container.isEmpty() || !container.get().isFull();
        };

        // Store in valid container
        if (mod.getBlockTracker().anyFound(validContainer, TO_SCAN)) {
            setDebugState("Storing in closest stash container");
            return new DoToClosestBlockTask(
                    (BlockPos bpos) -> new StoreInContainerTask(bpos, false, _storedItems.getUnstoredItemTargetsYouCanStore(mod, _toStore)),
                    validContainer,
                    TO_SCAN
            );
        }

        setDebugState("Traveling to stash (no non-full containers in stash range found)");
        BlockPos centerStash = _stashRange.getCenter();
        return new GetToXZTask(centerStash.getX(), centerStash.getZ());
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(TO_SCAN);
        _storedItems.stopTracking();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _storedItems != null && _storedItems.getUnstoredItemTargetsYouCanStore(mod, _toStore).length == 0;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof StoreInStashTask task) {
            return task._stashRange.equals(_stashRange) && task._getIfNotPresent == _getIfNotPresent && Arrays.equals(task._toStore, _toStore);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in stash" + _stashRange + ": " + Arrays.toString(_toStore);
    }
}
