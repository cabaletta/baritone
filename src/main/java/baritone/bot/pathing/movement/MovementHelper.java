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

import baritone.bot.Baritone;
import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.MovementState.MovementTarget;
import baritone.bot.pathing.movement.movements.MovementDescend;
import baritone.bot.pathing.movement.movements.MovementFall;
import baritone.bot.utils.*;
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
            Item.getItemFromBlock(Blocks.COBBLESTONE),
            Item.getItemFromBlock(Blocks.NETHERRACK)
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
        if (BlockStateInterface.isFlowing(state) || BlockStateInterface.isLiquid(pos.up())) {
            return false; // Don't walk through flowing liquids
        }
        return block.isPassable(mc.world, pos);
    }

    static boolean avoidWalkingInto(Block block) {
        return BlockStateInterface.isLava(block)
                || block instanceof BlockCactus
                || block instanceof BlockFire
                || block instanceof BlockEndPortal
                || block instanceof BlockWeb
                || block instanceof BlockMagma;
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
        if (block.equals(Blocks.MAGMA)) {
            return false;
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
            if (!Baritone.settings().allowBreak) {
                return COST_INF;
            }
            //if (!Baritone.allowBreakOrPlace) {
            //    return COST_INF;
            //}
            double m = Blocks.CRAFTING_TABLE.equals(block) ? 10 : 1;
            return m / ts.getStrVsBlock(state, position);
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

    static boolean throwaway(boolean select) {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            if (ACCEPTABLE_THROWAWAY_ITEMS.contains(item.getItem())) {
                if (select) {
                    p.inventory.currentItem = i;
                }
                return true;
            }
        }
        return false;
    }

    static MovementState moveTowards(MovementState state, BlockPos pos) {
        return state.setTarget(new MovementTarget(new Rotation(Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                Utils.getBlockPosCenter(pos),
                new Rotation(mc.player.rotationYaw, mc.player.rotationPitch)).getFirst(), mc.player.rotationPitch))
        ).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
    }

    static Movement generateMovementFallOrDescend(BlockPos pos, EnumFacing direction, CalculationContext calcContext) {
        BlockPos dest = pos.offset(direction);
        // A
        //SA
        // B
        // B
        // C
        // D
        //if S is where you start, both of B need to be air for a movementfall
        //A is plausibly breakable by either descend or fall
        //C, D, etc determine the length of the fall
        for (int i = 1; i < 3; i++) {
            if (!canWalkThrough(dest.down(i))) {
                //if any of these two (B in the diagram) aren't air
                //have to do a descend, because fall is impossible

                //this doesn't guarantee descend is possible, it just guarantees fall is impossible
                return new MovementDescend(pos, dest.down()); // standard move out by 1 and descend by 1
            }
        }
        // we're clear for a fall 2
        // let's see how far we can fall
        for (int fallHeight = 3; true; fallHeight++) {
            BlockPos onto = dest.down(fallHeight);
            if (onto.getY() < 0) {
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
                if (calcContext.hasWaterBucket() || fallHeight <= 4) {
                    // fallHeight = 4 means onto.up() is 3 blocks down, which is the max
                    return new MovementFall(pos, onto.up());
                } else {
                    return null;
                }
            }
            break;
        }
        return null;
    }
}
