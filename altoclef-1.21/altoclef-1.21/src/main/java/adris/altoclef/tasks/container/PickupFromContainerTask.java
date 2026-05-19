package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.FurnaceSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.SmokerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class PickupFromContainerTask extends AbstractDoToStorageContainerTask {

    private final BlockPos _targetContainer;
    private final ItemTarget[] _targets;

    private final EnsureFreeInventorySlotTask _freeInventoryTask = new EnsureFreeInventorySlotTask();

    public PickupFromContainerTask(BlockPos targetContainer, ItemTarget... targets) {
        _targets = targets;
        _targetContainer = targetContainer;
    }

    public static Optional<Slot> getBestSlotToTransfer(AltoClef mod, ItemTarget itemToMove, int currentItemQuantity, List<Slot> grabPotentials, Function<ItemStack, Boolean> canStackFit) {
        Slot bestPotential = null;
        int leftNeeded = itemToMove.getTargetCount() - currentItemQuantity;
        for (Slot slot : grabPotentials) {
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (itemToMove.matches(stack.getItem())) {
                if (bestPotential == null) {
                    bestPotential = slot;
                    continue;
                }
                ItemStack currBest = StorageHelper.getItemStackInSlot(bestPotential);
                int overshoot = stack.getCount() - leftNeeded;
                int currBestOverhoot = currBest.getCount() - leftNeeded;
                boolean canFit = canStackFit.apply(stack);
                boolean currBestCanFit = canStackFit.apply(currBest);
                // Prioritize "fitting" in our inventory.
                if (canFit || !currBestCanFit) {
                    if (overshoot < 0) {
                        // Prioritize highest that goes under, then lowest that goes over.
                        if (currBestOverhoot > 0 || overshoot > currBestOverhoot)
                            bestPotential = slot;
                    } else if (overshoot > 0) {
                        // Prioritize the smaller overshoot.
                        if (overshoot < currBestOverhoot)
                            bestPotential = slot;
                    } else if (currBestOverhoot != 0) {
                        // We have a "perfect" fit.
                        bestPotential = slot;
                    }
                }
            }
        }
        return Optional.ofNullable(bestPotential);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PickupFromContainerTask task) {
            return Objects.equals(_targetContainer, task._targetContainer) && Arrays.equals(_targets, task._targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Picking up from container at (" + _targetContainer.toShortString() + "): " + Arrays.toString(_targets);
    }

    @Override
    protected Optional<BlockPos> getContainerTarget() {
        return Optional.of(_targetContainer);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Free inventory while we're doing it.
        if (_freeInventoryTask.isActive() && !_freeInventoryTask.isFinished(mod) && !mod.getItemStorage().hasEmptyInventorySlot()) {
            setDebugState("Freeing inventory.");
            return _freeInventoryTask;
        }
        return super.onTick(mod);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return Arrays.stream(_targets).allMatch(target -> mod.getItemStorage().getItemCountInventoryOnly(target.getMatches()) >= target.getTargetCount());
    }

    @Override
    protected Task onContainerOpenSubtask(AltoClef mod, ContainerCache containerCache) {
        for (ItemTarget target : _targets) {
            // Go through each item
            int count = mod.getItemStorage().getItemCountInventoryOnly(target.getMatches());
            if (target.matches(StorageHelper.getItemStackInCursorSlot().getItem()))
                count -= StorageHelper.getItemStackInCursorSlot().getCount();
            if (count < target.getTargetCount()) {
                setDebugState("Collecting " + target);
                // Grab the item from the current chest that most closely matches our requirements
                List<Slot> potentials = mod.getItemStorage().getSlotsWithItemContainer(target.getMatches());

                // Pick the best slot to grab from.
                Optional<Slot> bestPotential = getBestSlotToTransfer(mod, target, count, potentials, stack -> mod.getItemStorage().getSlotThatCanFitInPlayerInventory(stack, false).isPresent());
                ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
                if (!cursorStack.isEmpty()) {
                    Optional<Slot> toPlace = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false).or(() -> StorageHelper.getGarbageSlot(mod));
                    if (toPlace.isPresent() && target.matches(cursorStack.getItem())) {
                        mod.getSlotHandler().clickSlot(toPlace.get(), 0, SlotActionType.PICKUP);
                        return null;
                    }
                    if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        return null;
                    }
                    if (toPlace.isPresent()) {
                        mod.getSlotHandler().clickSlot(toPlace.get(), 0, SlotActionType.PICKUP);
                        return null;
                    }
                }
                if (bestPotential.isPresent()) {
                    // Just pick it up, it's now ours.
                    mod.getSlotHandler().clickSlot(bestPotential.get(), 0, SlotActionType.PICKUP);
                    return null;
                }
                setDebugState("SHOULD NOT HAPPEN! No valid items detected.");
            }
        }

        // We're done.
        setDebugState("Done");
        if (mod.getPlayer().currentScreenHandler instanceof SmokerScreenHandler || mod.getPlayer().currentScreenHandler
                instanceof FurnaceScreenHandler) {
            mod.getSlotHandler().clickSlot(FurnaceSlot.INPUT_SLOT_MATERIALS, 0, SlotActionType.PICKUP);
            return null;
        }
        return null;
    }
}
