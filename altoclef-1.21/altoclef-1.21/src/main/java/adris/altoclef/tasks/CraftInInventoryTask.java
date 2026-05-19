package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.Optional;

/**
 * Crafts an item within the 2x2 inventory crafting grid.
 */
public class CraftInInventoryTask extends ResourceTask {

    private final RecipeTarget _target;
    private final boolean _collect;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _fullCheckFailed = false;

    public CraftInInventoryTask(RecipeTarget target, boolean collect, boolean ignoreUncataloguedSlots) {
        super(new ItemTarget(target.getOutputItem(), target.getTargetCount()));
        _target = target;
        _collect = collect;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    public CraftInInventoryTask(RecipeTarget target) {
        this(target, true, false);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        _fullCheckFailed = false;
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
        } // Just to be safe I guess
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Grab from output FIRST
        if (StorageHelper.isPlayerInventoryOpen()) {
            if (StorageHelper.getItemStackInCursorSlot().isEmpty()) {
                Item outputItem = StorageHelper.getItemStackInSlot(PlayerSlot.CRAFT_OUTPUT_SLOT).getItem();
                if (_itemTargets != null) {
                    for (ItemTarget target : _itemTargets) {
                        if (target.matches(outputItem)) {
                            return new ReceiveCraftingOutputSlotTask(PlayerSlot.CRAFT_OUTPUT_SLOT, target.getTargetCount());
                        }
                    }
                }
            }
        }
        if (StorageHelper.isBigCraftingOpen()) {
            StorageHelper.closeScreen();
        }
        ItemTarget toGet = _itemTargets[0];
        Item toGetItem = toGet.getMatches()[0];
        if (_collect && !StorageHelper.hasRecipeMaterialsOrTarget(mod, _target)) {
            // Collect recipe materials
            setDebugState("Collecting materials");
            return collectRecipeSubTask(mod);
        }

        // No need to free inventory, output gets picked up.

        setDebugState("Crafting in inventory... for " + toGet);
        return mod.getModSettings().shouldUseCraftingBookToCraft()
                ? new CraftGenericWithRecipeBooksTask(_target)
                : new CraftGenericManuallyTask(_target);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            List<Slot> moveTo = mod.getItemStorage().getSlotsThatCanFitInPlayerInventory(cursorStack, false);
            if (!moveTo.isEmpty()) {
                for (Slot MoveTo : moveTo) {
                    mod.getSlotHandler().clickSlot(MoveTo, 0, SlotActionType.PICKUP);
                }
            } else {
                Optional<Slot> garbageSlot = StorageHelper.getGarbageSlot(mod);
                if (garbageSlot.isPresent()) {
                    mod.getSlotHandler().clickSlot(garbageSlot.get(), 0, SlotActionType.PICKUP);
                } else {
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                }
            }
        }
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInInventoryTask task) {
            if (!task._target.equals(_target)) return false;
            return isCraftingEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return toCraftingDebugStringName() + " " + _target;
    }

    // virtual. By default assumes subtasks are CATALOGUED (in TaskCatalogue.java)
    protected Task collectRecipeSubTask(AltoClef mod) {
        return new CollectRecipeCataloguedResourcesTask(_ignoreUncataloguedSlots, _target);
    }

    protected String toCraftingDebugStringName() {
        return "Craft 2x2 Task";
    }

    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }

    public RecipeTarget getRecipeTarget() {
        return _target;
    }
}
