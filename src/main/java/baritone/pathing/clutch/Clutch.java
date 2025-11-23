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
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class Clutch {
    private final double speedMultiplier;

    protected Clutch(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    protected Clutch() {
        this(1.0);
    }

    public final ItemStack getClutchingItem(CalculationContext context) { // TODO We could return the slot instead of the item
        NonNullList<ItemStack> items = context.baritone.getPlayerContext().player().getInventory().items;
        for (int slot = 0; slot < (context.allowInventory ? 36 : 9); slot++) {
            ItemStack item = items.get(slot);
            if (isAcceptedItem(item.getItem())) {
                return item;
            }
        }
        return null;
    }

    public abstract boolean isAcceptedItem(Item item);

    public abstract boolean compare(Level world, BlockPos pos, BlockState state);

    public abstract boolean isSolid(CalculationContext context);

    public boolean isPlaceable(CalculationContext context, int x, int y, int z, BlockState block) {
        return MovementHelper.canPlaceAgainst(context.bsi, x, y, z, block);
    }

    public boolean clutch(IBaritone baritone, MovementState state, BlockPos dest, MutableClutchResult result) {
        return ClutchUtils.blockClutch(baritone, state, dest, result);
    }

    public boolean hasClutched(IPlayerContext ctx, BetterBlockPos dest, BlockState destState) {
        VoxelShape shape = destState.getCollisionShape(ctx.world(), dest);
        if (shape.isEmpty()) {
            return ctx.player().getBoundingBox().intersects(dest.x, dest.y, dest.z, dest.x + 1, dest.y + 1, dest.z + 1);
        } else {
            return ctx.player().getBoundingBox().intersects(
                    dest.x + shape.bounds().minX, dest.y + shape.bounds().minY, dest.z + shape.bounds().minZ,
                    dest.x + shape.bounds().maxX, dest.y + shape.bounds().maxY, dest.z + shape.bounds().maxZ);
        }
    }

    public boolean isFinished(IPlayerContext ctx, MovementState state, MutableClutchResult result) {
        return true;
    }

    public float getFallDamage(float fallDamage) {
        return 0f;
    }

    public Pair<Double, Double> getCost(double distance, double endBlockHeight, double velocity) {
        return ActionCosts.distanceToTicks(distance, endBlockHeight, speedMultiplier, velocity);
    }

    public double getAdditionalCost() {
        return 0.0;
    }

    public boolean slowsOnTopBlock() {
        return true;
    }

    public boolean topBlockPriority() {
        return true;
    }
}
