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
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.clutch.clutches.*;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface ClutchUtils {
    // This list holds the order to try the clutches in. More convenient clutches should go further up.
    Clutch[] CLUTCHES = new Clutch[]{
            WaterClutch.INSTANCE,
            LavaClutch.INSTANCE,
            PowderedSnowClutch.INSTANCE,
            TwistingVineClutch.INSTANCE,
            SweetBerryClutch.INSTANCE,
            VineClutch.INSTANCE,
            LadderClutch.INSTANCE,
            ScaffoldingClutch.INSTANCE,
            SlimeClutch.INSTANCE,
            HayBaleClutch.INSTANCE,
            CobwebClutch.INSTANCE,
            BlockClutch.INSTANCE,
    };
    ItemStack STACK_EMPTY_BUCKET = new ItemStack(Items.BUCKET);

    static boolean isClutchBlock(Level world, BlockPos pos, BlockState state) {
        for (Clutch clutch : CLUTCHES) {
            if (clutch.compare(world, pos, state)) {
                return true;
            }
        }
        return false;
    }

    static boolean blockClutch(IBaritone baritone, MovementState state, BlockPos dest, MutableClutchResult result) {
        IPlayerContext ctx = baritone.getPlayerContext();
        state.setTarget(new MovementState.MovementTarget(ctx.playerRotations().withPitch(90), true));
        double dist = ctx.playerHead().y() - (dest.getY() + 1); // Saying that all blocks below have a height of 1, but it doesn't matter
        if (dist > 0 && dist <= ctx.playerController().getBlockReachDistance()) {
            ((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(true, dest.getX(), dest.getY(), dest.getZ(), result.item.getItem());
            state.setInput(Input.CLICK_RIGHT, true);
            return true;
        }
        return false;
    }

    static boolean bucketPickup(MovementState state, Inventory inventory) {
        int slot = inventory.findSlotMatchingItem(STACK_EMPTY_BUCKET);
        if (Inventory.isHotbarSlot(slot)) {
            inventory.selected = slot;
            state.setInput(Input.CLICK_RIGHT, true);
        }
        return slot == -1;
    }
}
