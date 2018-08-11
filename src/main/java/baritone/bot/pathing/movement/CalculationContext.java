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

package baritone.bot.pathing.movement;

import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * @author Brady
 * @since 8/7/2018 4:30 PM
 */
public class CalculationContext implements Helper {

    private static final ItemStack STACK_BUCKET_WATER = new ItemStack(Items.WATER_BUCKET);

    private final ToolSet toolSet;
    private final boolean hasWaterBucket;

    public CalculationContext() {
        this(new ToolSet());
    }

    public CalculationContext(ToolSet toolSet) {
        this.toolSet = toolSet;
        this.hasWaterBucket = InventoryPlayer.isHotbar(player().inventory.getSlotFor(STACK_BUCKET_WATER)) && !world().provider.isNether();
    }

    public ToolSet getToolSet() {
        return this.toolSet;
    }

    public boolean hasWaterBucket() {
        return hasWaterBucket;
    }
}
