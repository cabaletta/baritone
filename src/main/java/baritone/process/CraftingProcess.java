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
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.client.CPacketPlaceRecipe;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public final class CraftingProcess extends BaritoneProcessHelper implements ICraftingProcess {
    private int amount;
    private List<IRecipe> recipes;
    private Goal goal;
    private boolean placeCraftingTable = false;
    private boolean clearToPush;
    private List<BlockPos> knownLocations;

    public CraftingProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return recipes != null && amount >= 1;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (calcFailed) {
            logDirect("path calculation failed");
            onLostControl();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        } else if (goal != null && !(goal instanceof GoalRunAway)) {
            //we are pathing to a table and therefor have to wait.
            baritone.getInputOverrideHandler().clearAllKeys();
            if (goal.isInGoal(ctx.playerFeet())) {
                rightClick();
                if (ctx.player().openContainer instanceof ContainerWorkbench) {
                    goal = null;
                }
            }
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        } else if (placeCraftingTable) {
            placeCraftingTable();
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        } else {
            //we no longer pathing so it's time to craft
            try {
                while (!canCraft(recipes.subList(0,1), 1) && amount > 0) {
                    if (recipes.size() == 1) {
                        logDirect("Insufficient Resources");
                        onLostControl();
                        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
                    }
                    recipes = recipes.subList(1, recipes.size());
                }
                if (!canCraftInInventory(recipes.get(0)) && !(ctx.player().openContainer instanceof ContainerWorkbench)) {
                    pathToACraftingTable();
                    return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
                }
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
        recipes = null;
        goal = null;
        placeCraftingTable = false;
        knownLocations = null;
        baritone.getInputOverrideHandler().clearAllKeys();
    }

    @Override
    public String displayName0() {
        return "Crafting " + amount + "x " + recipes.get(0).getRecipeOutput().getDisplayName();
    }

    @Override
    public void craft(List<IRecipe> recipes, int amount) {
        this.recipes = recipes;
        this.amount = amount;
        clearToPush = true;
        logDirect("Crafting now " + amount + "x [" + recipes.get(0).getRecipeOutput().getDisplayName() + "]");
    }

    @Override
    public List<IRecipe> getCraftingRecipes(Item item, boolean allCraftingRecipes) {
        List<IRecipe> recipes = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.getRecipeOutput().getItem().equals(item) && (canCraft(recipe) || allCraftingRecipes)) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    @Override
    public boolean canCraft(List<IRecipe> recipes, int amount) {
        RecipeItemHelper recipeItemHelper = new RecipeItemHelper();
        for (ItemStack stack : ctx.player().inventory.mainInventory) {
            recipeItemHelper.accountStack(stack);
        }
        int totalPossibleToCraft = 0;
        for (IRecipe recipe : recipes) {
            totalPossibleToCraft = totalPossibleToCraft + (recipeItemHelper.getBiggestCraftableStack(recipe,null) * recipe.getRecipeOutput().getCount());
        }
        return totalPossibleToCraft >= amount;
    }

    private boolean canCraft(IRecipe recipe) {
        List<IRecipe> recipeList = new ArrayList<>();
        recipeList.add(recipe);
        return canCraft(recipeList, 1);
    }

    @Override
    public boolean canCraftInInventory(IRecipe recipe) {
        return recipe.canFit(2, 2) && !ctx.player().isCreative();
    }

    private void moveItemsToCraftingGrid() {
        clearToPush = false;
        int windowId = ctx.player().openContainer.windowId;
        //try to put the recipe the required amount of times in to the crafting grid.
        for (int i = 0; i * recipes.get(0).getRecipeOutput().getCount() < amount; i++) {
            ctx.player().connection.sendPacket(new CPacketPlaceRecipe(windowId, recipes.get(0), GuiScreen.isShiftKeyDown()));
        }
    }

    private void takeResultFromOutput() {
        int inputCount = getInputCount();
        if (inputCount > 0) {
            int windowId = ctx.player().openContainer.windowId;
            int slotID = 0; //see cheat sheet https://wiki.vg/Inventory
            int mouseButton = 0;
            ctx.playerController().windowClick(windowId, slotID, mouseButton, ClickType.QUICK_MOVE, ctx.player());
            amount = amount - (recipes.get(0).getRecipeOutput().getCount() * inputCount);
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

    private void pathToACraftingTable() {
        knownLocations = MineProcess.searchWorld(new CalculationContext(baritone, false), new BlockOptionalMetaLookup(Blocks.CRAFTING_TABLE), 64, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        if (knownLocations.isEmpty()) {
            if (baritone.getInventoryBehavior().throwaway(false, this::isCraftingTable)) {
                placeCraftingTable = true;
            } else {
                logDirect("Recipe requires a crafting table.");
                onLostControl();
            }
        } else {
            goal = new GoalComposite(knownLocations.stream().map(this::createGoal).toArray(Goal[]::new));
        }
    }

    private boolean isCraftingTable(ItemStack itemStack) {
        return itemStack.getItem() == Item.getItemFromBlock(Blocks.CRAFTING_TABLE);
    }

    private Goal createGoal(BlockPos pos) {
        return new GoalGetToBlock(pos);
    }

    private void rightClick() {
        BlockPos bp = null;
        if (goal instanceof GoalComposite) {
            Goal[] goals = ((GoalComposite) goal).goals();
            for (Goal goal : goals) {
                if (goal.isInGoal(ctx.playerFeet()) && goal instanceof GoalGetToBlock) {
                    bp = ((GoalGetToBlock) goal).getGoalPos();
                    break;
                }
            }
        } else if (goal instanceof GoalGetToBlock) {
            bp = ((GoalGetToBlock) goal).getGoalPos();
        }

        if (bp != null) {
            Optional<Rotation> reachable = RotationUtils.reachable(ctx, bp, ctx.playerController().getBlockReachDistance());
            if (reachable.isPresent()) {
                baritone.getLookBehavior().updateTarget(reachable.get(), true);
                if (bp.equals(ctx.getSelectedBlock().orElse(null))) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                    if (!(ctx.player().openContainer instanceof ContainerPlayer)) {
                        baritone.getInputOverrideHandler().clearAllKeys();
                    }
                }
            }
        }
    }

    private void placeCraftingTable() {
        Optional<Placement> toPlace = searchPlacement();

        if (toPlace.isPresent()) {
            Rotation rot = toPlace.get().rot;
            baritone.getLookBehavior().updateTarget(rot, true);
            baritone.getInventoryBehavior().throwaway(true, this::isCraftingTable);
            baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
            if ((ctx.isLookingAt(toPlace.get().placeAgainst) && ctx.objectMouseOver().sideHit.equals(toPlace.get().side)) || ctx.playerRotations().isReallyCloseTo(rot)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                //now that the table is placed path to it
                placeCraftingTable = false;
                goal = new GoalGetToBlock(toPlace.get().placeAgainst.offset(toPlace.get().side));
            }
        } else {
            goal = new GoalRunAway(5, ctx.playerFeet());
        }
    }

    private Optional<Placement> searchPlacement() {
        for (BetterBlockPos bbp : blockPosListSortedByDistanceFromCenter()) {
            if (MovementHelper.isReplaceable(bbp.x, bbp.y, bbp.z, baritone.bsi.get0(bbp.x, bbp.y, bbp.z), baritone.bsi)) {
                if (bbp.y == ctx.playerHead().y && baritone.bsi.get0(bbp.x, bbp.y + 1, bbp.z).getBlock() == Blocks.AIR) {
                    continue;
                }
                Optional<Placement> opt = possibleToPlace(Blocks.CRAFTING_TABLE.getDefaultState(), bbp.x, bbp.y, bbp.z, baritone.bsi);
                if (opt.isPresent()) {
                    return opt;
                }
            }
        }
        return Optional.empty();
    }

    private List<BetterBlockPos> blockPosListSortedByDistanceFromCenter() {
        //this is so stupid idk why im doing this
        Map<Integer, BetterBlockPos> map = new HashMap<>();
        List<Integer> usedIndexes = new ArrayList<>();
        BetterBlockPos center = ctx.playerFeet();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;

                    BetterBlockPos bbp = new BetterBlockPos(x, y, z);
                    int approxPosInList = (int) bbp.distanceSq(center) * 1000;
                    while (usedIndexes.contains(approxPosInList)) {
                        approxPosInList++;
                    }
                    map.put(approxPosInList, bbp);
                    usedIndexes.add(approxPosInList);
                }
            }
        }
        List<BetterBlockPos> sortedlist = new ArrayList<>();
        map.keySet().stream().sorted().forEach(key -> sortedlist.add(map.get(key)));
        return sortedlist;
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
                    return Optional.of(new Placement(placeAgainstPos, against.getOpposite(), rot));
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

    public static class Placement {
        private final BlockPos placeAgainst;
        private final EnumFacing side;
        private final Rotation rot;

        public Placement(BlockPos placeAgainst, EnumFacing side, Rotation rot) {
            this.placeAgainst = placeAgainst;
            this.side = side;
            this.rot = rot;
        }
    }
}
