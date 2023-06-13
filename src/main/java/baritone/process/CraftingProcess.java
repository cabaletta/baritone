/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.ICraftingProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {
    private int amount;
    private IRecipe recipe;
    private Goal goal;
    private BlockPos placeAt;
    private boolean clearToPush = true;

    public CraftingProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return recipe != null && amount >= 1;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (calcFailed) { //is this correct?
            logDirect("path calculation failed");
            onLostControl();
        }
        if (goal != null) {
            //we are pathing to a table and therefor have to wait.
            if (goal.isInGoal(ctx.playerFeet())) {
                rightClick();
                if (ctx.player().openContainer instanceof ContainerWorkbench) {
                    goal = null;
                }
            }
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        } else if (placeAt != null) {
            placeCraftingtableNearby();
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        } else {
            //we no longer pathing so it's time to craft
            try {
                if (clearToPush) {
                    moveItemsToCraftingGrid();
                }
                takeResultFromOutput();
            } catch (Exception e) {
                logDirect("Error! Did you close the crafting window while crafting process was still running?");
                onLostControl();
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
    }

    @Override
    public synchronized void onLostControl() {
        amount = 0;
        recipe = null;
        goal = null;
        placeAt = null;
        baritone.getInputOverrideHandler().clearAllKeys();
    }

    @Override
    public String displayName0() {
        return "Crafting " + amount + "x " + recipe.getRecipeOutput().getDisplayName();
    }

    @Override
    public void craftItem(Item item, int amount) {
        if (canCraft(item, amount)) {
            this.amount = amount;
            logDirect("Crafting now " + amount + "x [" + recipe.getRecipeOutput().getDisplayName() + "]");
            if (!canCraftInInventory(recipe)) {
                getACraftingTable();
            }
        } else {
            logDirect("Insufficient resources.");
        }
    }

    @Override
    public void craftRecipe(IRecipe recipe, int amount) {
        if (canCraft(recipe, amount)) {
            this.recipe = recipe;
            this.amount = amount;
            logDirect("Crafting now " + amount + "x [" + recipe.getRecipeOutput().getDisplayName() + "]");
            if (!canCraftInInventory(recipe)) {
                getACraftingTable();
            }
        } else {
            logDirect("Insufficient resources.");
        }
    }

    @Override
    //should this be in a helper class?
    public boolean hasCraftingRecipe(Item item) {
        ArrayList<IRecipe> recipes = getCraftingRecipes(item);
        return !recipes.isEmpty();
    }

    @Override
    //should this be in a helper class?
    public ArrayList<IRecipe> getCraftingRecipes(Item item) {
        ArrayList<IRecipe> recipes = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.getRecipeOutput().getItem().equals(item)) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    @Override
    //should this be in a helper class?
    public boolean canCraft(Item item, int amount) {
        List<IRecipe> recipeList = getCraftingRecipes(item);
        for (IRecipe recipe : recipeList) {
            if (canCraft(recipe, amount)) {
                this.recipe = recipe;
                return true;
            }
        }
        return false;
    }

    @Override
    //should this be in a helper class?
    public boolean canCraft(IRecipe recipe, int amount) {
        RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        for (ItemStack stack : ctx.player().inventory.mainInventory) {
            recipeItemHelper.accountStack(stack);
        }
        int outputCount = recipe.getRecipeOutput().getCount();
        int times = amount % outputCount == 0 ? amount / outputCount : (amount / outputCount) + 1;
        return recipeItemHelper.canCraft(recipe, null, times);
    }

    @Override
    public boolean canCraftInInventory(IRecipe recipe) {
        return recipe.canFit(2,2) && !ctx.player().isCreative();
    }

    private void moveItemsToCraftingGrid() {
        clearToPush = false;
        int windowId = ctx.player().openContainer.windowId;
        //try to put the recipe the required amount of times in to the crafting grid.
        for (int i = 0; i * recipe.getRecipeOutput().getCount() < amount; i++) {
            mc.playerController.func_194338_a(windowId, recipe, GuiScreen.isShiftKeyDown(), ctx.player());
        }
    }

    private void takeResultFromOutput() {
        int inputCount = getInputCount();
        if (inputCount > 0) {
            int windowId = ctx.player().openContainer.windowId;
            int slotID = 0; //slot id. for crafting table output it is 0
            int randomIntWeDontNeedButHaveToProvide = 0; //idk isnt used
            mc.playerController.windowClick(windowId, slotID, randomIntWeDontNeedButHaveToProvide, ClickType.QUICK_MOVE, ctx.player());
            amount = amount - (recipe.getRecipeOutput().getCount() * inputCount);
            clearToPush = true;
        }

        if (amount <= 0) {
            logDirect("Done");
            ctx.player().closeScreen();
            onLostControl();
        }
    }

    private int getInputCount() {
        int stackSize = Integer.MAX_VALUE;
        if (ctx.player().openContainer instanceof ContainerPlayer) {
            for (int i = 0; i < 4; i++) {
                ItemStack itemStack = ((ContainerPlayer) ctx.player().openContainer).craftMatrix.getStackInSlot(i);
                if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                    stackSize = Math.min(itemStack.getCount(), stackSize);
                }
            }
        } else if (ctx.player().openContainer instanceof ContainerWorkbench) {
            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = ((ContainerWorkbench) ctx.player().openContainer).craftMatrix.getStackInSlot(i);
                if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                    stackSize = Math.min(itemStack.getCount(), stackSize);
                }
            }
        }
        return stackSize == Integer.MAX_VALUE ? 0 : stackSize;
    }

    private void getACraftingTable() {
        List<BlockPos> knownLocations = MineProcess.searchWorld(new CalculationContext(baritone, false), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 64, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        if (knownLocations.isEmpty()) {
            if (hasCraftingTable()) {
                placeAt = ctx.playerFeet().north();
            } else {
                logDirect("Recipe requires a crafting table.");
                onLostControl();
            }
        } else {
            goal = new GoalComposite(knownLocations.stream().map(this::createGoal).toArray(Goal[]::new));
        }
    }

    private boolean hasCraftingTable() {
        for (ItemStack itemstack : ctx.player().inventory.mainInventory) {
            if (itemstack.getItem() == Item.getItemFromBlock(Blocks.CRAFTING_TABLE)) {
                return true;
            }
        }
        return false;
    }

    private Goal createGoal(BlockPos pos) {
        return new GoalGetToBlock(pos);
    }

    private boolean rightClick() { //shamelessly copied from go to block process
        BlockPos bp = null;
        if (goal instanceof GoalComposite) {
            Goal[] goals = ((GoalComposite)goal).goals();
            for (Goal goal : goals) {
                if (goal.isInGoal(ctx.playerFeet()) && goal instanceof GoalGetToBlock) {
                    bp = ((GoalGetToBlock)goal).getGoalPos();
                    break;
                }
            }
        } else {
            bp = ((GoalGetToBlock)goal).getGoalPos();
        }
        if (bp != null) {
            Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), bp, ctx.playerController().getBlockReachDistance());
            if (reachable.isPresent()) {
                baritone.getLookBehavior().updateTarget(reachable.get(), true);
                if (bp.equals(ctx.getSelectedBlock().orElse(null))) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                    System.out.println(ctx.player().openContainer);
                    if (!(ctx.player().openContainer instanceof ContainerPlayer)) {
                        baritone.getInputOverrideHandler().clearAllKeys();
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        return false;
    }

    private void placeCraftingtableNearby() { //this code is so buggy im amazed that there are special cases where it works
        selectCraftingTable();
        //todo search and look at a position where the table can be placed
        baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
        baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);

        placeAt = null;
        getACraftingTable();
    }

    private void selectCraftingTable() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = ctx.player().inventory.mainInventory.get(i);
            if (stack.getItem() == Item.getItemFromBlock(Blocks.CRAFTING_TABLE)) {
                if (i < 9) {
                    ctx.player().inventory.currentItem = i;
                    return;
                } else {
                    baritone.getInventoryBehavior().attemptToPutOnHotbar(i, null);
                    i=0;
                }
            }
        }
    }
}
