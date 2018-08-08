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

import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.movements.MovementDescend;
import baritone.bot.pathing.movement.movements.MovementFall;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

    List<Item> ACCEPTABLE_THROWAWAY_ITEMS = Arrays.asList(
            Item.getItemFromBlock(Blocks.DIRT),
            Item.getItemFromBlock(Blocks.COBBLESTONE)
    );

    static boolean avoidBreaking(BlockPos pos) {
        Block b = BlockStateInterface.getBlock(pos);
        BlockPos below = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        return Blocks.ICE.equals(b) // ice becomes water, and water can mess up the path
                || b instanceof BlockSilverfish
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()))//don't break anything touching liquid on any side
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ()))
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ()))
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1))
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1))
                || (!(b instanceof BlockLilyPad && BlockStateInterface.isWater(below)) && BlockStateInterface.isLiquid(below));//if it's a lilypad above water, it's ok to break, otherwise don't break if its liquid
    }

    /**
     * Can I walk through this block? e.g. air, saplings, torches, etc
     *
     * @param pos
     * @return
     */
    static boolean canWalkThrough(BlockPos pos) {
        IBlockState state = BlockStateInterface.get(pos);
        return canWalkThrough(pos, state);
    }

    static boolean canWalkThrough(BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockLilyPad
                || block instanceof BlockFire
                || block instanceof BlockTripWire) {//you can't actually walk through a lilypad from the side, and you shouldn't walk through fire
            return false;
        }
        if (BlockStateInterface.isFlowing(pos) || BlockStateInterface.isLiquid(pos.up())) {
            return false; // Don't walk through flowing liquids
        }
        return block.isPassable(mc.world, pos);
    }

    static boolean avoidWalkingInto(Block block) {
        return BlockStateInterface.isLava(block)
                || block instanceof BlockCactus
                || block instanceof BlockFire
                || block instanceof BlockEndPortal
                || block instanceof BlockWeb;
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * lava
     *
     * @return
     */
    static boolean canWalkOn(BlockPos pos, IBlockState state) {

        Block block = state.getBlock();
        if (block instanceof BlockLadder || block instanceof BlockVine) {
            return true;
        }
        if (block instanceof BlockAir) {
            return false;
        }
        if (BlockStateInterface.isWater(block)) {
            return BlockStateInterface.isWater(pos.up()); // You can only walk on water if there is water above it
        }
        return state.isBlockNormalCube() && !BlockStateInterface.isLava(block);
    }

    static boolean canWalkOn(BlockPos pos) {
        IBlockState state = BlockStateInterface.get(pos);
        return canWalkOn(pos, state);
    }

    static boolean canFall(BlockPos pos) {
        return BlockStateInterface.get(pos).getBlock() instanceof BlockFalling;
    }

    static double getMiningDurationTicks(ToolSet ts, BlockPos position) {
        IBlockState state = BlockStateInterface.get(position);
        Block block = state.getBlock();
        if (!block.equals(Blocks.AIR) && !canWalkThrough(position)) {
            if (avoidBreaking(position)) {
                return COST_INF;
            }
            //if (!Baritone.allowBreakOrPlace) {
            //    return COST_INF;
            //}
            double m = Blocks.CRAFTING_TABLE.equals(block) ? 10 : 1;
            return m / ts.getStrVsBlock(state, position) + BREAK_ONE_BLOCK_ADD;
        }
        return 0;
    }

    /**
     * The entity the player is currently looking at
     *
     * @return the entity object
     */
    static Optional<Entity> whatEntityAmILookingAt() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            return Optional.of(mc.objectMouseOver.entityHit);
        }
        return Optional.empty();
    }

    /**
     * AutoTool
     */
    static void switchToBestTool() {
        LookBehaviorUtils.getSelectedBlock().ifPresent(pos -> {
            IBlockState state = BlockStateInterface.get(pos);
            if (state.getBlock().equals(Blocks.AIR)) {
                return;
            }
            switchToBestToolFor(state);
        });
    }

    /**
     * AutoTool for a specific block
     *
     * @param b the blockstate to mine
     */
    static void switchToBestToolFor(IBlockState b) {
        switchToBestToolFor(b, new ToolSet());
    }

    /**
     * AutoTool for a specific block with precomputed ToolSet data
     *
     * @param b  the blockstate to mine
     * @param ts previously calculated ToolSet
     */
    static void switchToBestToolFor(IBlockState b, ToolSet ts) {
        mc.player.inventory.currentItem = ts.getBestSlot(b);
    }

    static boolean switchtothrowaway() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            if (ACCEPTABLE_THROWAWAY_ITEMS.contains(item.getItem())) {
                p.inventory.currentItem = i;
                return true;
            }
        }
        return false;
    }

    static Movement generateMovementFallOrDescend(BlockPos pos, EnumFacing direction) {
        BlockPos dest = pos.offset(direction);
        BlockPos destUp = dest.up();
        BlockPos destDown = dest.down();
        for (int i = 0; i < 4; i++) {
            if (!(BlockStateInterface.get(destUp.down(i)).getBlock() instanceof BlockAir)) {
                //if any of these four aren't air, that means that a fall N isn't possible
                //so try a movementdescend

                //if all four of them are air, a movementdescend isn't possible anyway
                return new MovementDescend(pos, destDown);
            }
        }
        System.out.println(dest + " descend distance!");
        // we're clear for a fall 2
        // let's see how far we can fall
        for (int fallHeight = 3; true; fallHeight++) {
            BlockPos onto = dest.down(fallHeight);
            if (onto.getY() <= 0) {
                break;
            }
            IBlockState ontoBlock = BlockStateInterface.get(onto);
            if (BlockStateInterface.isWater(ontoBlock.getBlock())) {
                return new MovementFall(pos, onto);
            }
            if (canWalkThrough(onto, ontoBlock)) {
                continue;
            }
            if (canWalkOn(onto, ontoBlock)) {
                return new MovementFall(pos, onto.up());
            }
            break;
        }
        return null;
    }

    static boolean hasthrowaway() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            if (ACCEPTABLE_THROWAWAY_ITEMS.contains(item.getItem())) {
                return true;
            }
        }
        return false;
    }
}
