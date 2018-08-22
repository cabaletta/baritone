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

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.utils.Helper;
import baritone.utils.ToolSet;
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
    private final boolean hasThrowaway;
    private final boolean canSprint;
    private final double placeBlockCost;
    private final boolean allowBreak;
    private final int maxFallHeightNoWater;
    private final int maxFallHeightBucket;

    public CalculationContext() {
        this(new ToolSet());
    }

    public CalculationContext(ToolSet toolSet) {
        player().setSprinting(true);
        this.toolSet = toolSet;
        this.hasThrowaway = Baritone.settings().allowPlace.get() && MovementHelper.throwaway(false);
        this.hasWaterBucket = Baritone.settings().allowWaterBucketFall.get() && InventoryPlayer.isHotbar(player().inventory.getSlotFor(STACK_BUCKET_WATER)) && !world().provider.isNether();
        this.canSprint = Baritone.settings().allowSprint.get() && player().getFoodStats().getFoodLevel() > 6;
        this.placeBlockCost = Baritone.settings().blockPlacementPenalty.get();
        this.allowBreak = Baritone.settings().allowBreak.get();
        this.maxFallHeightNoWater = Baritone.settings().maxFallHeightNoWater.get();
        this.maxFallHeightBucket = Baritone.settings().maxFallHeightBucket.get();
        // why cache these things here, why not let the movements just get directly from settings?
        // because if some movements are calculated one way and others are calculated another way,
        // then you get a wildly inconsistent path that isn't optimal for either scenario.
    }

    public ToolSet getToolSet() {
        return this.toolSet;
    }

    public boolean hasWaterBucket() {
        return hasWaterBucket;
    }

    public boolean hasThrowaway() {
        return hasThrowaway;
    }

    public boolean canSprint() {
        return canSprint;
    }

    public double placeBlockCost() {
        return placeBlockCost;
    }

    public boolean allowBreak() {
        return allowBreak;
    }

    public int maxFallHeightNoWater() {
        return maxFallHeightNoWater;
    }

    public int maxFallHeightBucket() {
        return maxFallHeightBucket;
    }

}
