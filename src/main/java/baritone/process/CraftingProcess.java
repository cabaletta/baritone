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
import baritone.api.pathing.goals.GoalRunAway;
import baritone.api.process.ICraftingProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.schematic.FillSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {
    private int amount;
    private IRecipe recipe;
    private Goal goal;
    private boolean placeCraftingTable = false;
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
        if (calcFailed) {
            logDirect("path calculation failed");
            onLostControl();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        } else if (goal != null && !(goal instanceof GoalRunAway)) {
            //we are pathing to a table and therefor have to wait.
            if (baritone.getInputOverrideHandler().isInputForcedDown(Input.SNEAK)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, false);
            }
            if (baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_RIGHT)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            }
            if (goal.isInGoal(ctx.playerFeet())) {
                rightClick();
                if (ctx.player().openContainer instanceof ContainerWorkbench) {
                    goal = null;
                }
            }
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        } else if (placeCraftingTable) {
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
        placeCraftingTable = false;
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
                placeCraftingTable = true;
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

    private void placeCraftingtableNearby() {
        selectCraftingTable();

        ISchematic schematic = new FillSchematic(5, 4, 5, Blocks.CRAFTING_TABLE.getDefaultState());

        List<IBlockState> desirableOnHotbar = new ArrayList<>();
        Optional<Placement> toPlace = searchForPlaceables(schematic, desirableOnHotbar);

        if (toPlace.isPresent()) {
            Rotation rot = toPlace.get().rot;
            baritone.getLookBehavior().updateTarget(rot, true);
            ctx.player().inventory.currentItem = toPlace.get().hotbarSelection;
            baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
            if ((ctx.isLookingAt(toPlace.get().placeAgainst) && ctx.objectMouseOver().sideHit.equals(toPlace.get().side)) || ctx.playerRotations().isReallyCloseTo(rot)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                placeCraftingTable = false;
                getACraftingTable();
            }
        } else {
            goal = new GoalRunAway(5, ctx.playerFeet());
        }
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

    private Optional<Placement> searchForPlaceables(ISchematic schematic, List<IBlockState> desirableOnHotbar) {
        BetterBlockPos center = ctx.playerFeet();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    List<IBlockState> approxPlacable = new ArrayList<>();
                    approxPlacable.add(Blocks.CRAFTING_TABLE.getDefaultState());
                    IBlockState desired = schematic.desiredState(x, y, z, baritone.bsi.get0(x, y, z), approxPlacable);
                    if (desired == null) {
                        continue; // irrelevant
                    }
                    IBlockState curr = baritone.bsi.get0(x, y, z);
                    if (MovementHelper.isReplaceable(x, y, z, curr, baritone.bsi) && !valid(curr, desired, false)) {
                        if (dy == 1 && baritone.bsi.get0(x, y + 1, z).getBlock() == Blocks.AIR) {
                            continue;
                        }
                        desirableOnHotbar.add(desired);
                        Optional<Placement> opt = possibleToPlace(desired, x, y, z, baritone.bsi);
                        if (opt.isPresent()) {
                            return opt;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean valid(IBlockState current, IBlockState desired, boolean itemVerify) {
        if (desired == null) {
            return true;
        }
        return current.equals(desired);
    }

    private Optional<Placement> possibleToPlace(IBlockState toPlace, int x, int y, int z, BlockStateInterface bsi) {
        for (EnumFacing against : EnumFacing.values()) {
            BetterBlockPos placeAgainstPos = new BetterBlockPos(x, y, z).offset(against);
            IBlockState placeAgainstState = bsi.get0(placeAgainstPos);
            if (MovementHelper.isReplaceable(placeAgainstPos.x, placeAgainstPos.y, placeAgainstPos.z, placeAgainstState, bsi)) {
                continue;
            }
            if (!ctx.world().mayPlace(toPlace.getBlock(), new BetterBlockPos(x, y, z), false, against, null)) {
                continue;
            }
            AxisAlignedBB aabb = placeAgainstState.getBoundingBox(ctx.world(), placeAgainstPos);
            for (Vec3d placementMultiplier : aabbSideMultipliers(against)) {
                double placeX = placeAgainstPos.x + aabb.minX * placementMultiplier.x + aabb.maxX * (1 - placementMultiplier.x);
                double placeY = placeAgainstPos.y + aabb.minY * placementMultiplier.y + aabb.maxY * (1 - placementMultiplier.y);
                double placeZ = placeAgainstPos.z + aabb.minZ * placementMultiplier.z + aabb.maxZ * (1 - placementMultiplier.z);
                Rotation rot = RotationUtils.calcRotationFromVec3d(RayTraceUtils.inferSneakingEyePosition(ctx.player()), new Vec3d(placeX, placeY, placeZ), ctx.playerRotations());
                RayTraceResult result = RayTraceUtils.rayTraceTowards(ctx.player(), rot, ctx.playerController().getBlockReachDistance(), true);
                if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK && result.getBlockPos().equals(placeAgainstPos) && result.sideHit == against.getOpposite()) {
                    OptionalInt hotbar = hasAnyItemThatWouldPlace(toPlace, result, rot);
                    if (hotbar.isPresent()) {
                        return Optional.of(new Placement(hotbar.getAsInt(), placeAgainstPos, against.getOpposite(), rot));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Vec3d[] aabbSideMultipliers(EnumFacing side) {
        switch (side) {
            case UP:
                return new Vec3d[]{new Vec3d(0.5, 1, 0.5), new Vec3d(0.1, 1, 0.5), new Vec3d(0.9, 1, 0.5), new Vec3d(0.5, 1, 0.1), new Vec3d(0.5, 1, 0.9)};
            case DOWN:
                return new Vec3d[]{new Vec3d(0.5, 0, 0.5), new Vec3d(0.1, 0, 0.5), new Vec3d(0.9, 0, 0.5), new Vec3d(0.5, 0, 0.1), new Vec3d(0.5, 0, 0.9)};
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                double x = side.getXOffset() == 0 ? 0.5 : (1 + side.getXOffset()) / 2D;
                double z = side.getZOffset() == 0 ? 0.5 : (1 + side.getZOffset()) / 2D;
                return new Vec3d[]{new Vec3d(x, 0.25, z), new Vec3d(x, 0.75, z)};
            default: // null
                throw new IllegalStateException();
        }
    }

    private OptionalInt hasAnyItemThatWouldPlace(IBlockState desired, RayTraceResult result, Rotation rot) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = ctx.player().inventory.mainInventory.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }
            float originalYaw = ctx.player().rotationYaw;
            float originalPitch = ctx.player().rotationPitch;
            // the state depends on the facing of the player sometimes
            ctx.player().rotationYaw = rot.getYaw();
            ctx.player().rotationPitch = rot.getPitch();
            IBlockState wouldBePlaced = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(
                    ctx.world(),
                    result.getBlockPos().offset(result.sideHit),
                    result.sideHit,
                    (float) result.hitVec.x - result.getBlockPos().getX(), // as in PlayerControllerMP
                    (float) result.hitVec.y - result.getBlockPos().getY(),
                    (float) result.hitVec.z - result.getBlockPos().getZ(),
                    stack.getItem().getMetadata(stack.getMetadata()),
                    ctx.player()
            );
            ctx.player().rotationYaw = originalYaw;
            ctx.player().rotationPitch = originalPitch;
            if (valid(wouldBePlaced, desired, true)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public static class Placement {

        private final int hotbarSelection;
        private final BlockPos placeAgainst;
        private final EnumFacing side;
        private final Rotation rot;

        public Placement(int hotbarSelection, BlockPos placeAgainst, EnumFacing side, Rotation rot) {
            this.hotbarSelection = hotbarSelection;
            this.placeAgainst = placeAgainst;
            this.side = side;
            this.rot = rot;
        }
    }
}
