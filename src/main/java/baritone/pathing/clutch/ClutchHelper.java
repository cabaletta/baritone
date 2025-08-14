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
import baritone.api.utils.input.Input;
import baritone.pathing.clutch.clutches.*;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.pathing.MutableClutchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface ClutchHelper {
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
    };
    ItemStack STACK_EMPTY_BUCKET = new ItemStack(Items.BUCKET);

    static boolean blockClutch(IBaritone baritone, MovementState state, BlockPos dest, MutableClutchResult result, boolean allowDown) {
        if (MovementHelper.attemptToPlaceABlock(state, baritone, dest, allowDown, true, false, result.item.getItem()) == MovementHelper.PlaceResult.READY_TO_PLACE) {
            state.setInput(Input.CLICK_RIGHT, true);
            return true;
        } else {
            return false;
        }
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
