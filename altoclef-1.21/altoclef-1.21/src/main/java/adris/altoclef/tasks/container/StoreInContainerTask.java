package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Moves items from your inventory to a storage container.
 */
public class StoreInContainerTask extends AbstractDoToStorageContainerTask {

    private final BlockPos _targetContainer;
    private final boolean _getIfNotPresent;
    private final ItemTarget[] _toStore;

    private ContainerStoredTracker _storedItems;

    public StoreInContainerTask(BlockPos targetContainer, boolean getIfNotPresent, ItemTarget... toStore) {
        _targetContainer = targetContainer;
        _getIfNotPresent = getIfNotPresent;
        _toStore = toStore;
    }

    @Override
    protected Optional<BlockPos> getContainerTarget() {
        return Optional.of(_targetContainer);
    }

    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);
        if (_storedItems == null) {
            // Only consider transfers to the container we wish
            _storedItems = new ContainerStoredTracker(slot -> {
                Optional<BlockPos> openContainer = mod.getItemStorage().getLastBlockPosInteraction();
                return openContainer.isPresent() && openContainer.get().equals(_targetContainer);
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
        return super.onTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        super.onStop(mod, interruptTask);
        _storedItems.stopTracking();
    }

    @Override
    protected Task onContainerOpenSubtask(AltoClef mod, ContainerCache containerCache) {
        // Move all items that aren't in the container
        for (ItemTarget target : _storedItems.getUnstoredItemTargetsYouCanStore(mod, _toStore)) {
            setDebugState("Dumping " + target);
            // Grab the item from the current chest that most closely matches our requirements
            List<Slot> potentials = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches());

            // Pick the best slot to grab from.
            Optional<Slot> bestPotential = PickupFromContainerTask.getBestSlotToTransfer(
                    mod,
                    target,
                    mod.getItemStorage().getItemCountContainer(target.getMatches()),
                    potentials,
                    stack -> mod.getItemStorage().getSlotThatCanFitInOpenContainer(stack, false).isPresent());
            if (bestPotential.isPresent()) {
                ItemStack stackIn = StorageHelper.getItemStackInSlot(bestPotential.get());
                Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInOpenContainer(stackIn, false);
                if (toMoveTo.isEmpty()) {
                    setDebugState("CONTAINER FULL!");
                    return null;
                }
                setDebugState("Moving to slot...");
                return new MoveItemToSlotFromInventoryTask(target, toMoveTo.get());
            }
            setDebugState("SHOULD NOT HAPPEN! No valid items detected.");
        }
        setDebugState("SHOULD NOT HAPPEN! All items stored but we're still trying.");
        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // We've stored all items
        return _storedItems != null && _storedItems.getUnstoredItemTargetsYouCanStore(mod, _toStore).length == 0;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof StoreInContainerTask task) {
            return task._targetContainer.equals(_targetContainer) && task._getIfNotPresent == _getIfNotPresent && Arrays.equals(task._toStore, _toStore);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in container[" + _targetContainer.toShortString() + "] " + Arrays.toString(_toStore);
    }
}
