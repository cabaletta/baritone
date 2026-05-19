package adris.altoclef.trackers.blacklisting;

import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WorldLocateBlacklist extends AbstractObjectBlacklist<BlockPos> {
    @Override
    protected Vec3d getPos(BlockPos item) {
        return WorldHelper.toVec3d(item);
    }
}
