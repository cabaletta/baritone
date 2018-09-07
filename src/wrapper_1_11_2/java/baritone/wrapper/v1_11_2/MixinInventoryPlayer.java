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

package baritone.wrapper.v1_11_2;

import baritone.wrapper.IInventoryPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.*;

import java.util.List;

/**
 * @author Brady
 * @since 9/7/2018
 */
@Implements(@Interface(iface = IInventoryPlayer.class, prefix = "wrapper$"))
@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer implements IInventoryPlayer {

    @Shadow @Final public NonNullList<ItemStack> mainInventory;

    @Intrinsic
    public List<ItemStack> wrapper$getMainInventory() {
        return this.mainInventory;
    }
}
