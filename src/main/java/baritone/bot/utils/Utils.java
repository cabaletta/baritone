package baritone.bot.utils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 8/1/2018 12:56 AM
 */
public final class Utils {

    public static Tuple<Float, Float> calcRotationFromCoords(BlockPos orig, BlockPos dest) {
        return calcRotationFromVec3d(vec3dFromBlockPos(orig), vec3dFromBlockPos(dest));
    }

    public static Tuple<Float, Float> calcRotationFromVec3d(Vec3d orig, Vec3d dest) {
        double yaw = Math.atan2(orig.x - dest.x, -orig.z + dest.z);
        double dist = Math.sqrt((orig.x - dest.x) * (orig.x - dest.x) + (-orig.x + dest.x) * (-orig.z + dest.z));
        double pitch = Math.atan2(orig.y - dest.y, dist);
        return new Tuple<>((float) (yaw * 180 / Math.PI),
                (float) (pitch * 180 / Math.PI));
    }

    public static Vec3d calcCenterFromCoords(BlockPos orig, World world) {
        IBlockState b = world.getBlockState(orig);
        AxisAlignedBB bbox = b.getBoundingBox(world, orig);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        return new Vec3d(orig.getX() + xDiff,
                orig.getY() + yDiff,
                orig.getZ() + zDiff);
    }

    public static Vec3d vec3dFromBlockPos(BlockPos orig) {
        return new Vec3d(orig.getX() + 0.0D, orig.getY() + 0.0D, orig.getZ() + 0.0D);
    }

    public static double distanceToCenter(BlockPos pos, double x, double y, double z) {
        double xdiff = x - (pos.getX() + 0.5D);
        double ydiff = y - (pos.getY() + 0.5D);
        double zdiff = z - (pos.getZ() + 0.5D);
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }
}
