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
import baritone.pathing.clutch.Clutch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PowderedSnowClutch extends Clutch {
    public static final PowderedSnowClutch INSTANCE = new PowderedSnowClutch();

    private PowderedSnowClutch() {}

    @Override
    public boolean isAcceptedItem(Item item) {
        return item.equals(Items.POWDER_SNOW_BUCKET);
    }

    @Override
    public boolean compare(Level world, BlockPos pos, BlockState state) {
        return state.is(Blocks.POWDER_SNOW);
    }

    @Override
    public boolean isSolid(CalculationContext context) {
        return context.getBaritone().getPlayerContext().player().getInventory().getArmor(3).is(Items.LEATHER_BOOTS);
    }

    @Override
    public boolean isFinished(IPlayerContext ctx, MovementState state, MutableClutchResult result) {
        return ClutchUtils.bucketPickup(state, ctx.player().getInventory());
    }

    @Override
    public boolean slowsOnTopBlock() {
        return false;
    }
}