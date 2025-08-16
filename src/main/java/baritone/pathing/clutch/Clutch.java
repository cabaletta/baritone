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

package baritone.pathing.clutch;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Pair;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class Clutch {
    private final double costMultiplier;

    protected Clutch(double costMultiplier) {
        this.costMultiplier = costMultiplier;
    }

    protected Clutch() {
        this(1d);
    }

    public final ItemStack getClutchingItem(CalculationContext context) { // We could return the slot instead of the item
        for (int slot = 0; slot < (Baritone.settings().allowInventory.value ? 36 : 9); slot++) {
            ItemStack item = context.getBaritone().getPlayerContext().player().getInventory().items.get(slot);
            if (isAcceptedItem(item.getItem())) {
                return item;
            }
        }
        return null;
    }

    public abstract boolean isAcceptedItem(Item item);

    public abstract boolean compare(BlockState state);

    public boolean isSolid(CalculationContext context) {
        return false;
    }

    public boolean isPlaceable(CalculationContext context, int x, int y, int z, BlockState block) {
        return MovementHelper.canPlaceAgainst(context.bsi, x, y, z, block);
    }

    public boolean clutch(IBaritone baritone, MovementState state, BlockPos dest, MutableClutchResult result) {
        return ClutchHelper.blockClutch(baritone, state, dest, result, true);
    }

    public boolean hasClutched(IPlayerContext ctx, BetterBlockPos dest) {
        return ctx.player().getBoundingBox().intersects(Vec3.atLowerCornerOf(dest), Vec3.atLowerCornerWithOffset(dest, 1, 1, 1));
    }

    public boolean isFinished(IPlayerContext ctx, MovementState state, MutableClutchResult result) {
        return true;
    }

    public float getFallDamage(int fallDamage) {
        return 0f;
    }

    public Pair<Double, Double> getCost(double distance, double endBlockHeight, double velocity) {
        return ActionCosts.distanceToTicks(distance, endBlockHeight, costMultiplier, velocity);
    }

    public double getAdditionalCost() {
        return 0d;
    }

    public boolean slowsOnTopBlock() {
        return true;
    }

    public boolean topBlockPriority() {
        return true;
    }
}
