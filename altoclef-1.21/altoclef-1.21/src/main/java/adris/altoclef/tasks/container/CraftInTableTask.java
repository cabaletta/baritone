package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftGenericManuallyTask;
import adris.altoclef.tasks.CraftGenericWithRecipeBooksTask;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.JankCraftingRecipeMapping;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Crafts an item in a crafting table, obtaining and placing the table down if none was found.
 */
public class CraftInTableTask extends ResourceTask {

    private final RecipeTarget[] _targets;

    private final DoCraftInTableTask _craftTask;

    public CraftInTableTask(RecipeTarget[] targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        _craftTask = new DoCraftInTableTask(_targets);
    }

    public CraftInTableTask(RecipeTarget target, boolean collect, boolean ignoreUncataloguedSlots) {
        super(new ItemTarget(target.getOutputItem(), target.getTargetCount()));
        _targets = new RecipeTarget[]{target};
        _craftTask = new DoCraftInTableTask(_targets, collect, ignoreUncataloguedSlots);
    }

    public CraftInTableTask(RecipeTarget target) {
        this(target, true, true);
    }

    /**
     * Extracts item targets from recipe targets.
     *
     * @param recipeTargets The array of recipe targets.
     * @return The array of item targets.
     */
    private static ItemTarget[] extractItemTargets(RecipeTarget[] recipeTargets) {
        // Use Java streams to map each recipe target to a new item target
        return Arrays.stream(recipeTargets)
                .map(t -> new ItemTarget(t.getOutputItem(), t.getTargetCount()))
                .toArray(ItemTarget[]::new);
    }

    /**
     * Determines whether the player should avoid picking up items.
     *
     * @param mod The AltoClef mod instance.
     * @return true if the player should avoid picking up items, false otherwise.
     */
    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    /**
     * Called when the resource starts.
     *
     * @param mod The AltoClef mod instance.
     */
    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    /**
     * This method is called on each tick of the resource manager.
     * It returns the task that should be executed on each tick.
     *
     * @param mod The instance of the AltoClef mod.
     * @return The task to be executed on each tick.
     */
    @Override
    protected Task onResourceTick(AltoClef mod) {
        return _craftTask;
    }

    /**
     * Override method called when the resource stops.
     *
     * @param mod           The AltoClef mod.
     * @param interruptTask The interrupt task.
     */
    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Get the item stack in the cursor slot.
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        // If the cursor stack is not empty, handle it.
        if (!cursorStack.isEmpty()) {
            // Find a slot in the player inventory that can fit the cursor stack.
            Optional<Slot> moveToSlot = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);

            // If a slot is found, pick up the item from the cursor slot and move it to the found slot.
            moveToSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

            // If the item can be thrown away, pick up the item from the cursor slot and throw it away.
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }

            // Find the garbage slot and move the item from the cursor slot to the garbage slot.
            Optional<Slot> garbageSlot = StorageHelper.getGarbageSlot(mod);
            garbageSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
        } else {
            // If the cursor stack is empty, close the screen.
            StorageHelper.closeScreen();
        }

        // Pick up an undefined slot.
        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
    }

    /**
     * Checks if the given ResourceTask is equal to this CraftInTableTask.
     *
     * @param other The ResourceTask to compare with.
     * @return true if the ResourceTask is a CraftInTableTask and its craftTask is equal to this task's craftTask, false otherwise.
     */
    @Override
    protected boolean isEqualResource(ResourceTask other) {
        // Check if the other task is an instance of CraftInTableTask
        if (other instanceof CraftInTableTask task) {
            // Compare the craftTask of the two tasks
            return _craftTask.isEqual(task._craftTask);
        }
        // The other task is not a CraftInTableTask, return false
        return false;
    }

    /**
     * Returns the debug string name of the craft task.
     * If the craft task is not null, it calls the toDebugString() method of the craft task and returns the result.
     * Otherwise, it returns null.
     *
     * @return the debug string name of the craft task, or null if the craft task is null.
     */
    @Override
    protected String toDebugStringName() {
        return (_craftTask != null) ? _craftTask.toDebugString() : null;
    }

    /**
     * Returns a copy of the recipe targets.
     *
     * @return The recipe targets.
     */
    public RecipeTarget[] getRecipeTargets() {
        return Arrays.copyOf(_targets, _targets.length);
    }
}


class DoCraftInTableTask extends DoStuffInContainerTask {

    private final float CRAFT_RESET_TIMER_BONUS_SECONDS = 10;

    private final RecipeTarget[] _targets;

    private final boolean _collect;

    private final CollectRecipeCataloguedResourcesTask _collectTask;
    private final TimerGame _craftResetTimer = new TimerGame(CRAFT_RESET_TIMER_BONUS_SECONDS);
    private int _craftCount;

