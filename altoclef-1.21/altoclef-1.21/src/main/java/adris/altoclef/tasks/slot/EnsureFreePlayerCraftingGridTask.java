package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class EnsureFreePlayerCraftingGridTask extends Task {
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        setDebugState("Clearing the 2x2 crafting grid");
        for (Slot slot : PlayerSlot.CRAFT_INPUT_SLOTS) {
            ItemStack items = StorageHelper.getItemStackInSlot(slot);
            ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
            if (!cursor.isEmpty()) {
                return new EnsureFreeCursorSlotTask();
            }
            if (!items.isEmpty()) {
                mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP);
                return null;
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof EnsureFreePlayerCraftingGridTask;
    }

    @Override
    protected String toDebugString() {
        return "Breaking the crafting grid";
    }
}
