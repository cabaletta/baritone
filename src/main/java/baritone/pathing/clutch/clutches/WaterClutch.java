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

package baritone.pathing.clutch.clutches;

import baritone.api.utils.IPlayerContext;
import baritone.pathing.clutch.ClutchUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.clutch.Clutch;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WaterClutch extends Clutch {
    private static final double WATER_HEIGHT = 0.887889;
    public static final WaterClutch INSTANCE = new WaterClutch();

    private WaterClutch() {}

    @Override
    public boolean isAcceptedItem(Item item) {
        return item.equals(Items.WATER_BUCKET) ||
                item.equals(Items.AXOLOTL_BUCKET) ||
                item.equals(Items.COD_BUCKET) ||
                item.equals(Items.TROPICAL_FISH_BUCKET) ||
                item.equals(Items.SALMON_BUCKET) ||
                item.equals(Items.TADPOLE_BUCKET);
    }

    @Override
    public boolean compare(Level world, BlockPos pos, BlockState state) {
        return state.getFluidState().getType() instanceof WaterFluid;
    }

    @Override
    public boolean isSolid(CalculationContext context) {
        return false;
    }

    @Override
    public boolean isPlaceable(CalculationContext context, int x, int y, int z, BlockState block) {
        VoxelShape shape = block.getCollisionShape(context.world, new BlockPos(x, y, z));
        return super.isPlaceable(context, x, y, z, block) &&
                (!(block.getBlock() instanceof SimpleWaterloggedBlock) ^ (shape.isEmpty() || shape.bounds().maxY < WATER_HEIGHT)) &&
                context.world.dimension() != Level.NETHER;
    }

    @Override
    public boolean isFinished(IPlayerContext ctx, MovementState state, MutableClutchResult result) {
        return ClutchUtils.bucketPickup(state, ctx.player().getInventory());
    }

    @Override
    public boolean topBlockPriority() {
        return false;
    }
}
