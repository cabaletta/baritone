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
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ScaffoldingClutch extends Clutch {
    public static final ScaffoldingClutch INSTANCE = new ScaffoldingClutch();

    private ScaffoldingClutch() {
        super(1.5d);

    }
    @Override
    public boolean isAcceptedItem(Item item) {
        return  item.equals(Items.SCAFFOLDING);
    }

    @Override
    public boolean compare(BlockState state) {
        return state.is(Blocks.SCAFFOLDING);
    }

    @Override
    public boolean hasClutched(IPlayerContext ctx, BetterBlockPos dest) {
        return super.hasClutched(ctx, dest.above());
    }

    @Override
    public boolean isFinished(IPlayerContext ctx, MovementState state, MutableClutchResult result) {
        state.setInput(Input.SNEAK, true);
        return ctx.world().getBlockState(ctx.playerFeet()).is(Blocks.SCAFFOLDING);
    }
}
