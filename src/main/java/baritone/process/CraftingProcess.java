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
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.ICraftingProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {

    //todo this is a copy paste of the getToBlockProcess and hasnt been cleand up

    private BlockOptionalMeta gettingTo;
    private List<BlockPos> knownLocations;
    private List<BlockPos> blacklist; // locations we failed to calc to
    private BlockPos start;

    private int tickCount = 0;
    private int arrivalTickCount = 0;
    private RecipeBook book;
    private Item item;
    private int amount;

    public CraftingProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void getToBlock(BlockOptionalMeta block) {
        onLostControl();
        gettingTo = block;
        start = ctx.playerFeet();
        blacklist = new ArrayList<>();
        arrivalTickCount = 0;
        rescan(new ArrayList<>(), new GetToBlockCalculationContext(false));
    }

    @Override
    public boolean isActive() {
        return item != null && amount >= 1;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (baritone.getGetToBlockProcess().isActive()) {
            return new PathingCommand(null, PathingCommandType.DEFER);
        } else {
            //logDirect("we no longer pathing so its time to craft");
            try {
                int outputCount = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftResult.getStackInSlot(420).getCount();
                int inputCount = 0;
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = ((ContainerWorkbench) baritone.getPlayerContext().player().openContainer).craftMatrix.getStackInSlot(i);
                    if (itemStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                        inputCount = itemStack.getCount();
                        break;
                    }
                }
                if (outputCount * inputCount >= amount || inputCount == 64) {
                    int a = ctx.player().openContainer.windowId;
                    int b = 0; //slot id. for crafting table output it is 0
                    int c = 0; //idk isnt used
                    Minecraft.getMinecraft().playerController.windowClick(a, b, c, ClickType.QUICK_MOVE, ctx.player());
                    logDirect("we finished crafting");
                    onLostControl();
                }
                if (mc.currentScreen instanceof GuiCrafting) {
                    moveItemsToCraftingGrid();
                }
            } catch (Exception e) {
                //you probably closed the crafting window.
                onLostControl();
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
    }

    private void moveItemsToCraftingGrid() {
        int a = baritone.getPlayerContext().player().openContainer.windowId;
        IRecipe recipe = getCraftingRecipes(item).get(0);
        boolean b = GuiScreen.isShiftKeyDown();
        //create a package to move a recipe into the crafting grid.
        //CPacketPlaceRecipe placeRecipe = new CPacketPlaceRecipe(a, recipe, b);

        for (int i = 0; i*recipe.getRecipeOutput().getCount() < amount; i++) {
            mc.playerController.func_194338_a(a,recipe, b, ctx.player());
        }

        //send package
        //baritone.getPlayerContext().player().connection.sendPacket(placeRecipe);
    }

    // blacklist the closest block and its adjacent blocks
    public synchronized boolean blacklistClosest() {
        List<BlockPos> newBlacklist = new ArrayList<>();
        knownLocations.stream().min(Comparator.comparingDouble(ctx.player()::getDistanceSq)).ifPresent(newBlacklist::add);
        outer:
        while (true) {
            for (BlockPos known : knownLocations) {
                for (BlockPos blacklist : newBlacklist) {
                    if (areAdjacent(known, blacklist)) { // directly adjacent
                        newBlacklist.add(known);
                        knownLocations.remove(known);
                        continue outer;
                    }
                }
            }
            // i can't do break; (codacy gets mad), and i can't do if(true){break}; (codacy gets mad)
            // so i will do this
            switch (newBlacklist.size()) {
                default:
                    break outer;
            }
        }
        logDebug("Blacklisting unreachable locations " + newBlacklist);
        blacklist.addAll(newBlacklist);
        return !newBlacklist.isEmpty();
    }

    @Override
    public boolean hasCraftingRecipe(Item item) {
        ArrayList<IRecipe> recipes = getCraftingRecipes(item);
        return !recipes.isEmpty();
    }

    @Override
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
    public boolean canCraft(Item item, int amount) {
        RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        IRecipe recipe = getCraftingRecipes(item).get(0);
        boolean canDo = recipeItemHelper.canCraft(recipe, null, amount);
        return canDo;

        /*EntityPlayerSP player = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player();
        RecipeBook book = player.getRecipeBook();
        ArrayList<ItemStack> invSlots = new ArrayList<>();
        invSlots.addAll(player.inventory.mainInventory);
        invSlots.addAll(player.inventory.offHandInventory);
        ArrayList<IRecipe> recipes = getCraftingRecipes(item);
        for (IRecipe recipe : recipes) {
            logDirect(recipe.getRecipeOutput().getTranslationKey());
        }
        IRecipe recipeInUse = recipes.get(0);
        int timesToCraft = (amount / recipeInUse.getRecipeOutput().getCount()) + (amount % recipeInUse.getRecipeOutput().getCount() == 0 ? 0 : 1);
        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ingredients.addAll(recipeInUse.getIngredients());
        for (Ingredient ingredient : ingredients) {
            ingredient.getMatchingStacks()[0].getCount()*timesToCraft;
        }
        //idk lets just assume we can
        return true;/**/
    }

    @Override
    public void craft(Item item, int amount) {
        //active = true;
        book = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player().getRecipeBook();
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

    // this is to signal to MineProcess that we don't care about the allowBreak setting
    // it is NOT to be used to actually calculate a path
    public class GetToBlockCalculationContext extends CalculationContext {

        public GetToBlockCalculationContext(boolean forUseOnAnotherThread) {
            super(CraftingProcess.super.baritone, forUseOnAnotherThread);
        }

        @Override
        public double breakCostMultiplierAt(int x, int y, int z, IBlockState current) {
            return 1;
        }
    }

    // safer than direct double comparison from distanceSq
    private boolean areAdjacent(BlockPos posA, BlockPos posB) {
        int diffX = Math.abs(posA.getX() - posB.getX());
        int diffY = Math.abs(posA.getY() - posB.getY());
        int diffZ = Math.abs(posA.getZ() - posB.getZ());
        return (diffX + diffY + diffZ) == 1;
    }

    @Override
    public synchronized void onLostControl() {
        amount = 0;
        item = null;

        gettingTo = null;
        knownLocations = null;
        start = null;
        blacklist = null;
        baritone.getInputOverrideHandler().clearAllKeys();
    }

    @Override
    public String displayName0() {
        if (knownLocations.isEmpty()) {
            return "Exploring randomly to find " + gettingTo + ", no known locations";
        }
        return "Get To " + gettingTo + ", " + knownLocations.size() + " known locations";
    }

    private synchronized void rescan(List<BlockPos> known, CalculationContext context) {
        List<BlockPos> positions = MineProcess.searchWorld(context, new BlockOptionalMetaLookup(gettingTo), 64, known, blacklist, Collections.emptyList());
        positions.removeIf(blacklist::contains);
        knownLocations = positions;
    }

    private Goal createGoal(BlockPos pos) {
        if (walkIntoInsteadOfAdjacent(gettingTo.getBlock())) {
            return new GoalTwoBlocks(pos);
        }
        if (blockOnTopMustBeRemoved(gettingTo.getBlock()) && baritone.bsi.get0(pos.up()).isBlockNormalCube()) {
            return new GoalBlock(pos.up());
        }
        return new GoalGetToBlock(pos);
    }

    private boolean rightClick() {
        for (BlockPos pos : knownLocations) {
            Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), pos, ctx.playerController().getBlockReachDistance());
            if (reachable.isPresent()) {
                baritone.getLookBehavior().updateTarget(reachable.get(), true);
                if (knownLocations.contains(ctx.getSelectedBlock().orElse(null))) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true); // TODO find some way to right click even if we're in an ESC menu
                    System.out.println(ctx.player().openContainer);
                    if (!(ctx.player().openContainer instanceof ContainerPlayer)) {
                        return true;
                    }
                }
                if (arrivalTickCount++ > 20) {
                    logDirect("Right click timed out");
                    return true;
                }
                return false; // trying to right click, will do it next tick or so
            }
        }
        logDirect("Arrived but failed to right click open");
        return true;
    }

    private boolean walkIntoInsteadOfAdjacent(Block block) {
        if (!Baritone.settings().enterPortal.value) {
            return false;
        }
        return block == Blocks.PORTAL;
    }

    private boolean rightClickOnArrival(Block block) {
        if (!Baritone.settings().rightClickContainerOnArrival.value) {
            return false;
        }
        return block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || block == Blocks.LIT_FURNACE || block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }

    private boolean blockOnTopMustBeRemoved(Block block) {
        if (!rightClickOnArrival(block)) { // only if we plan to actually open it on arrival
            return false;
        }
        // only these chests; you can open a crafting table or furnace even with a block on top
        return block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }
}
