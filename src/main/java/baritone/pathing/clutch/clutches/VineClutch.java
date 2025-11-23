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

import baritone.api.IBaritone;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.pathing.clutch.ClutchUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.clutch.Clutch;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

public class VineClutch extends Clutch {
    public static final VineClutch INSTANCE = new VineClutch();

    private VineClutch() {}

    @Override
    public boolean isAcceptedItem(Item item) {
        return item.equals(Items.VINE);
    }

    @Override
    public boolean compare(Level world, BlockPos pos, BlockState state) {
        return state.is(Blocks.VINE);
    }

    @Override
    public boolean isSolid(CalculationContext context) {
        return false;
    }

    // Had to yoink this out of VineBlock since it was private
    private boolean canSupportAtFace(BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) {
            return false;
        } else {
            if (VineBlock.isAcceptableNeighbour(level, pos, direction)) {
                return true;
            } else if (direction.getAxis() == Direction.Axis.Y) {
                return false;
            } else {
                // Will never use this
                BlockState lv3 = level.getBlockState(pos.above());
                return lv3.is(Blocks.VINE) && lv3.getValue(VineBlock.PROPERTY_BY_DIRECTION.get(direction));
            }
        }
    }

    @Override
    public boolean isPlaceable(CalculationContext context, int x, int y, int z, BlockState block) {
        return super.isPlaceable(context, x, y, z, block) &&
                (canSupportAtFace(context.bsi.access, new BetterBlockPos(x - 1, y + 1, z), Direction.WEST) ||
                        canSupportAtFace(context.bsi.access, new BetterBlockPos(x + 1, y + 1, z), Direction.EAST) ||
                        canSupportAtFace(context.bsi.access, new BetterBlockPos(x, y + 1, z - 1), Direction.NORTH) ||
                        canSupportAtFace(context.bsi.access, new BetterBlockPos(x, y + 1, z + 1), Direction.SOUTH));
    }

    @Override
    public boolean hasClutched(IPlayerContext ctx, BetterBlockPos dest, BlockState destState) {
        return ctx.player().getBoundingBox().intersects(dest.x, dest.y, dest.z, dest.x + 1, dest.y + 1, dest.z + 1);
    }
}
