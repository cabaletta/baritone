package baritone.bot.pathing.util;

import baritone.bot.utils.Helper;
import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 8/4/2018 2:01 AM
 */
public interface IBlockTypeAccess extends Helper {

    PathingBlockType getBlockType(int x, int y, int z);

    default PathingBlockType getBlockType(BlockPos pos) {
        return getBlockType(pos.getX(), pos.getY(), pos.getZ());
    }
}
