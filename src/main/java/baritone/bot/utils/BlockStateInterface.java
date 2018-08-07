package baritone.bot.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class BlockStateInterface {
    public static IBlockState get(BlockPos pos) { // wrappers for future chunk caching capability
        return Minecraft.getMinecraft().world.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return get(pos).getBlock();
    }

    public static final Block waterFlowing = Blocks.FLOWING_WATER;
    public static final Block waterStill = Blocks.WATER;
    public static final Block lavaFlowing = Blocks.FLOWING_LAVA;
    public static final Block lavaStill = Blocks.LAVA;

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     *
     * @param b The block
     * @return Whether or not the block is water
     */
    public static boolean isWater(Block b) {
        return waterFlowing.equals(b) || waterStill.equals(b);
    }

    /**
     * Returns whether or not the block at the specified pos is
     * water, regardless of whether or not it is flowing.
     *
     * @param bp The block pos
     * @return Whether or not the block is water
     */
    public static boolean isWater(BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(bp));
    }

    public static boolean isLava(Block b) {
        return lavaFlowing.equals(b) || lavaStill.equals(b);
    }

    /**
     * Returns whether or not the specified pos has a liquid
     *
     * @param p The pos
     * @return Whether or not the block is a liquid
     */
    public static boolean isLiquid(BlockPos p) {
        return BlockStateInterface.getBlock(p) instanceof BlockLiquid;
    }

    public static boolean isFlowing(BlockPos pos) {
        // Will be IFluidState in 1.13
        IBlockState state = BlockStateInterface.get(pos);
        return state.getBlock() instanceof BlockLiquid
                && state.getPropertyKeys().contains(BlockLiquid.LEVEL)
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }

    public static boolean isAir(BlockPos pos) {
        return BlockStateInterface.getBlock(pos).equals(Blocks.AIR);
    }

    public static boolean isAir(IBlockState state) {
        return state.getBlock().equals(Blocks.AIR);
    }

    static boolean canFall(BlockPos pos) {
        return BlockStateInterface.get(pos).getBlock() instanceof BlockFalling;
    }


}
