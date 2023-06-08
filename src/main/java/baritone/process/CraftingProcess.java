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
import baritone.api.pathing.goals.*;
import baritone.api.process.ICraftingProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {
    private int amount;
    private IRecipe recipe;
    private Goal goal;
    private BlockPos placeAt;

    public CraftingProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return recipe != null && amount >= 1;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (calcFailed) {
            logDirect("path calculation failed");
            onLostControl();
        }
        if (goal != null) {
            //we are pathing to a table and therefor have to wait.
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        } else if (placeAt != null) {

            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        } else {
            //we no longer pathing so it's time to craft
            try {
                moveItemsToCraftingGrid();
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
            getACraftingTable();
        } else {
            logDirect("Insufficient resources.");
        }
    }

    @Override
    public void craftRecipe(IRecipe recipe, int amount) {
        if (canCraft(recipe, amount)) {
            this.recipe = recipe;
            this.amount = amount;
            getACraftingTable();
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
        //we maybe still be able to craft but need to mix ingredients.
        //example we have 1 oak plank and 1 spruce plank and want to make sticks. this statement could be wrong.
    }

    @Override
    //should this be in a helper class?
    public boolean canCraft(IRecipe recipe, int amount) {
        RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        for (ItemStack stack : ctx.player().inventory.mainInventory) {
            recipeItemHelper.accountStack(stack);
        }
        //could be inlined but i think that would be not very readable
        int outputCount = recipe.getRecipeOutput().getCount();
        int times = amount % outputCount == 0 ? amount / outputCount : (amount / outputCount) + 1;
        return recipeItemHelper.canCraft(recipe, null, times);
    }

    @Override
    public boolean canCraftInInventory(IRecipe recipe) {
        return recipe.canFit(2,2);
    }

    private int getInputCount() {
        int stackSize = Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftMatrix.getStackInSlot(i);
            if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                stackSize = Math.min(itemStack.getCount(), stackSize);
            }
        }
        return stackSize == Integer.MAX_VALUE ? 0 : stackSize;
    }

    private void takeResultFromOutput() {
        int inputCount = getInputCount();
        if (inputCount > 0) {
            int windowId = ctx.player().openContainer.windowId;
            int slotID = 0; //slot id. for crafting table output it is 0
            int randomIntWeDontNeedButHaveToProvide = 0; //idk isnt used
            mc.playerController.windowClick(windowId, slotID, randomIntWeDontNeedButHaveToProvide, ClickType.QUICK_MOVE, ctx.player());
            amount = amount - (recipe.getRecipeOutput().getCount() * inputCount);
        }

        if (amount <= 0) {
            logDirect("Done");
            //we finished crafting
            ctx.player().closeScreen();
            onLostControl();
        }
    }

    private void moveItemsToCraftingGrid() {
        int windowId = ctx.player().openContainer.windowId;
        //try to put the recipe the required amount of times in to the crafting grid.
        for (int i = 0; i * recipe.getRecipeOutput().getCount() < amount; i++) {
            mc.playerController.func_194338_a(windowId, recipe, GuiScreen.isShiftKeyDown(), ctx.player());
        }
    }

    private void getACraftingTable() {
        List<BlockPos> knownLocations = MineProcess.searchWorld(new CalculationContext(baritone, false), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 1, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        if (knownLocations.isEmpty()) {
            logDirect("no crafting table found");
            if (hasCraftingTable()) {
                logDirect("but player has one in inventory");
                placeAt = ctx.playerFeet().north();
            } else {
                logDirect("No CraftingTable.");
                onLostControl();
            }
        } else {
            logDirect("crafting table found");
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
}
