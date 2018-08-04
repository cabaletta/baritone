package baritone.bot.pathing.movement;

import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

    Block waterFlowing = Blocks.FLOWING_WATER;
    Block waterStill = Blocks.WATER;
    Block lavaFlowing = Blocks.FLOWING_LAVA;
    Block lavaStill = Blocks.LAVA;

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     *
     * @param b The block
     * @return Whether or not the block is water
     */
    static boolean isWater(Block b) {
        return waterFlowing.equals(b) || waterStill.equals(b);
    }

    /**
     * Returns whether or not the block at the specified pos is
     * water, regardless of whether or not it is flowing.
     *
     * @param bp The block pos
     * @return Whether or not the block is water
     */
    static boolean isWater(BlockPos bp) {
        return isWater(BlockStateInterface.get(bp).getBlock());
    }

    /**
     * Returns whether or not the specified block is any sort of liquid.
     *
     * @param b The block
     * @return Whether or not the block is a liquid
     */
    static boolean isLiquid(Block b) {
        return b instanceof BlockLiquid;
    }

    static boolean isLiquid(BlockPos p) {
        return isLiquid(BlockStateInterface.get(p).getBlock());
    }

    static boolean isFlowing(IBlockState state) {
        return state.getBlock() instanceof BlockLiquid
                && state.getPropertyKeys().contains(BlockLiquid.LEVEL)
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }

    static boolean isLava(Block b) {
        return lavaFlowing.equals(b) || lavaStill.equals(b);
    }

    static boolean avoidBreaking(BlockPos pos) {
        Block b = BlockStateInterface.get(pos).getBlock();
        Block below = BlockStateInterface.get(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ())).getBlock();
        return Blocks.ICE.equals(b) // ice becomes water, and water can mess up the path
                || b instanceof BlockSilverfish
                || isLiquid(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()))//don't break anything touching liquid on any side
                || isLiquid(new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ()))
                || isLiquid(new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ()))
                || isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1))
                || isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1))
                || (!(b instanceof BlockLilyPad && isWater(below)) && isLiquid(below));//if it's a lilypad above water, it's ok to break, otherwise don't break if its liquid
    }

    /**
     * Can I walk through this block? e.g. air, saplings, torches, etc
     *
     * @param pos
     * @return
     */
    static boolean canWalkThrough(BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockLilyPad
                || block instanceof BlockFire
                || block instanceof BlockTripWire) {//you can't actually walk through a lilypad from the side, and you shouldn't walk through fire
            return false;
        }
        if (isFlowing(state) || isLiquid(pos.up())) {
            return false; // Don't walk through flowing liquids
        }
        return block.isPassable(Minecraft.getMinecraft().world, pos);
    }

    static boolean avoidWalkingInto(Block block) {
        return isLava(block)
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
     * @param pos
     * @return
     */
    static boolean canWalkOn(BlockPos pos) {
        IBlockState state = BlockStateInterface.get(pos);
        Block block = state.getBlock();
        if (block instanceof BlockLadder || block instanceof BlockVine) {
            return true;
        }
        if (isWater(block)) {
            return isWater(pos.up()); // You can only walk on water if there is water above it
        }
        return state.isBlockNormalCube() && !isLava(block);
    }

    static boolean canFall(BlockPos pos) {
        return BlockStateInterface.get(pos).getBlock() instanceof BlockFalling;
    }

    static double getMiningDurationTicks(ToolSet ts, IBlockState block, BlockPos position) {
        if (!block.equals(Blocks.AIR) && !canWalkThrough(position, block)) {
            if (avoidBreaking(position)) {
                return COST_INF;
            }
            //if (!Baritone.allowBreakOrPlace) {
            //    return COST_INF;
            //}
            double m = Blocks.CRAFTING_TABLE.equals(block) ? 10 : 1;
            return m / ts.getStrVsBlock(block, position) + BREAK_ONE_BLOCK_ADD;
        }
        return 0;
    }

    /**
     * The currently highlighted block.
     * Updated once a tick by Minecraft.
     *
     * @return the position of the highlighted block, or null if no block is highlighted
     */
    static BlockPos whatAmILookingAt() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            return mc.objectMouseOver.getBlockPos();
        }
        return null;
    }

    /**
     * The entity the player is currently looking at
     *
     * @return the entity object, or null if the player isn't looking at an entity
     */
    static Entity whatEntityAmILookingAt() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            return mc.objectMouseOver.entityHit;
        }
        return null;
    }

    /**
     * AutoTool
     */
    static void switchToBestTool() {
        BlockPos pos = whatAmILookingAt();
        if (pos == null) {
            return;
        }
        IBlockState state = BlockStateInterface.get(pos);
        if (state.getBlock().equals(Blocks.AIR)) {
            return;
        }
        switchToBestToolFor(state);
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
        Minecraft.getMinecraft().player.inventory.currentItem = ts.getBestSlot(b);
    }

}
