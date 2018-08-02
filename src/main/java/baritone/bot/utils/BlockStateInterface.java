package baritone.bot.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class BlockStateInterface {
    public static IBlockState get(BlockPos pos) { // wrappers for future chunk caching capability
        return Minecraft.getMinecraft().world.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return get(pos).getBlock();
    }
}
