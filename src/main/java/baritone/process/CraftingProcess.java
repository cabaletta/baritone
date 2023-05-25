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
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.*;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {

    //todo this is a copy paste of the getToBlockProcess and hasnt been cleand up

    private Item item;
    private int amount;

    public CraftingProcess(Baritone baritone) {
        super(baritone);
    }


    @Override
    public boolean isActive() {
        return item != null && amount >= 1;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (baritone.getGetToBlockProcess().isActive()) {
            //we are pathing to a table and therefor have to wait.
            return new PathingCommand(null, PathingCommandType.DEFER);
        } else {
            //we no longer pathing so it's time to craft
            try {
                //todo if we cant craft more because of stack limit or lack of resources grab result and either throw low resource exception or continue crafting
                int outputCount = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftResult.getStackInSlot(420).getCount(); //items per crafting cycle
                int inputCount = 0;
                //todo this should, in order to consider low or non stackable items, search for the lowest non zero stack count
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftMatrix.getStackInSlot(i);
                    if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                        inputCount = itemStack.getCount();
                        break;
                    }
                }

                //if we have enought or reached stack limit on a input stack grab the result and subtract it from the target amount.
                // todo dynamicly check input stack limit based on lowest stackable item
                if (outputCount * inputCount >= amount || inputCount == 64) {
                    int windowId = ctx.player().openContainer.windowId;
                    int slotID = 0; //slot id. for crafting table output it is 0
                    int randomIntWeDontNeedButHaveToProvide = 0; //idk isnt used
                    mc.playerController.windowClick(windowId, slotID, randomIntWeDontNeedButHaveToProvide, ClickType.QUICK_MOVE, ctx.player());
                    amount = amount - (outputCount * inputCount);

                    if (amount <= 0) {
                        //we finished crafting
                        ctx.player().closeScreen();
                        onLostControl();
                    }
                }
                if (mc.currentScreen instanceof GuiCrafting) {
                    if (canCraft(item, amount)) {
                        moveItemsToCraftingGrid();
                    } else {
                        logDirect("we cant craft"); //this should be a more meaning full message also if we check craftability beforehand we should never run out of resources mid crafting
                        mc.player.closeScreen();
                        onLostControl();
                    }
                }
            } catch (Exception e) {
                //you probably closed the crafting window while crafting process was still running.
                onLostControl();
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
    }

    private void moveItemsToCraftingGrid() {
        int windowId = baritone.getPlayerContext().player().openContainer.windowId;
        IRecipe recipe = getCraftingRecipes(item).get(0); //todo better way to get a recipe

        //try to put the recipe the required amount of times in to the crafting grid.
        for (int i = 0; i*recipe.getRecipeOutput().getCount() < amount; i++) {
            mc.playerController.func_194338_a(windowId,recipe, GuiScreen.isShiftKeyDown(), ctx.player());
        }
    }

    @Override
    public boolean hasCraftingRecipe(Item item) {
        ArrayList<IRecipe> recipes = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.getRecipeOutput().getItem().equals(item)) {
                recipes.add(recipe);
            }
        }
        return !recipes.isEmpty();
    }

    @Override
    public ArrayList<IRecipe> getCraftingRecipes(Item item) {
        ((GuiCrafting)mc.currentScreen).func_194310_f().getSearchBar().setText("itemname");
        List<RecipeList> recipeLists = ((GuiCrafting)mc.currentScreen).func_194310_f().getRecipeBookPage().getRecipeLists();
        return (ArrayList<IRecipe>) recipeLists.get(0).getRecipes();
    }

    @Override
    public boolean canCraft(Item item, int amount) {
        RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        IRecipe recipe = getCraftingRecipes(item).stream().findAny().get();
        return recipeItemHelper.canCraft(recipe, null, amount);
    }

    @Override
    public void craft(Item item, int amount) {
        //book = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player().getRecipeBook();
        this.item = item;
        this.amount = amount;
        //todo check for crafting tables that are close. if none are found place one. else gotoBlock.
        baritone.getGetToBlockProcess().getToBlock(Blocks.CRAFTING_TABLE);
        //todo once we are at the crafting table start crafting

        //todo check if crafting gui is open
        //book.setGuiOpen(true);
        //book.setFilteringCraftable(true);
        //RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        logDirect("im totaly crafting right now");
    }

    @Override
    public synchronized void onLostControl() {
        amount = 0;
        item = null;
    }

    @Override
    public String displayName0() {
        return "Crafting "+amount+"x "+item.getTranslationKey();
    }
}
