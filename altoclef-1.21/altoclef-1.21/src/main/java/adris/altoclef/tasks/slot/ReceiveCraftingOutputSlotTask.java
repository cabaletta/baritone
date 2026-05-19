package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class ReceiveCraftingOutputSlotTask extends Task implements ITaskUsesCraftingGrid {

    private final int _toTake;
    private final Slot _slot;

    public ReceiveCraftingOutputSlotTask(Slot slot, int toTake) {
        _slot = slot;
        _toTake = toTake;
    }

    public ReceiveCraftingOutputSlotTask(Slot slot, boolean all) {
        this(slot, all ? Integer.MAX_VALUE : 1);
    }

    // How many multiples of the current crafting recipe can we craft?
    private static int getCraftMultipleCount(AltoClef mod) {
        int minNonZero = Integer.MAX_VALUE;
        boolean found = false;
        for (Slot check : (StorageHelper.isBigCraftingOpen() ? CraftingTableSlot.INPUT_SLOTS : PlayerSlot.CRAFT_INPUT_SLOTS)) {
            ItemStack stack = StorageHelper.getItemStackInSlot(check);
            if (!stack.isEmpty()) {
                minNonZero = Math.min(stack.getCount(), minNonZero);
                found = true;
            }
        }
        if (!found)
            return 0;
        return minNonZero;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        ItemStack inOutput = StorageHelper.getItemStackInSlot(_slot);
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        boolean cursorSlotFree = cursor.isEmpty();
        if (!cursorSlotFree && !ItemHelper.canStackTogether(inOutput, cursor)) {
            return new EnsureFreeCursorSlotTask();
        }
        int craftCount = inOutput.getCount() * getCraftMultipleCount(mod);
        int weWantToAddToInventory = _toTake - mod.getItemStorage().getItemCountInventoryOnly(inOutput.getItem());
        boolean takeAll = weWantToAddToInventory >= craftCount;
        if (takeAll && mod.getItemStorage().getSlotThatCanFitInPlayerInventory(inOutput, true).isPresent()) {
            setDebugState("Quick moving output");
            mod.getSlotHandler().clickSlot(_slot, 0, SlotActionType.QUICK_MOVE);
            return null;
        }
        setDebugState("Picking up output");
        mod.getSlotHandler().clickSlot(_slot, 0, SlotActionType.PICKUP);
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ReceiveCraftingOutputSlotTask task) {
            return task._slot.equals(_slot) && task._toTake == _toTake;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Receiving output";
    }
}
