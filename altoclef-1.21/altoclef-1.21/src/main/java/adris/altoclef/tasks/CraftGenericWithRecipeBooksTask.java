package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.slot.EnsureFreePlayerCraftingGridTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.JankCraftingRecipeMapping;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

public class CraftGenericWithRecipeBooksTask extends Task implements ITaskUsesCraftingGrid {

    private final RecipeTarget _target;

    public CraftGenericWithRecipeBooksTask(RecipeTarget target) {
        _target = target;
    }

    /**
     * This method is called when the mod starts.
     *
     * @param mod The AltoClef mod instance.
     */
    @Override
    protected void onStart(AltoClef mod) {

    }

    /**
     * This method handles the logic for the onTick event.
     * It checks various conditions and performs actions accordingly.
     *
     * @param mod The instance of the mod.
     * @return The next task to execute.
     */
    @Override
    protected Task onTick(AltoClef mod) {
        // Check if the big crafting UI or player inventory UI is open
        boolean isBigCraftingOpen = StorageHelper.isBigCraftingOpen();
        boolean isPlayerInventoryOpen = StorageHelper.isPlayerInventoryOpen();

        // Get the item stack in the cursor slot
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        // Declare variables for the slots to move to and the garbage slot
        Optional<Slot> moveTo;
        Optional<Slot> garbage;

        // Check if neither the big crafting UI nor the player inventory UI is open
        if (!isBigCraftingOpen && !isPlayerInventoryOpen) {
            // Check if the cursor stack is not empty
            if (!cursorStack.isEmpty()) {
                // Find a slot in the player's inventory to move the item to
                moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    // Click the slot to move the item to the player's inventory
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return null;
                }
                // Check if the item can be thrown away
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    // Click an undefined slot to throw away the item
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return null;
                }
                // Find the garbage slot and click it to move the item there
                garbage = StorageHelper.getGarbageSlot(mod);
                if (garbage.isPresent()) {
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                }
                // Click an undefined slot to clear the cursor stack
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                // Close the screen
                StorageHelper.closeScreen();
            }
        }

        // Determine the output slot based on whether the big crafting UI is open
        Slot outputSlot = isBigCraftingOpen ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
        // Get the item stack in the output slot
        ItemStack output = StorageHelper.getItemStackInSlot(outputSlot);

        // Check if the output item matches the target item and the target count has not been reached
        if (_target.getOutputItem() == output.getItem() && mod.getItemStorage().getItemCount(_target.getOutputItem()) < _target.getTargetCount()) {
            // Return a task to receive the crafting output slot
            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
        }

        // Check if the cursor stack is not empty
        if (!cursorStack.isEmpty()) {
            // Find a slot in the player's inventory to move the item to
            moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (moveTo.isPresent()) {
                // Click the slot to move the item to the player's inventory
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            // Check if the item can be thrown away
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                // Click an undefined slot to throw away the item
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            // Find the garbage slot and click it to move the item there
            garbage = StorageHelper.getGarbageSlot(mod);
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            // Click an undefined slot to clear the cursor stack
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return null;
        }

        // Check if neither the big crafting UI nor the player inventory UI is open
        if (!isBigCraftingOpen) {
            PlayerSlot[] playerInputSlots = PlayerSlot.CRAFT_INPUT_SLOTS;
            for (PlayerSlot playerInputSlot : playerInputSlots) {
                ItemStack playerInput = StorageHelper.getItemStackInSlot(playerInputSlot);
                if (!playerInput.isEmpty()) {
                    // Return a task to ensure a free player crafting grid
                    return new EnsureFreePlayerCraftingGridTask();
                }
            }
        }

        Optional<RecipeEntry<?>> recipeToSend = JankCraftingRecipeMapping.getMinecraftMappedRecipe(_target.getRecipe(), _target.getOutputItem());
        if (recipeToSend.isPresent()) {
            if (mod.getSlotHandler().canDoSlotAction()) {
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                assert player != null;
                // Click the recipe to send it
                mod.getController().clickRecipe(player.currentScreenHandler.syncId, recipeToSend.get(), true);
                mod.getSlotHandler().registerSlotAction();
            }
        }

        return null;
    }

    /**
     * This method is called when the task is interrupted.
     *
     * @param mod           The AltoClef mod.
     * @param interruptTask The task that interrupted the current task.
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    /**
     * Checks if a given Task object is equal to this CraftGenericWithRecipeBooksTask object.
     *
     * @param other The Task object to compare with.
     * @return True if the given Task is equal to this CraftGenericWithRecipeBooksTask, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        // Check if the other Task is an instance of CraftGenericWithRecipeBooksTask
        if (other instanceof CraftGenericWithRecipeBooksTask) {
            CraftGenericWithRecipeBooksTask task = (CraftGenericWithRecipeBooksTask) other;

            // Check if the target of the other task is equal to the target of this task
            boolean isEqual = task._target.equals(_target);

            // Log a message if the targets are not equal
            if (!isEqual) {
                Debug.logInternal("Task targets are not equal");
            }

            // Return the result of the equality check
            return isEqual;
        }

        // Log a message if the other Task is not an instance of CraftGenericWithRecipeBooksTask
        Debug.logInternal("Task is not an instance of CraftGenericWithRecipeBooksTask");

        // Return false if the other Task is not an instance of CraftGenericWithRecipeBooksTask
        return false;
    }

    /**
     * Returns a debug string representation of the object.
     *
     * @return The debug string representation.
     */
    @Override
    protected String toDebugString() {
        // Return the debug string.
        return getClass().getSimpleName() + " (w/ RECIPE): " + _target;
    }
}
