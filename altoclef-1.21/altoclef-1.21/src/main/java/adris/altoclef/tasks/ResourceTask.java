package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.PickupFromContainerTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasks.slot.EnsureFreePlayerCraftingGridTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasksystem.ITaskCanForce;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The parent for all "collect an item" tasks.
 * <p>
 * If the target item is on the ground or in a chest, will grab from those sources first.
 */
public abstract class ResourceTask extends Task implements ITaskCanForce {

    protected final ItemTarget[] _itemTargets;

    private final PickupDroppedItemTask _pickupTask;
    private final EnsureFreePlayerCraftingGridTask _ensureFreeCraftingGridTask = new EnsureFreePlayerCraftingGridTask();
    private ContainerCache _currentContainer;
    // Extra resource parameters
    private Block[] _mineIfPresent = null;
    private boolean _forceDimension = false;
    private Dimension _targetDimension;
    private BlockPos _mineLastClosest = null;

    public ResourceTask(ItemTarget[] itemTargets) {
        _itemTargets = itemTargets;
        _pickupTask = new PickupDroppedItemTask(_itemTargets, true);
    }

    public ResourceTask(ItemTarget target) {
        this(new ItemTarget[]{target});
    }

    public ResourceTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return StorageHelper.itemTargetsMetInventoryNoCursor(mod, _itemTargets);
    }

    @Override
    public boolean shouldForce(AltoClef mod, Task interruptingCandidate) {
        // We have an important item target in our cursor.
        return StorageHelper.itemTargetsMetInventory(mod, _itemTargets) && !isFinished(mod)
                // This _should_ be redundant, but it'll be a guard just to make 100% sure.
                && Arrays.stream(_itemTargets).anyMatch(target -> target.matches(StorageHelper.getItemStackInCursorSlot().getItem()));
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        //removeThrowawayItems(_itemTargets);
        if (_mineIfPresent != null) {
            mod.getBlockTracker().trackBlock(_mineIfPresent);
        }
        onResourceStart(mod);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        mod.getBehaviour().addProtectedItems(ItemTarget.getMatches(_itemTargets));
        // If we have an item in an INACCESSIBLE inventory slot
        if (!(thisOrChildSatisfies(task -> task instanceof ITaskUsesCraftingGrid)) || _ensureFreeCraftingGridTask.isActive()) {
            for (ItemTarget target : _itemTargets) {
                if (StorageHelper.isItemInaccessibleToContainer(mod, target)) {
                    setDebugState("Moving from SPECIAL inventory slot");
                    return new MoveInaccessibleItemToInventoryTask(target);
                }
            }
        }
        // We have enough items COUNTING the cursor slot, we just need to move an item from our cursor.
        if (StorageHelper.itemTargetsMetInventory(mod, _itemTargets) && Arrays.stream(_itemTargets).anyMatch(target -> target.matches(StorageHelper.getItemStackInCursorSlot().getItem()))) {
            setDebugState("Moving from cursor");
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(StorageHelper.getItemStackInCursorSlot(), false);
            if (moveTo.isPresent()) {
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(mod, StorageHelper.getItemStackInCursorSlot())) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return null;
        }

        if (!shouldAvoidPickingUp(mod)) {
            // Check if items are on the floor. If so, pick em up.
            if (mod.getEntityTracker().itemDropped(_itemTargets)) {

                // If we're picking up a pickaxe (we can't go far underground or mine much)
                if (PickupDroppedItemTask.isIsGettingPickaxeFirst(mod)) {
                    if (_pickupTask.isCollectingPickaxeForThis()) {
                        setDebugState("Picking up (pickaxe first!)");
                        // Our pickup task is the one collecting the pickaxe, keep it going.
                        return _pickupTask;
                    }
                    // Only get items that are CLOSE to us.
                    Optional<ItemEntity> closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), _itemTargets);
                    if (closest.isPresent() && !closest.get().isInRange(mod.getPlayer(), 10)) {
                        return onResourceTick(mod);
                    }
                }

                double range = mod.getModSettings().getResourcePickupRange();
                Optional<ItemEntity> closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), _itemTargets);
                if (range < 0 || (closest.isPresent() && closest.get().isInRange(mod.getPlayer(), range)) || (_pickupTask.isActive() && !_pickupTask.isFinished(mod))) {
                    setDebugState("Picking up");
                    return _pickupTask;
                }
            }
        }

        // Check for chests and grab resources from them.
        if (_currentContainer == null) {
            List<ContainerCache> containersWithItem = mod.getItemStorage().getContainersWithItem(Arrays.stream(_itemTargets).reduce(new Item[0], (items, target) -> ArrayUtils.addAll(items, target.getMatches()), ArrayUtils::addAll));
            if (!containersWithItem.isEmpty()) {
                ContainerCache closest = containersWithItem.stream().min(StlHelper.compareValues(container -> container.getBlockPos().getSquaredDistance(mod.getPlayer().getPos()))).get();
                if (closest.getBlockPos().isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceChestLocateRange())) {
                    _currentContainer = closest;
                }
            }
        }
        if (_currentContainer != null) {
            Optional<ContainerCache> container = mod.getItemStorage().getContainerAtPosition(_currentContainer.getBlockPos());
            if (container.isPresent()) {
                if (Arrays.stream(_itemTargets).noneMatch(target -> container.get().hasItem(target.getMatches()))) {
                    _currentContainer = null;
                } else {
                    // We have a current chest, grab from it.
                    setDebugState("Picking up from container");
                    return new PickupFromContainerTask(_currentContainer.getBlockPos(), _itemTargets);
                }
            } else {
                _currentContainer = null;
            }
        }

        // We may just mine if a block is found.
        if (_mineIfPresent != null) {
            ArrayList<Block> satisfiedReqs = new ArrayList<>(Arrays.asList(_mineIfPresent));
            satisfiedReqs.removeIf(block -> !StorageHelper.miningRequirementMet(mod, MiningRequirement.getMinimumRequirementForBlock(block)));
            if (!satisfiedReqs.isEmpty()) {
                if (mod.getBlockTracker().anyFound(satisfiedReqs.toArray(Block[]::new))) {
                    Optional<BlockPos> closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), _mineIfPresent);
                    if (closest.isPresent() && closest.get().isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange())) {
                        _mineLastClosest = closest.get();
                    }
                    if (_mineLastClosest != null) {
                        if (_mineLastClosest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange() * 1.5 + 20)) {
                            return new MineAndCollectTask(_itemTargets, _mineIfPresent, MiningRequirement.HAND);
                        }
                    }
                }
            }
        }
        // Make sure that items don't get stuck in the player crafting grid. May be an issue if a future task isn't a resource task.
        if (StorageHelper.isPlayerInventoryOpen()) {
            if (!(thisOrChildSatisfies(task -> task instanceof ITaskUsesCraftingGrid)) || _ensureFreeCraftingGridTask.isActive()) {
                for (Slot slot : PlayerSlot.CRAFT_INPUT_SLOTS) {
                    if (!StorageHelper.getItemStackInSlot(slot).isEmpty()) {
                        return _ensureFreeCraftingGridTask;
                    }
                }
            }
        }
        return onResourceTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        if (_mineIfPresent != null) {
            mod.getBlockTracker().stopTracking(_mineIfPresent);
        }
        onResourceStop(mod, interruptTask);
    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof ResourceTask t) {
            if (!isEqualResource(t)) return false;
            return Arrays.equals(t._itemTargets, _itemTargets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append(toDebugStringName()).append(": [");
        int c = 0;
        if (_itemTargets != null) {
            for (ItemTarget target : _itemTargets) {
                result.append(target != null ? target.toString() : "(null)");
                if (++c != _itemTargets.length) {
                    result.append(", ");
                }
            }
        }
        result.append("]");
        return result.toString();
    }

    protected boolean isInWrongDimension(AltoClef mod) {
        if (_forceDimension) {
            return WorldHelper.getCurrentDimension() != _targetDimension;
        }
        return false;
    }

    protected Task getToCorrectDimensionTask(AltoClef mod) {
        return new DefaultGoToDimensionTask(_targetDimension);
    }

    public ResourceTask mineIfPresent(Block[] toMine) {
        _mineIfPresent = toMine;
        return this;
    }

    public ResourceTask forceDimension(Dimension dimension) {
        _forceDimension = true;
        _targetDimension = dimension;
        return this;
    }

    protected abstract boolean shouldAvoidPickingUp(AltoClef mod);

    protected abstract void onResourceStart(AltoClef mod);

    protected abstract Task onResourceTick(AltoClef mod);

    protected abstract void onResourceStop(AltoClef mod, Task interruptTask);

    protected abstract boolean isEqualResource(ResourceTask other);

    protected abstract String toDebugStringName();

    public ItemTarget[] getItemTargets() {
        return _itemTargets;
    }
}
