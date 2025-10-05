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

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.clutch.Clutch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SlimeClutch extends Clutch {
    public static final SlimeClutch INSTANCE = new SlimeClutch();

    private boolean onGround = false;

    private SlimeClutch() {}

    @Override
    public boolean isAcceptedItem(Item item) {
        return item.equals(Items.SLIME_BLOCK);
    }

    @Override
    public boolean compare(BlockState state) {
        return state.is(Blocks.SLIME_BLOCK);
    }

    @Override
    public boolean isSolid(CalculationContext context) {
        return true;
    }

    @Override
    public boolean isFinished(IPlayerContext ctx, MovementState state, MutableClutchResult result) {
        state.setInput(Input.SNEAK, false);
        state.setInput(Input.JUMP, true);
        if (onGround) {
            return true;
        }
        onGround = ctx.player().isOnGround();
        return false;
    }

    @Override
    public double getAdditionalCost() {
        return 13.0182684d;
    }

    @Override
    public boolean hasClutched(IPlayerContext ctx, BetterBlockPos dest, BlockState destState) {
        return ctx.player().getBoundingBox().intersects(dest.x, dest.y, dest.z, dest.x + 1, dest.y + 2, dest.z + 1);
    }
}
