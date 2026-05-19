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
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.slots.SmokerSlot;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SmokerScreenHandler;
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
 * Smelt in a smoker, placing a smoker and collecting fuel as needed.
 */
public class SmeltInSmokerTask extends ResourceTask {

    private final SmeltTarget[] _targets;

    private final DoSmeltInSmokerTask _doTask;

    public SmeltInSmokerTask(SmeltTarget[] targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        // TODO: Do them in order.
        boolean ignoreMaterials = false;
        _doTask = new DoSmeltInSmokerTask(targets[0], ignoreMaterials);
    }

    public SmeltInSmokerTask(SmeltTarget target) {
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
        mod.getBlockTracker().trackBlock(Blocks.SMOKER);
        mod.getBehaviour().push();
        if (_targets.length != 1) {
            Debug.logWarning("Tried smelting multiple targets, only one target is supported at a time!");
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> smokerPos = mod.getBlockTracker().getNearestTracking(Blocks.SMOKER);
        smokerPos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        return _doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.SMOKER);
        mod.getBehaviour().pop();
        // Close smoker screen
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
        if (other instanceof SmeltInSmokerTask task) {
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
    static class DoSmeltInSmokerTask extends DoStuffInContainerTask {

        private final SmeltTarget _target;
        private final SmokerCache _smokerCache = new SmokerCache();
        private final ItemTarget _allMaterials;
        private boolean _ignoreMaterials;

        public DoSmeltInSmokerTask(SmeltTarget target, boolean ignoreMaterials) {
            super(Blocks.SMOKER, new ItemTarget(Items.SMOKER));
            _target = target;
            _ignoreMaterials = ignoreMaterials;
            _allMaterials = new ItemTarget(Stream.concat(Arrays.stream(_target.getMaterial().getMatches()), Arrays.stream(_target.getOptionalMaterials())).toArray(Item[]::new), _target.getMaterial().getTargetCount());
        }

        public void ignoreMaterials() {
            _ignoreMaterials = true;
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
            if (other instanceof DoSmeltInSmokerTask task) {
                return task._target.equals(_target) && task._ignoreMaterials == _ignoreMaterials;
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            return (mod.getPlayer().currentScreenHandler instanceof SmokerScreenHandler);
        }

        @Override
        protected Task onTick(AltoClef mod) {
            mod.getBehaviour().addProtectedItems(ItemHelper.PLANKS);
            mod.getBehaviour().addProtectedItems(Items.COAL);
            mod.getBehaviour().addProtectedItems(_allMaterials.getMatches());
            mod.getBehaviour().addProtectedItems(_target.getMaterial().getMatches());
            tryUpdateOpenSmoker(mod);
            // Include both regular + optional items
            ItemTarget materialTarget = _allMaterials;
            ItemTarget outputTarget = _target.getItem();
            // Materials needed = (mat_target (- 0*mat_in_inventory) - out_in_inventory - mat_in_furnace - out_in_furnace)
            // ^ 0 * mat_in_inventory because we always care aobut the TARGET materials, not how many LEFT there are.
            int materialsNeeded = materialTarget.getTargetCount()
                    /*- mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())*/ // See comment above
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (materialTarget.matches(_smokerCache.materialSlot.getItem()) ? _smokerCache.materialSlot.getCount() : 0)
                    - (outputTarget.matches(_smokerCache.outputSlot.getItem()) ? _smokerCache.outputSlot.getCount() : 0);
            double totalFuelInSmoker = ItemHelper.getFuelAmount(_smokerCache.fuelSlot) + _smokerCache.burningFuelCount + _smokerCache.burnPercentage;
            // Fuel needed = (mat_target - out_in_inventory - out_in_furnace - totalFuelInFurnace)
            double fuelNeeded = _ignoreMaterials
                    ? Math.min(materialTarget.matches(_smokerCache.materialSlot.getItem()) ? _smokerCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    /* - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) */
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(_smokerCache.outputSlot.getItem()) ? _smokerCache.outputSlot.getCount() : 0)
                    - totalFuelInSmoker;

            // We don't have enough materials...
            if (mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) < materialsNeeded) {
                setDebugState("Getting Materials");
                return getMaterialTask(_target.getMaterial());
            }

            // We don't have enough fuel...
            if (_smokerCache.burningFuelCount <= 0 && StorageHelper.calculateInventoryFuelCount(mod) < fuelNeeded) {
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
            ItemStack output = StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT);
            ItemStack material = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS);
            ItemStack fuel = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_FUEL);

            // Receive from output if present
            double currentlyCachedWhileCooking = StorageHelper.getSmokerFuel() + StorageHelper.getSmokerCookPercent();
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
                    mod.getSlotHandler().clickSlot(SmokerSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP);
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
                mod.getSlotHandler().clickSlot(SmokerSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP);
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
                return new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), SmokerSlot.INPUT_SLOT_MATERIALS);
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
                double currentlyCached = StorageHelper.getSmokerFuel() + StorageHelper.getSmokerCookPercent();
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
                        return new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), SmokerSlot.INPUT_SLOT_FUEL);
                    }
                }
            }

            setDebugState("Waiting...");
            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (_smokerCache.burnPercentage > 0 || _smokerCache.burningFuelCount > 0 ||
                    _smokerCache.fuelSlot != null || _smokerCache.materialSlot != null ||
                    _smokerCache.outputSlot != null) {
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 8 &&
                    mod.getItemStorage().getItemCount(ItemHelper.LOG) > 4) {
                double cost = 100.0 - 90.0 * (((double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE})
                        / 8.0) + ((double) mod.getItemStorage().getItemCount(ItemHelper.LOG) / 4.0));
                return Math.max(cost, 10.0);
            }
            return StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD) ? 50.0 : 100.0;
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            return getTargetContainerPosition();
        }

        private void tryUpdateOpenSmoker(AltoClef mod) {
            if (isContainerOpen(mod)) {
                // Update current furnace cache
                _smokerCache.burnPercentage = StorageHelper.getSmokerCookPercent();
                _smokerCache.burningFuelCount = StorageHelper.getSmokerFuel();
                _smokerCache.fuelSlot = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_FUEL);
                _smokerCache.materialSlot = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS);
                _smokerCache.outputSlot = StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT);
            }
        }
    }

    static class SmokerCache {
        public ItemStack materialSlot = ItemStack.EMPTY;
        public ItemStack fuelSlot = ItemStack.EMPTY;
        public ItemStack outputSlot = ItemStack.EMPTY;
        public double burningFuelCount;
        public double burnPercentage;
    }
}
