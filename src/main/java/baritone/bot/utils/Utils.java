package baritone.bot.utils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * @author Brady
 * @since 8/1/2018 12:56 AM
 */
public final class Utils {

    public static void ifConditionThen(Supplier<Boolean> condition, Runnable runnable) {
        if (condition.get()) {
            runnable.run();
        }
    }

    public static Tuple<Float, Float> calcRotationFromCoords(BlockPos orig, BlockPos dest) {
        double yaw = Math.atan2(orig.getX() - dest.getX(), -orig.getZ() + dest.getZ());
        double dist = Math.sqrt((orig.getX() - dest.getX()) * (orig.getX() - dest.getX()) + (-orig.getZ() + dest.getZ()) * (-orig.getZ() + dest.getZ()));
        double pitch = Math.atan2(orig.getY() - dest.getY(), dist);
        Tuple<Float, Float> rotation = new Tuple<>((float) (yaw * 180 / Math.PI),
                (float) (pitch * 180 / Math.PI));
        return rotation;
    }

    public static BlockPos calcCenterFromCoords(BlockPos orig, World world) {
        IBlockState b = world.getBlockState(orig);
        AxisAlignedBB bbox = b.getBoundingBox(world, orig);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        BlockPos centerPos = new BlockPos(orig.getX() + xDiff,
                orig.getY() + yDiff,
                orig.getZ() + zDiff);
        return centerPos;
    }
}
