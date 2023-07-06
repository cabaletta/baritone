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
import baritone.PerformanceCritical;
import baritone.api.utils.IPlayerContext;
import baritone.utils.accessor.IItemTool;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import java.util.function.ToDoubleFunction;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author Avery, Brady, leijurv
 */
public final class ToolSet {

    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private final Cache breakStrengthCache;

    /**
     * My buddy leijurv owned me so we have this to not create a new lambda instance.
     */
    private final ToDoubleFunction<IBlockState> backendCalculation;

    private final IPlayerContext ctx;

    public ToolSet(IPlayerContext ctx) {
        this.ctx = ctx;
        this.breakStrengthCache = new Cache();

        if (Baritone.settings().considerPotionEffects.value) {
            double amplifier = this.potionAmplifier();
            this.backendCalculation = block -> amplifier * this.getBestDestructionSpeed(block);
        } else {
            this.backendCalculation = this::getBestDestructionSpeed;
        }
    }

    /**
     * Using the best tool on the hotbar, how fast we can mine this block
     *
     * @param state the state to be mined
     * @return the speed of how fast we'll mine it. 1/(time in ticks)
     */
    @PerformanceCritical
    public double getStrVsBlock(IBlockState state) {
        return this.breakStrengthCache.computeIfAbsent(state, this.backendCalculation);
    }

    /**
     * Calculate how effectively a block can be destroyed
     *
     * @param state the block state to be mined
     * @return A double containing the destruction speed with the best tool
     */
    private double getBestDestructionSpeed(IBlockState state) {
        final ItemStack stack = ctx.player().inventory.getStackInSlot(this.getBestSlot(state, false, true));
        return calculateSpeedVsBlock(stack, state) * avoidanceMultiplier(state.getBlock());
    }

