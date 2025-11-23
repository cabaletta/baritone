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

import baritone.pathing.clutch.Clutch;
import baritone.pathing.movement.CalculationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TwistingVineClutch extends Clutch {
    public static final TwistingVineClutch INSTANCE = new TwistingVineClutch();

    private TwistingVineClutch() {}

    @Override
    public boolean isAcceptedItem(Item item) {
        return item.equals(Items.TWISTING_VINES);
    }

    @Override
    public boolean compare(Level world, BlockPos pos, BlockState state) {
        return state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT);
    }

    @Override
    public boolean isSolid(CalculationContext context) {
        return false;
    }
}
