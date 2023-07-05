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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.Helper;
import baritone.utils.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Predicate;

public final class InventoryBehavior extends Behavior implements Helper {

    int ticksSinceLastInventoryMove;
    int[] lastTickRequestedMove; // not everything asks every tick, so remember the request while coming to a halt

    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (!Baritone.settings().allowInventory.value) {
            return;
        }
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (ctx.player().openContainer != ctx.player().container) {
            // we have a crafting table or a chest or something open
            return;
        }
        ticksSinceLastInventoryMove++;
        if (firstValidThrowaway() >= 9) { // aka there are none on the hotbar, but there are some in main inventory
            requestSwapWithHotBar(firstValidThrowaway(), 8);
        }
        int pick = bestToolAgainst(Blocks.STONE, PickaxeItem.class);
        if (pick >= 9) {
            requestSwapWithHotBar(pick, 0);
        }
        if (lastTickRequestedMove != null) {
            logDebug("Remembering to move " + lastTickRequestedMove[0] + " " + lastTickRequestedMove[1] + " from a previous tick");
            requestSwapWithHotBar(lastTickRequestedMove[0], lastTickRequestedMove[1]);
        }
    }

    public boolean attemptToPutOnHotbar(int inMainInvy, Predicate<Integer> disallowedHotbar) {
        OptionalInt destination = getTempHotbarSlot(disallowedHotbar);
        if (destination.isPresent()) {
            if (!requestSwapWithHotBar(inMainInvy, destination.getAsInt())) {
                return false;
            }
        }
        return true;
    }

    public OptionalInt getTempHotbarSlot(Predicate<Integer> disallowedHotbar) {
        // we're using 0 and 8 for pickaxe and throwaway
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            if (ctx.player().inventory.mainInventory.get(i).isEmpty() && !disallowedHotbar.test(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            for (int i = 1; i < 8; i++) {
                if (!disallowedHotbar.test(i)) {
                    candidates.add(i);
                }
            }
        }
        if (candidates.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(candidates.get(new Random().nextInt(candidates.size())));
    }

    private boolean requestSwapWithHotBar(int inInventory, int inHotbar) {
        lastTickRequestedMove = new int[]{inInventory, inHotbar};
        if (ticksSinceLastInventoryMove < Baritone.settings().ticksBetweenInventoryMoves.value) {
            logDebug("Inventory move requested but delaying " + ticksSinceLastInventoryMove + " " + Baritone.settings().ticksBetweenInventoryMoves.value);
            return false;
        }
        if (Baritone.settings().inventoryMoveOnlyIfStationary.value && !baritone.getInventoryPauserProcess().stationaryForInventoryMove()) {
            logDebug("Inventory move requested but delaying until stationary");
            return false;
        }
        ctx.playerController().windowClick(ctx.player().container.windowId, inInventory < 9 ? inInventory + 36 : inInventory, inHotbar, ClickType.SWAP, ctx.player());
        ticksSinceLastInventoryMove = 0;
        lastTickRequestedMove = null;
        return true;
    }

    private int firstValidThrowaway() { // TODO offhand idk
        NonNullList<ItemStack> invy = ctx.player().inventory.mainInventory;
        for (int i = 0; i < invy.size(); i++) {
            if (Baritone.settings().acceptableThrowawayItems.value.contains(invy.get(i).getItem())) {
                return i;
            }
        }
        return -1;
    }

    private int bestToolAgainst(Block against, Class<? extends ToolItem> cla$$) {
        NonNullList<ItemStack> invy = ctx.player().inventory.mainInventory;
        int bestInd = -1;
        double bestSpeed = -1;
        for (int i = 0; i < invy.size(); i++) {
            ItemStack stack = invy.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (Baritone.settings().itemSaver.value && (stack.getDamage() + Baritone.settings().itemSaverThreshold.value) >= stack.getMaxDamage() && stack.getMaxDamage() > 1) {
                continue;
            }
            if (cla$$.isInstance(stack.getItem())) {
                double speed = ToolSet.calculateSpeedVsBlock(stack, against.getDefaultState()); // takes into account enchants
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestInd = i;
                }
            }
        }
        return bestInd;
    }

    public boolean hasGenericThrowaway() {
        for (Item item : Baritone.settings().acceptableThrowawayItems.value) {
            if (throwaway(false, stack -> item.equals(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }

    public boolean selectThrowawayForLocation(boolean select, int x, int y, int z) {
        BlockState maybe = baritone.getBuilderProcess().placeAt(x, y, z, baritone.bsi.get0(x, y, z));
        if (maybe != null && throwaway(select, stack -> stack.getItem() instanceof BlockItem && maybe.equals(((BlockItem) stack.getItem()).getBlock().getStateForPlacement(new BlockItemUseContext(new ItemUseContext(ctx.world(), ctx.player(), Hand.MAIN_HAND, stack, new BlockRayTraceResult(new Vector3d(ctx.player().getPositionVec().x, ctx.player().getPositionVec().y, ctx.player().getPositionVec().z), Direction.UP, ctx.playerFeet(), false)) {}))))) {
            return true; // gotem
        }
        if (maybe != null && throwaway(select, stack -> stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock().equals(maybe.getBlock()))) {
            return true;
        }
        for (Item item : Baritone.settings().acceptableThrowawayItems.value) {
            if (throwaway(select, stack -> item.equals(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }

    public boolean throwaway(boolean select, Predicate<? super ItemStack> desired) {
        return throwaway(select, desired, Baritone.settings().allowInventory.value);
    }

    public boolean throwaway(boolean select, Predicate<? super ItemStack> desired, boolean allowInventory) {
        ClientPlayerEntity p = ctx.player();
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            // this usage of settings() is okay because it's only called once during pathing
            // (while creating the CalculationContext at the very beginning)
            // and then it's called during execution
            // since this function is never called during cost calculation, we don't need to migrate
            // acceptableThrowawayItems to the CalculationContext
            if (desired.test(item)) {
                if (select) {
                    p.inventory.currentItem = i;
                }
                return true;
            }
        }
        if (desired.test(p.inventory.offHandInventory.get(0))) {
            // main hand takes precedence over off hand
            // that means that if we have block A selected in main hand and block B in off hand, right clicking places block B
            // we've already checked above ^ and the main hand can't possible have an acceptablethrowawayitem
            // so we need to select in the main hand something that doesn't right click
            // so not a shovel, not a hoe, not a block, etc
            for (int i = 0; i < 9; i++) {
                ItemStack item = inv.get(i);
                if (item.isEmpty() || item.getItem() instanceof PickaxeItem) {
                    if (select) {
                        p.inventory.currentItem = i;
                    }
                    return true;
                }
            }
        }

        if (allowInventory) {
            for (int i = 9; i < 36; i++) {
                if (desired.test(inv.get(i))) {
                    if (select) {
                        requestSwapWithHotBar(i, 7);
                        p.inventory.currentItem = 7;
                    }
                    return true;
                }
            }
        }

        return false;
    }
}