    public DoCraftInTableTask(RecipeTarget[] targets, boolean collect, boolean ignoreUncataloguedSlots) {
        super(Blocks.CRAFTING_TABLE, new ItemTarget("crafting_table"));
        _collectTask = new CollectRecipeCataloguedResourcesTask(ignoreUncataloguedSlots, targets);
        _targets = targets;
        _collect = collect;
    }

    public DoCraftInTableTask(RecipeTarget[] targets) {
        this(targets, true, false);
    }

    /**
     * Override method called when the mod starts.
     * Refactored to handle item management and screen closing.
     * Resets the collect task.
     *
     * @param mod The AltoClef mod instance.
     */
    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);

        // Save the current behaviour and craft count
        mod.getBehaviour().push();
        _craftCount = 0;

        // Check if there is an item in the cursor slot
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        if (!cursorStack.isEmpty()) {
            // Move the item to a slot in the player's inventory that can fit it
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

            // Check if the item can be thrown away
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                // Throw away the item
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }

            // Move the item to the garbage slot
            StorageHelper.getGarbageSlot(mod).ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

            // Clear the cursor slot
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            // Close the screen if there is no item in the cursor slot
            StorageHelper.closeScreen();
        }

        // Reset the collect task
        _collectTask.reset();
    }

    /**
     * This method is called when the task is interrupted or stopped.
     * It performs the necessary actions to handle the interruption or stopping of the task.
     *
     * @param mod           The instance of the AltoClef mod.
     * @param interruptTask The task that caused the interruption, or null if the task was stopped manually.
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Get the item stack in the cursor slot
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        // If the cursor stack is empty, close the screen
        if (cursorStack.isEmpty()) {
            StorageHelper.closeScreen();
        } else {
            // Get a slot that can fit the cursor stack
            Optional<Slot> moveToSlot = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);

            // If a slot is found, move the cursor stack to that slot
            moveToSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

            // If the cursor stack can be thrown away, throw it away
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }

            // Get the garbage slot
            Optional<Slot> garbageSlot = StorageHelper.getGarbageSlot(mod);

            // If a garbage slot is found, move the cursor stack to that slot
            garbageSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

            // Move the cursor stack to an undefined slot
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        }

        // Call the onStop method of the super class
        super.onStop(mod, interruptTask);

        // Pop the behaviour from the stack
        mod.getBehaviour().pop();
    }

    /**
     * This method is called periodically to perform crafting-related tasks.
     *
     * @param mod The AltoClef mod instance.
     * @return The next task to execute.
     */
    @Override
    protected Task onTick(AltoClef mod) {
        // Add protected items to the behaviour
        mod.getBehaviour().addProtectedItems(getMaterialsArray());

        // Avoid breaking crafting tables
        if (mod.getBlockTracker().isTracking(Blocks.CRAFTING_TABLE)) {
            Optional<BlockPos> craftingTable = mod.getBlockTracker().getNearestTracking(Blocks.CRAFTING_TABLE);
            craftingTable.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        }
        // Check if the player inventory is open and the cursor slot is empty
        if (StorageHelper.isPlayerInventoryOpen() && StorageHelper.getItemStackInCursorSlot().isEmpty()) {
            // Get the item in the craft output slot
            Item outputItem = StorageHelper.getItemStackInSlot(PlayerSlot.CRAFT_OUTPUT_SLOT).getItem();
            // Check if the output item matches any of the targets and the target count is not reached
            for (RecipeTarget target : _targets) {
                if (target.getOutputItem() == outputItem && mod.getItemStorage().getItemCount(target.getOutputItem()) < target.getTargetCount()) {
                    return new ReceiveCraftingOutputSlotTask(PlayerSlot.CRAFT_OUTPUT_SLOT, target.getTargetCount());
                }
            }
        }

        // Check if we need to collect items and the collect task is not finished
        if (_collect && !_collectTask.isFinished(mod) && !StorageHelper.hasRecipeMaterialsOrTarget(mod, _targets)) {
            return _collectTask;
        }

        // Reset the craft reset timer if the container is not open
        if (!isContainerOpen(mod)) {
            _craftResetTimer.reset();
        }

        // Check if there is any inaccessible item in the recipes and move it to the inventory
        if (!thisOrChildSatisfies(task -> task instanceof CraftInInventoryTask)) {
            for (RecipeTarget target : _targets) {
                for (int slot = 0; slot < target.getRecipe().getSlotCount(); ++slot) {
                    ItemTarget toCheck = target.getRecipe().getSlot(slot);
                    if (StorageHelper.isItemInaccessibleToContainer(mod, toCheck)) {
                        return new MoveInaccessibleItemToInventoryTask(toCheck);
                    }
                }
            }
        }

        // Call the parent method
        return super.onTick(mod);
    }

    /**
     * Checks if the given DoStuffInContainerTask is equal to this task.
     *
     * @param other The other DoStuffInContainerTask to compare.
     * @return True if the tasks are equal, False otherwise.
     */
    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
        // Check if the other task is an instance of DoCraftInTableTask
        if (other instanceof DoCraftInTableTask task) {
            // Compare the targets arrays of the two tasks
            return Arrays.equals(task._targets, _targets);
        }
        // The other task is not an instance of DoCraftInTableTask, so they are not equal
        return false;
    }

    /**
     * Checks if the container is open.
     *
     * @param mod The AltoClef mod instance.
     * @return True if the container is open, false otherwise.
     */
    @Override
    protected boolean isContainerOpen(AltoClef mod) {
        return mod.getPlayer().currentScreenHandler instanceof CraftingScreenHandler;
    }

    /**
     * Executes the container subtask.
     *
     * @param mod The AltoClef mod instance.
     * @return The subtask to be executed.
     */
    @Override
    protected Task containerSubTask(AltoClef mod) {
        // Calculate the interval based on the container item move delay and a bonus duration
        float interval = mod.getModSettings().getContainerItemMoveDelay() * 10 + CRAFT_RESET_TIMER_BONUS_SECONDS;
        _craftResetTimer.setInterval(interval);

        // If the craft reset timer has elapsed, return a TimeoutWanderTask
        if (_craftResetTimer.elapsed()) {
            return new TimeoutWanderTask(5);
        }

        // Iterate through each target recipe
        for (RecipeTarget target : _targets) {
            // Check if the output item count meets the target count
            if (mod.getItemStorage().getItemCount(target.getOutputItem()) >= target.getTargetCount()) {
                continue;
            }

            // Get the recipe to send based on the target recipe and output item
            Optional<RecipeEntry<?>> recipeToSend = JankCraftingRecipeMapping.getMinecraftMappedRecipe(target.getRecipe(), target.getOutputItem());

            // Get the client player entity
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            // If crafting book is enabled, the recipe to send exists, and the player has the recipe in their recipe book, return a CraftGenericWithRecipeBooksTask
            if (mod.getModSettings().shouldUseCraftingBookToCraft() && recipeToSend.isPresent()) {
                assert player != null;
                if (player.getRecipeBook().contains(recipeToSend.get())) {
                    return new CraftGenericWithRecipeBooksTask(target);
                }
            }

            // Return a CraftGenericManuallyTask by default
            return new CraftGenericManuallyTask(target);
        }

        return null;
    }

    /**
     * Checks if the specified mod is finished.
     *
     * @param mod The mod to check.
     * @return True if the mod is finished, false otherwise.
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if the craft count is greater than or equal to the number of targets
        return _craftCount >= _targets.length;
    }

    /**
     * Returns the cost to make a new AltoClef mod.
     *
     * @param mod The AltoClef mod instance.
     * @return The cost to make a new AltoClef mod.
     */
    @Override
    protected double getCostToMakeNew(AltoClef mod) {
        // Get the nearest crafting table.
        Optional<BlockPos> closestCraftingTable = mod.getBlockTracker().getNearestTracking(Blocks.CRAFTING_TABLE);

        // If a crafting table is within 40 blocks of the player, return positive infinity.
        if (closestCraftingTable.isPresent() && closestCraftingTable.get().isWithinDistance(mod.getPlayer().getPos(), 40)) {
            return Double.POSITIVE_INFINITY;
        }

        // If the mod has logs or enough planks, return a cost of 10.
        if (mod.getItemStorage().hasItem(ItemHelper.LOG) || mod.getItemStorage().getItemCount(ItemHelper.PLANKS) >= 4) {
            return 10;
        }

        // Otherwise, return a cost of 100.
        return 100;
    }

    /**
     * Returns an array of materials.
     *
     * @return the array of materials
     */
    private Item[] getMaterialsArray() {
        List<Item> result = new ArrayList<>();

        // Iterate over each target
        for (RecipeTarget target : _targets) {
            // Iterate over each slot in the recipe
            for (int i = 0; i < target.getRecipe().getSlotCount(); ++i) {
                ItemTarget materialTarget = target.getRecipe().getSlot(i);
                // Check if the material target is not null and has matches
                if (materialTarget != null && materialTarget.getMatches() != null) {
                    // Add all the matches to the result list
                    Collections.addAll(result, materialTarget.getMatches());
                }
            }
        }

        // Convert the result list to an array and return it
        return result.toArray(new Item[0]);
    }

}
