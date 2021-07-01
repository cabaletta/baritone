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

package baritone.utils;

import baritone.Baritone;
import baritone.api.cache.IWaypoint;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaritoneProcessHelper implements IBaritoneProcess, Helper {

    protected final Baritone baritone;
    protected final IPlayerContext ctx;
    private boolean usingChest;

    public BaritoneProcessHelper(Baritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    public void returnHome() {
        IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(IWaypoint.Tag.HOME);
        if (waypoint != null) {
            Goal goal = new GoalBlock(waypoint.getLocation());
            baritone.getCustomGoalProcess().setGoalAndPath(goal);
        } else {
            logDirect("No recent waypoint found, can't return home");
        }
    }

    public boolean putInventoryInChest(Set<Item> validDrops) {
        List<Slot> chestInv = ctx.player().openContainer.inventorySlots;
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.isEmpty() && validDrops.contains(inv.get(i).getItem())) {
                for (int j = 0; j < chestInv.size() - inv.size(); j++) {
                    if (chestInv.get(j).getStack().isEmpty()) {
                        ctx.playerController().windowClick(ctx.player().openContainer.windowId, i < 9 ? chestInv.size() - 9 + i : chestInv.size() - inv.size() + i - 9, 0, ClickType.QUICK_MOVE, ctx.player());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isInventoryFull() {
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        boolean inventoryFull = true;
        for (ItemStack stack : inv) {
            if (stack.isEmpty()) {
                inventoryFull = false;
                break;
            }
        }
        return inventoryFull;
    }

    public Set<ItemStack> notFullStacks(Set<Item> validDrops) {
        NonNullList<ItemStack> inv = ctx.player().inventory.mainInventory;
        Set<ItemStack> stacks = inv.stream()
                .filter(stack -> validDrops.contains(stack.getItem()) && stack.getMaxStackSize() > stack.getCount())
                .collect(Collectors.toCollection(HashSet::new));
        return stacks;
    }

    public BlockOptionalMetaLookup getBlacklistBlocks(Set<ItemStack> notFullStacks, BlockOptionalMetaLookup filter) {
        List<BlockOptionalMeta> blacklistBlocks = new ArrayList<>();
        OUTER:
        for (BlockOptionalMeta bom : filter.blocks()) {
            for (ItemStack stack : notFullStacks) {
                if (bom.matches(stack)) {
                    continue OUTER;
                }
            }
            blacklistBlocks.add(bom);
        }
        return new BlockOptionalMetaLookup(blacklistBlocks.toArray(new BlockOptionalMeta[blacklistBlocks.size()]));
    }

    public PathingCommand goToChest(boolean isSafeToCancel) {
        IWaypoint chestLoc = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(IWaypoint.Tag.CHEST);
        if (chestLoc != null) {
            BlockPos chest = chestLoc.getLocation();
            Goal goal = new GoalGetToBlock(chestLoc.getLocation());
            if (goal.isInGoal(ctx.playerFeet()) && goal.isInGoal(baritone.getPathingBehavior().pathStart())) {
                Optional<Rotation> rot = RotationUtils.reachable(ctx, chest);
                if (rot.isPresent() && isSafeToCancel) {
                    baritone.getLookBehavior().updateTarget(rot.get(), true);
                    if (ctx.isLookingAt(chest)) {
                        if (ctx.player().openContainer == ctx.player().inventoryContainer) {
                            baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                        } else {
                            baritone.getInputOverrideHandler().clearAllKeys();
                        }
                    }
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
            } else {
                return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
            }
        } else {
            logDirect("No chest set, please use #setchest");
        }
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
    }

    public PathingCommand handleInventory(boolean isSafeToCancel, Set<Item> validDrops) {
        PathingCommand result = null;
        if (Baritone.settings().checkInventory.value) {
            boolean invFull = isInventoryFull();
            if (invFull) {
                Set<ItemStack> notFullStacks = notFullStacks(validDrops);
                if (notFullStacks.isEmpty()) {
                    if (Baritone.settings().putDropsInChest.value) {
                        result = goToChest(isSafeToCancel);
                        usingChest = result.commandType == PathingCommandType.REQUEST_PAUSE;
                    } else {
                        if (Baritone.settings().goHome.value) {
                            returnHome();
                        }
                        onLostControl();
                        logDirect("Inventory is full; Stopping current task.");
                    }
                }
            }
            if (usingChest && putInventoryInChest(validDrops)) {
                ctx.player().closeScreen();
                if (invFull) {
                    if (Baritone.settings().goHome.value) {
                        returnHome();
                    }
                    onLostControl();
                    logDirect("Inventory and chest are full; Stopping current task.");
                }
                usingChest = false;
            }
        }
        return result;
    }
}
