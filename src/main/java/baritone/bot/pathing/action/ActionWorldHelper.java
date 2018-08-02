package baritone.bot.pathing.action;

import baritone.bot.utils.BlockStateInterface;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface ActionWorldHelper {
    Block waterFlowing = Block.getBlockById(8);
    Block waterStill = Block.getBlockById(9);
    Block lavaFlowing = Block.getBlockById(10);
    Block lavaStill = Block.getBlockById(11);

    /**
     * Is this block water? Includes both still and flowing
     *
     * @param b
     * @return
     */
    static boolean isWater(Block b) {
        return waterFlowing.equals(b) || waterStill.equals(b);
    }

    static boolean isWater(BlockPos bp) {
        return isWater(BlockStateInterface.get(bp).getBlock());
    }

    static boolean isLiquid(Block b) {
        return b instanceof BlockLiquid;
        //return b != null && (waterFlowing.equals(b) || waterStill.equals(b) || lavaFlowing.equals(b) || lavaStill.equals(b));
    }

    static boolean isFlowing(BlockPos pos, IBlockState state) {
        Block b = state.getBlock();
        if (b instanceof BlockLiquid) {
            System.out.println("Need to fix get flow check!!!");
            //return BlockLiquid.getFlow(Minecraft.getMinecraft().world, pos, state) != -1000.0D;
        }
        return false;
    }

    static boolean isLava(Block b) {
        return lavaFlowing.equals(b) || lavaStill.equals(b);
    }

    static boolean isLiquid(BlockPos p) {
        return isLiquid(BlockStateInterface.get(p).getBlock());
    }

    static boolean avoidBreaking(BlockPos pos) {
        Block b = BlockStateInterface.get(pos).getBlock();
        Block below = BlockStateInterface.get(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ())).getBlock();
        return Block.getBlockFromName("minecraft:ice").equals(b)//ice becomes water, and water can mess up the path
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
    static boolean canWalkThrough(BlockPos pos) {
        IBlockState state = BlockStateInterface.get(pos);
        Block block = state.getBlock();
        if (block instanceof BlockLilyPad || block instanceof BlockFire) {//you can't actually walk through a lilypad from the side, and you shouldn't walk through fire
            return false;
        }
        if (isFlowing(pos, state)) {
            return false;//don't walk through flowing liquids
        }
        if (isLiquid(pos.up())) {
            return false;//you could drown
        }
        return block.isPassable(Minecraft.getMinecraft().world, pos);
    }

    static boolean avoidWalkingInto(BlockPos pos) {
        Block block = BlockStateInterface.get(pos).getBlock();
        if (isLava(block)) {
            return true;
        }
        if (block instanceof BlockCactus) {
            return true;
        }
        return block instanceof BlockFire;
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
            return isWater(pos.up());//you can only walk on water if there is water above it
        }
        return state.isBlockNormalCube() && !isLava(block);
    }
}
