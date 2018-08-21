/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.bot.Baritone;
import baritone.bot.event.events.ItemSlotEvent;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Brady
 * @since 8/20/2018
 */
@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer {

    @Redirect(
            method = "getDestroySpeed",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/entity/player/InventoryPlayer.currentItem:I"
            )
    )
    private int getDestroySpeed$getCurrentItem(InventoryPlayer inventory) {
        ItemSlotEvent event = new ItemSlotEvent(inventory.currentItem);
        Baritone.INSTANCE.getGameEventHandler().onQueryItemSlotForBlocks(event);
        return event.getSlot();
    }

    @Redirect(
            method = "canHarvestBlock",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "net/minecraft/entity/player/InventoryPlayer.currentItem:I"
            )
    )
    private int canHarvestBlock$getCurrentItem(InventoryPlayer inventory) {
        ItemSlotEvent event = new ItemSlotEvent(inventory.currentItem);
        Baritone.INSTANCE.getGameEventHandler().onQueryItemSlotForBlocks(event);
        return event.getSlot();
    }
}
