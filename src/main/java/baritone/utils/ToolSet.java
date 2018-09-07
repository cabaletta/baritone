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

package baritone.utils;

import baritone.Baritone;
import baritone.wrapper.IItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author avecowa, Brady, leijurv
 */
public class ToolSet implements Helper {

    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private Map<Block, Double> breakStrengthCache = new HashMap<>();

    /**
     * Create a toolset from the current player's inventory (but don't calculate any hardness values just yet)
     */
    public ToolSet() {}

    /**
     * Calculate which tool on the hotbar is best for mining
     *
     * @param b the blockstate to be mined
     * @return a byte indicating the index in the tools array that worked best
     */
    public byte getBestSlot(IBlockState b) {
        byte best = 0;
        double value = -1;
        for (byte i = 0; i < 9; i++) {
            double v = calculateStrVsBlock(i, b);
            if (v > value || value == -1) {
                value = v;
                best = i;
            }
        }
        return best;
    }

    /**
     * Using the best tool on the hotbar, how long would it take to mine this block
     *
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    public double getStrVsBlock(IBlockState state) {
        return this.breakStrengthCache.computeIfAbsent(state.getBlock(), b -> calculateStrVsBlock(getBestSlot(state), state));
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used.
     *
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    private double calculateStrVsBlock(byte slot, IBlockState state) {
        // Calculate the slot with the best item
        ItemStack contents = player().inventory.getStackInSlot(slot);

        // In 1.10 null item stacks were a thing, no such thing as empty ones.
        if (contents == null)
            return 0;

        float blockHard = state.getBlockHardness(null, null);
        if (blockHard < 0) {
            return 0;
        }

        // noinspection ConstantConditions
        IItemStack wrapped = (IItemStack) (Object) contents;

        float speed = contents.getDestroySpeed(state);
        if (speed > 1) {
            int effLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, contents);
            if (effLevel > 0 && !wrapped.isEmpty()) {
                speed += effLevel * effLevel + 1;
            }
        }

        if (Baritone.settings().considerPotionEffects.get()) {
            if (player().isPotionActive(MobEffects.HASTE)) {
                speed *= 1 + (player().getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2;
            }
            if (player().isPotionActive(MobEffects.MINING_FATIGUE)) {
                switch (player().getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                    case 0:
                        speed *= 0.3;
                        break;
                    case 1:
                        speed *= 0.09;
                        break;
                    case 2:
                        speed *= 0.0027;
                        break;
                    default:
                        speed *= 0.00081;
                        break;
                }
            }
        }
        speed /= blockHard;
        if (state.getMaterial().isToolNotRequired() || (!wrapped.isEmpty() && contents.canHarvestBlock(state))) {
            return speed / 30;
        } else {
            return speed / 100;
        }
    }
}
