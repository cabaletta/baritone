package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MoveItemToSlotTask extends Task {

    private final ItemTarget _toMove;
    private final Slot _destination;
    private final Function<AltoClef, List<Slot>> _getMovableSlots;

    public MoveItemToSlotTask(ItemTarget toMove, Slot destination, Function<AltoClef, List<Slot>> getMovableSlots) {
        _toMove = toMove;
        _destination = destination;
        _getMovableSlots = getMovableSlots;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getSlotHandler().canDoSlotAction()) {
            // Rough plan
            // - If empty slot or wrong item
            //      Find best matching item (smallest count over target, or largest count if none over)
            //      Click on it (one turn)
            // - If held slot has < items than target count
            //      Left click on destination slot (one turn)
            // - If held slot has > items than target count
            //      Right click on destination slot (one turn)
            ItemStack currentHeld = StorageHelper.getItemStackInCursorSlot();
            ItemStack atTarget = StorageHelper.getItemStackInSlot(_destination);

            // Items that CAN be moved to that slot.
            Item[] validItems = _toMove.getMatches();//Arrays.stream(_toMove.getMatches()).filter(item -> mod.getItemStorage().getItemCount(item) >= _toMove.getTargetCount()).toArray(Item[]::new);

            // We need to deal with our cursor stack OR put an item there (to move).
            boolean wrongItemHeld = !Arrays.asList(validItems).contains(currentHeld.getItem());
            if (currentHeld.isEmpty() || wrongItemHeld) {
                Optional<Slot> toPlace;
                if (currentHeld.isEmpty()) {
                    // Just pick up
                    toPlace = getBestSlotToPickUp(mod, validItems);
                } else {
                    // Try to fit the currently held item first.
                    toPlace = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(currentHeld, true);
                    if (toPlace.isEmpty()) {
                        // If all else fails, just swap it.
                        toPlace = getBestSlotToPickUp(mod, validItems);
                    }
                }
                if (toPlace.isEmpty()) {
                    Debug.logError("Called MoveItemToSlotTask when item/not enough item is available! valid items: " + StlHelper.toString(validItems, Item::getTranslationKey));
                    return null;
                }
                mod.getSlotHandler().clickSlot(toPlace.get(), 0, SlotActionType.PICKUP);
                return null;
            }

            int currentlyPlaced = Arrays.asList(validItems).contains(atTarget.getItem()) ? atTarget.getCount() : 0;
            if (currentHeld.getCount() + currentlyPlaced <= _toMove.getTargetCount()) {
                // Just place all of 'em
                mod.getSlotHandler().clickSlot(_destination, 0, SlotActionType.PICKUP);
            } else {
                // Place one at a time.
                mod.getSlotHandler().clickSlot(_destination, 1, SlotActionType.PICKUP);
            }
            return null;
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    public boolean isFinished(AltoClef mod) {
        ItemStack atDestination = StorageHelper.getItemStackInSlot(_destination);
        return (_toMove.matches(atDestination.getItem()) && atDestination.getCount() >= _toMove.getTargetCount());
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof MoveItemToSlotTask task) {
            return task._toMove.equals(_toMove) && task._destination.equals(_destination);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Moving " + _toMove + " to " + _destination;
    }

    private Optional<Slot> getBestSlotToPickUp(AltoClef mod, Item[] validItems) {
        Slot bestMatch = null;
        if (!_getMovableSlots.apply(mod).isEmpty()) {
            for (Slot slot : _getMovableSlots.apply(mod)) {
                if (Slot.isCursor(slot))
                    continue;
                if (!_toMove.matches(StorageHelper.getItemStackInSlot(slot).getItem()))
                    continue;
                if (bestMatch == null) {
                    bestMatch = slot;
                    continue;
                }
                int countBest = StorageHelper.getItemStackInSlot(bestMatch).getCount();
                int countCheck = StorageHelper.getItemStackInSlot(slot).getCount();
                if ((countBest < _toMove.getTargetCount() && countCheck > countBest)
                        || (countBest >= _toMove.getTargetCount() && countCheck >= _toMove.getTargetCount() && countCheck > countBest)) {
                    // If we don't have enough, go for largest
                    // If we have too much, go for smallest over the limit.
                    bestMatch = slot;
                }
            }
        }
        return Optional.ofNullable(bestMatch);
    }
}