    /**
     * Evaluate the material cost of a possible tool. The priority matches the
     * harvest level order; there is a chance for multiple at the same with modded tools
     * but in that case we don't really care.
     *
     * @param itemStack a possibly empty ItemStack
     * @return The tool's harvest level, or {@code -1} if the stack isn't a tool
     */
    private static int getMaterialCost(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemTool) {
            ItemTool tool = (ItemTool) itemStack.getItem();
            return ((IItemTool) tool).getHarvestLevel();
        } else {
            return -1;
        }
    }

    /**
     * Calculate which tool on the hotbar is best for mining, depending on an override setting,
     * related to auto tool movement cost, it will either return current selected slot, or the best slot.
     *
     * @param state the blockstate to be mined
     * @param preferSilkTouch whether to prefer silk touch tools
     * @param pathingCalculation whether the call to this method is for pathing calculation
     * @return An int containing the index in the tools array that worked best
     */
    public int getBestSlot(IBlockState state, boolean preferSilkTouch, boolean pathingCalculation) {

        /*
        If we actually want know what efficiency our held item has instead of the best one
        possible, this lets us make pathing depend on the actual tool to be used (if auto tool is disabled)
        */
        if (!Baritone.settings().autoTool.value && pathingCalculation) {
            return ctx.player().inventory.currentItem;
        }

        int best = 0;
        double highestSpeed = Double.NEGATIVE_INFINITY;
        int lowestCost = Integer.MIN_VALUE;
        boolean bestSilkTouch = false;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = ctx.player().inventory.getStackInSlot(i);
            if (!Baritone.settings().useSwordToMine.value && itemStack.getItem() instanceof ItemSword) {
                continue;
            }

            if (Baritone.settings().itemSaver.value && (itemStack.getItemDamage() + Baritone.settings().itemSaverThreshold.value) >= itemStack.getMaxDamage() && itemStack.getMaxDamage() > 1) {
                continue;
            }
            double speed = calculateSpeedVsBlock(itemStack, state);
            boolean silkTouch = hasSilkTouch(itemStack);
            if (speed > highestSpeed) {
                highestSpeed = speed;
                best = i;
                lowestCost = getMaterialCost(itemStack);
                bestSilkTouch = silkTouch;
            } else if (speed == highestSpeed) {
                int cost = getMaterialCost(itemStack);
                if ((cost < lowestCost && (silkTouch || !bestSilkTouch)) ||
                        (preferSilkTouch && !bestSilkTouch && silkTouch)) {
                    highestSpeed = speed;
                    best = i;
                    lowestCost = cost;
                    bestSilkTouch = silkTouch;
                }
            }
        }
        return best;
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used. A negative value is returned if the specified block is unbreakable.
     *
     * @param item  the item to mine it with
     * @param state the blockstate to be mined
     * @return the speed of how fast we'll mine it. 1/(time in ticks)
     */
    public static double calculateSpeedVsBlock(ItemStack item, IBlockState state) {
        float hardness;
        try {
            // noinspection DataFlowIssue
            hardness = state.getBlockHardness(null, null);
        } catch (NullPointerException ignored) {
            // Just catch the exception and act as if the block is unbreakable. Even in situations where we could
            // reasonably determine the hardness by passing the correct world/position (not via 'getStrVsBlock' during
            // performance critical cost calculation), it's not worth it for the sake of consistency.
            return -1;
        }
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
            return speed / 30;
        } else {
            return speed / 100;
        }
    }

    private static double avoidanceMultiplier(Block block) {
        return Baritone.settings().blocksToAvoidBreaking.value.contains(block)
                ? Baritone.settings().avoidBreakingMultiplier.value : 1;
    }

    private static boolean hasSilkTouch(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;
    }

    /**
     * Calculates any modifier to breaking time based on status effects.
     *
     * @return a double to scale block breaking speed.
     */
    private double potionAmplifier() {
        double speed = 1;
        if (ctx.player().isPotionActive(MobEffects.HASTE)) {
            speed *= 1 + (ctx.player().getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2;
        }
        if (ctx.player().isPotionActive(MobEffects.MINING_FATIGUE)) {
            switch (ctx.player().getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    speed *= 0.3;
                    break;
                case 1:
                    speed *= 0.09;
                    break;
                case 2:
                    speed *= 0.0027; // you might think that 0.09*0.3 = 0.027 so that should be next, that would make too much sense. it's 0.0027.
                    break;
                default:
                    speed *= 0.00081;
                    break;
            }
        }
        return speed;
    }

    /*
     * Copyright (C) 2002-2022 Sebastiano Vigna
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    private static final class Cache extends Reference2DoubleOpenHashMap<IBlockState> {

        public double computeIfAbsent(final IBlockState key, final ToDoubleFunction<IBlockState> mappingFunction) {
            int pos = this.find(key);
            if (pos >= 0) {
                return this.value[pos];
            } else {
                double newValue = mappingFunction.applyAsDouble(key);
                this.insert(-pos - 1, key, newValue);
                return newValue;
            }
        }

        private int find(final IBlockState k) {
            if (((k) == (null))) return containsNullKey ? n : -(n + 1);
            Object curr;
            final Object[] key = this.key;
            int pos;
            // The starting point.
            if (((curr = key[pos = (HashCommon.mix(System.identityHashCode(k))) & mask]) == (null))) return -(pos + 1);
            if (((k) == (curr))) return pos;
            // There's always an unused entry.
            while (true) {
                if (((curr = key[pos = (pos + 1) & mask]) == (null))) return -(pos + 1);
                if (((k) == (curr))) return pos;
            }
        }

        private void insert(int pos, IBlockState k, double v) {
            if (pos == this.n) {
                this.containsNullKey = true;
            }
            final Object[] key = this.key;
            key[pos] = k;
            this.value[pos] = v;
            if (this.size++ >= this.maxFill) {
                this.rehash(HashCommon.arraySize(this.size + 1, this.f));
            }
        }
    }
}
