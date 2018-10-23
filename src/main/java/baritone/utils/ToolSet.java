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

package baritone.utils;

import baritone.Baritone;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author Avery, Brady, leijurv
 */
public class ToolSet implements Helper {
    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private final Map<Block, Double> breakStrengthCache;

    /**
     * My buddy leijurv owned me so we have this to not create a new lambda instance.
     */
    private final Function<Block, Double> backendCalculation;

    public ToolSet() {
        breakStrengthCache = new HashMap<>();

        if (Baritone.settings().considerPotionEffects.get()) {
            double amplifier = potionAmplifier();
            Function<Double, Double> amplify = x -> amplifier * x;
            backendCalculation = amplify.compose(this::getBestDestructionTime);
        } else {
            backendCalculation = this::getBestDestructionTime;
        }
    }

    /**
     * Using the best tool on the hotbar, how long would it take to mine this block
     *
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    public double getStrVsBlock(IBlockState state) {
        return breakStrengthCache.computeIfAbsent(state.getBlock(), backendCalculation);
    }

    /**
     * Evaluate the material cost of a possible tool. The priority matches the
     * listed order in the Item.ToolMaterial enum.
     *
     * @param itemStack a possibly empty ItemStack
     * @return values range from -1 to 4
     */
    private int getMaterialCost(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemTool) {
            ItemTool tool = (ItemTool) itemStack.getItem();
            return ToolMaterial.valueOf(tool.getToolMaterialName()).ordinal();
        } else {
            return -1;
        }
    }

    /**
     * Calculate which tool on the hotbar is best for mining
     *
     * @param b the blockstate to be mined
     * @return A byte containing the index in the tools array that worked best
     */
    public byte getBestSlot(Block b) {
        byte best = 0;
        double value = Double.NEGATIVE_INFINITY;
        int materialCost = Integer.MIN_VALUE;
        IBlockState blockState = b.getDefaultState();
        for (byte i = 0; i < 9; i++) {
            ItemStack itemStack = player().inventory.getStackInSlot(i);
            double v = calculateStrVsBlock(itemStack, blockState);
            if (v > value) {
                value = v;
                best = i;
                materialCost = getMaterialCost(itemStack);
            } else if (v == value) {
                int c = getMaterialCost(itemStack);
                if (c < materialCost) {
                    value = v;
                    best = i;
                    materialCost = c;
                }
            }
        }
        return best;
    }

    /**
     * Calculate how effectively a block can be destroyed
     *
     * @param b the blockstate to be mined
     * @return A double containing the destruction ticks with the best tool
     */
    private double getBestDestructionTime(Block b) {
        ItemStack stack = player().inventory.getStackInSlot(getBestSlot(b));
        return calculateStrVsBlock(stack, b.getDefaultState());
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used. A negative value is returned if the specified block is unbreakable.
     *
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    private double calculateStrVsBlock(ItemStack item, IBlockState state) {
        float hardness = state.getBlockHardness(null, null);
        if (hardness < 0) {
            return -1;
        }

        float speed = item.getDestroySpeed(state);
        if (speed > 1) {
            int effLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, item);
            if (effLevel > 0 && !item.isEmpty()) {
                speed += effLevel * effLevel + 1;
            }
        }

        speed /= hardness;
        if (state.getMaterial().isToolNotRequired() || (!item.isEmpty() && item.canHarvestBlock(state))) {
            speed /= 30;
        } else {
            speed /= 100;
        }
        return speed;
    }

    /**
     * Calculates any modifier to breaking time based on status effects.
     *
     * @return a double to scale block breaking speed.
     */
    private double potionAmplifier() {
        double speed = 1;
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
        return speed;
    }
}
