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
import baritone.api.process.ICraftingProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.ArrayList;
import java.util.List;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {
    private int amount;
    private IRecipe recipe;

    public CraftingProcess(Baritone baritone) {
        super(baritone);
    }


    @Override
    public boolean isActive() {
        return recipe != null && amount >= 1;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (baritone.getGetToBlockProcess().isActive()) {
            //we are pathing to a table and therefor have to wait.
            return new PathingCommand(null, PathingCommandType.DEFER);
        } else {
            //we no longer pathing so it's time to craft
            try {
                int outputCount = getOutputCount(); //items per crafting cycle
                int inputCount = getInputCount();
                boolean inputStackLimitReached = inputCount >= getLowestMaxStackSize();

                takeResultFromOutput(outputCount, inputCount, inputStackLimitReached);
                if (mc.currentScreen instanceof GuiCrafting) {
                    moveItemsToCraftingGrid();
                }
            } catch (Exception e) {
                logDirect("Error! Did you close the crafting window while crafting process was still running?");
                onLostControl();
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
    }

    private void takeResultFromOutput(int outputCount, int inputCount, boolean inputStackLimitReached) {
        if (outputCount * inputCount >= amount || inputStackLimitReached) {
            int windowId = ctx.player().openContainer.windowId;
            int slotID = 0; //slot id. for crafting table output it is 0
            int randomIntWeDontNeedButHaveToProvide = 0; //idk isnt used
            mc.playerController.windowClick(windowId, slotID, randomIntWeDontNeedButHaveToProvide, ClickType.QUICK_MOVE, ctx.player());
            amount = amount - (outputCount * inputCount);

            if (amount <= 0) {
                logDirect("done");
                //we finished crafting
                ctx.player().closeScreen();
                onLostControl();
            }
        }
    }

    private int getOutputCount() {
        return ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftResult.getStackInSlot(420).getCount();
    }

    private int getInputCount() {
        int stackSize = Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftMatrix.getStackInSlot(i);
            if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                stackSize = Math.min(itemStack.getCount(), stackSize);
            }
        }
        return stackSize;
    }

    private int getLowestMaxStackSize() {
        int maxStackSize = Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftMatrix.getStackInSlot(i);
            if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                maxStackSize = Math.min(itemStack.getMaxStackSize(), maxStackSize);
            }
        }
        return maxStackSize;
    }

    private void moveItemsToCraftingGrid() {
        int windowId = baritone.getPlayerContext().player().openContainer.windowId;
        //try to put the recipe the required amount of times in to the crafting grid.
        for (int i = 0; i*recipe.getRecipeOutput().getCount() < amount; i++) {
            mc.playerController.func_194338_a(windowId,recipe, GuiScreen.isShiftKeyDown(), ctx.player());
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
    public boolean canCraft(IRecipe recipe, int amount) {
        RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        for (ItemStack stack : ctx.player().inventory.mainInventory) {
            recipeItemHelper.accountStack(stack);
        }
        return recipeItemHelper.canCraft(recipe, null, amount);
    }

    @Override
    //should this be in a helper class?
    public boolean canCraft(Item item, int amount) {
        List<IRecipe> recipeList = getCraftingRecipes(item);
        for (IRecipe recipe : recipeList) {
            if(canCraft(recipe, amount)) {
                this.recipe = recipe;
                return true;
            }
        }
        return false;
        //we maybe still be able to craft but need to mix ingredients.
        //example we have 1 oak plank and 1 spruce plank and want to make sticks. this statement could be wrong.
    }

    @Override
    public void craftItem(Item item, int amount) {
        if(canCraft(item, amount)) {
            this.amount = amount;
            //todo check for crafting tables that are close. if none are found place one. else gotoBlock.
            baritone.getGetToBlockProcess().getToBlock(Blocks.CRAFTING_TABLE);
            logDirect("im totally crafting right now");
        } else {
            logDirect("unable to find a craftable recipe. do you have the necessary resources?");
        }
    }

    @Override
    //this is intended for use over the api
    public void craftRecipe(IRecipe recipe, int amount) {
        if(canCraft(recipe,amount)) {
            this.recipe = recipe;
            this.amount = amount;
            baritone.getGetToBlockProcess().getToBlock(Blocks.CRAFTING_TABLE);
        } else {
            logDirect("this recipe is not craftable with the available resources.");
        }
    }

    @Override
    public synchronized void onLostControl() {
        amount = 0;
        recipe = null;
    }

    @Override
    public String displayName0() {
        return "Crafting "+amount+"x "+recipe.getRecipeOutput().getDisplayName();
    }
}
