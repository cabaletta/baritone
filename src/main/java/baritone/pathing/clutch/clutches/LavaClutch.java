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
import baritone.pathing.clutch.Clutch;
import baritone.pathing.clutch.ClutchUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;

public class LavaClutch extends Clutch {
    public static final LavaClutch INSTANCE = new LavaClutch();

    private LavaClutch() {}

    @Override
    public boolean isAcceptedItem(Item item) {
        return item.equals(Items.LAVA_BUCKET);
    }

    @Override
    public boolean compare(BlockState state) {
        return state.getFluidState().getType() instanceof LavaFluid;
    }

    @Override
    public boolean isPlaceable(CalculationContext context, int x, int y, int z, BlockState block) {
        return (!context.considerPotionEffects || context.getBaritone().getPlayerContext().player().hasEffect(MobEffects.FIRE_RESISTANCE)) &&
                super.isPlaceable(context, x, y, z, block);
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
