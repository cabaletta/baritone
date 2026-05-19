package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.resources.CollectFuelTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.FurnaceSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


// Ref
// https://minecraft.gamepedia.com/Smelting

/**
 * Smelt in a furnace, placing a furnace and collecting fuel as needed.
 */
public class SmeltInFurnaceTask extends ResourceTask {
    private final SmeltTarget[] _targets;

    private final DoSmeltInFurnaceTask _doTask;

    public SmeltInFurnaceTask(SmeltTarget[] targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        // TODO: Do them in order.
        _doTask = new DoSmeltInFurnaceTask(targets[0]);
    }

    public SmeltInFurnaceTask(SmeltTarget target) {
        this(new SmeltTarget[]{target});
    }

    private static ItemTarget[] extractItemTargets(SmeltTarget[] recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.length);
        for (SmeltTarget target : recipeTargets) {
            result.add(target.getItem());
        }
        return result.toArray(ItemTarget[]::new);
    }

    public void ignoreMaterials() {
        _doTask.ignoreMaterials();
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.FURNACE);
        mod.getBehaviour().push();
        if (_targets.length != 1) {
            Debug.logWarning("Tried smelting multiple targets, only one target is supported at a time!");
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> furnacePos = mod.getBlockTracker().getNearestTracking(Blocks.FURNACE);
        furnacePos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        return _doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.FURNACE);
        mod.getBehaviour().pop();
        // Close furnace screen
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return super.isFinished(mod) || _doTask.isFinished(mod);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof SmeltInFurnaceTask task) {
            return task._doTask.isEqual(_doTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return _doTask.toDebugString();
    }

    public SmeltTarget[] getTargets() {
        return _targets;
    }

    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    static class DoSmeltInFurnaceTask extends DoStuffInContainerTask {

        private final SmeltTarget _target;
        private final FurnaceCache _furnaceCache = new FurnaceCache();
        private final ItemTarget _allMaterials;
        private boolean _ignoreMaterials;

        public DoSmeltInFurnaceTask(SmeltTarget target) {
            super(Blocks.FURNACE, new ItemTarget(Items.FURNACE));
            _target = target;
            _allMaterials = new ItemTarget(Stream.concat(Arrays.stream(_target.getMaterial().getMatches()), Arrays.stream(_target.getOptionalMaterials())).toArray(Item[]::new), _target.getMaterial().getTargetCount());
        }

        public void ignoreMaterials() {
            _ignoreMaterials = true;
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
            if (other instanceof DoSmeltInFurnaceTask task) {
                return task._target.equals(_target) && task._ignoreMaterials == _ignoreMaterials;
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            return (mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler);
        }

        @Override
        protected Task onTick(AltoClef mod) {
            mod.getBehaviour().addProtectedItems(ItemHelper.PLANKS);
            mod.getBehaviour().addProtectedItems(Items.COAL);
            mod.getBehaviour().addProtectedItems(_allMaterials.getMatches());
            mod.getBehaviour().addProtectedItems(_target.getMaterial().getMatches());
            tryUpdateOpenFurnace(mod);
            // Include both regular + optional items
            ItemTarget materialTarget = _allMaterials;
            ItemTarget outputTarget = _target.getItem();
            // Materials needed = (mat_target (- 0*mat_in_inventory) - out_in_inventory - mat_in_furnace - out_in_furnace)
            // ^ 0 * mat_in_inventory because we always care aobut the TARGET materials, not how many LEFT there are.
            int materialsNeeded = materialTarget.getTargetCount()
                    /*- mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())*/ // See comment above
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (materialTarget.matches(_furnaceCache.materialSlot.getItem()) ? _furnaceCache.materialSlot.getCount() : 0)
                    - (outputTarget.matches(_furnaceCache.outputSlot.getItem()) ? _furnaceCache.outputSlot.getCount() : 0);
            double totalFuelInFurnace = ItemHelper.getFuelAmount(_furnaceCache.fuelSlot) + _furnaceCache.burningFuelCount + _furnaceCache.burnPercentage;
            // Fuel needed = (mat_target - out_in_inventory - out_in_furnace - totalFuelInFurnace)
            double fuelNeeded = _ignoreMaterials
                    ? Math.min(materialTarget.matches(_furnaceCache.materialSlot.getItem()) ? _furnaceCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    /* - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) */
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(_furnaceCache.outputSlot.getItem()) ? _furnaceCache.outputSlot.getCount() : 0)
                    - totalFuelInFurnace;

            // We don't have enough materials...
            if (mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) < materialsNeeded) {
                setDebugState("Getting Materials");
                return getMaterialTask(_target.getMaterial());
            }

            // We don't have enough fuel...
            if (_furnaceCache.burningFuelCount <= 0 && StorageHelper.calculateInventoryFuelCount(mod) < fuelNeeded) {
                setDebugState("Getting Fuel");
                return new CollectFuelTask(fuelNeeded + 1);
            }

            // Make sure our materials are accessible in our inventory
            if (StorageHelper.isItemInaccessibleToContainer(mod, _allMaterials)) {
                return new MoveInaccessibleItemToInventoryTask(_allMaterials);
            }

            // We have fuel and materials. Get to our container and smelt!
            return super.onTick(mod);
        }

        // Override this if our materials must be acquired in a special way.
        // virtual
        protected Task getMaterialTask(ItemTarget target) {
            return TaskCatalogue.getItemTask(target);
        }

        @Override
        protected Task containerSubTask(AltoClef mod) {
            // We have appropriate materials/fuel.
            /*
             * - If output slot has something, receive it.
             * - Calculate needed material input. If we don't have, put it in.
             * - Calculate needed fuel input. If we don't have, put it in.
             * - Wait lol
             */
            ItemStack output = StorageHelper.getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            ItemStack material = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS);
            ItemStack fuel = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_FUEL);

            // Receive from output if present
            double currentlyCachedWhileCooking = StorageHelper.getFurnaceFuel() + StorageHelper.getFurnaceCookPercent();
            double needsWhileCooking = material.getCount() - currentlyCachedWhileCooking;
            if (needsWhileCooking <= 0) {
                if (!fuel.isEmpty()) {
                    ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                    if (!ItemHelper.canStackTogether(fuel, cursor)) {
                        Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
                        if (toFit.isPresent()) {
                            mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                            return null;
                        } else {
                            // Eh screw it
                            if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                return null;
                            }
                        }
                    }
                    mod.getSlotHandler().clickSlot(FurnaceSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP);
                    return null;
                }
            }
            if (!output.isEmpty()) {
                setDebugState("Receiving Output");
                // Ensure our cursor is empty/can receive our item
                ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                if (!ItemHelper.canStackTogether(output, cursor)) {
                    Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
                    if (toFit.isPresent()) {
                        mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                        return null;
                    } else {
                        // Eh screw it
                        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            return null;
                        }
                    }
                }
                // Pick up
                mod.getSlotHandler().clickSlot(FurnaceSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP);
                return null;
                // return new MoveItemToSlotTask(new ItemTarget(output.getItem(), output.getCount()), toMoveTo.get(), mod -> FurnaceSlot.OUTPUT_SLOT);
            }

            // Fill in input if needed
            // Materials needed in slot = (mat_target - out_in_inventory - out_in_furnace)
            ItemTarget materialTarget = _allMaterials;

            int neededMaterialsInSlot = materialTarget.getTargetCount()
                    - mod.getItemStorage().getItemCountInventoryOnly(_target.getItem().getMatches())
                    - (_target.getItem().matches(output.getItem()) ? output.getCount() : 0);
            // We don't have the right material or we need more
            if (!_allMaterials.matches(material.getItem()) || neededMaterialsInSlot > material.getCount()) {
                int materialsAlreadyIn = (materialTarget.matches(material.getItem()) ? material.getCount() : 0);
                setDebugState("Moving Materials");
                return new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), FurnaceSlot.INPUT_SLOT_MATERIALS);
            }

            /*
            double currentFuel = _ignoreMaterials
                    ? (Math.min(materialTarget.matches(_furnaceCache.materialSlot.getItem()) ? _furnaceCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(_furnaceCache.outputSlot.getItem()) ? _furnaceCache.outputSlot.getCount() : 0)
                    - totalFuelInFurnace;
             */
            // Fill in fuel if needed
            if (fuel.isEmpty() || ItemHelper.isFuel(fuel.getItem())) {
                double currentlyCached = StorageHelper.getFurnaceFuel() + StorageHelper.getFurnaceCookPercent();
                double needs = material.getCount() - currentlyCached;
                if (needs > 0) {
                    // Get best fuel to fill
                    double closestDelta = Double.NEGATIVE_INFINITY;
                    ItemStack bestStack = null;
                    for (ItemStack stack : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
                        if (mod.getModSettings().isSupportedFuel(stack.getItem())) {
                            double fuelAmount = ItemHelper.getFuelAmount(stack.getItem()) * stack.getCount();
                            double delta = needs - fuelAmount;
                            if (
                                    (bestStack == null) ||
                                            // If our best is above, prioritize lower values
                                            (closestDelta > 0 && delta < closestDelta) ||
                                            // If our best is below, prioritize higher below values
                                            (closestDelta < 0 && delta < 0 && delta > closestDelta)
                            ) {
                                bestStack = stack;
                                closestDelta = delta;
                            }
                        }
                    }
                    if (bestStack != null) {
                        setDebugState("Filling fuel");
                        return new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), FurnaceSlot.INPUT_SLOT_FUEL);
                    }
                }
            }

            setDebugState("Waiting...");
            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (_furnaceCache.burnPercentage > 0 || _furnaceCache.burningFuelCount > 0 ||
                    _furnaceCache.fuelSlot != null || _furnaceCache.materialSlot != null ||
                    _furnaceCache.outputSlot != null) {
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 8) {
                double cost = 100.0 - 90.0 * (double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE}) / 8.0;
                return Math.max(cost, 10.0);
            }
            return StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD) ? 50.0 : 100.0;
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            return getTargetContainerPosition();
        }

        private void tryUpdateOpenFurnace(AltoClef mod) {
            if (isContainerOpen(mod)) {
                // Update current furnace cache
                _furnaceCache.burnPercentage = StorageHelper.getFurnaceCookPercent();
                _furnaceCache.burningFuelCount = StorageHelper.getFurnaceFuel();
                _furnaceCache.fuelSlot = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_FUEL);
                _furnaceCache.materialSlot = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS);
                _furnaceCache.outputSlot = StorageHelper.getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            }
        }
    }

    static class FurnaceCache {
        public ItemStack materialSlot = ItemStack.EMPTY;
        public ItemStack fuelSlot = ItemStack.EMPTY;
        public ItemStack outputSlot = ItemStack.EMPTY;
        public double burningFuelCount;
        public double burnPercentage;
    }
}
