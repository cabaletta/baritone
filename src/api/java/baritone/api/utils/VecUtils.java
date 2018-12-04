/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.utils;

import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 10/13/2018
 */
public final class VecUtils {

    private VecUtils() {}

    /**
     * Calculates the center of the block at the specified position's bounding box
     *
     * @param world The world that the block is in, used to provide the bounding box
     * @param pos   The block position
     * @return The center of the block's bounding box
     * @see #getBlockPosCenter(BlockPos)
     */
    public static Vec3d calculateBlockCenter(World world, BlockPos pos) {
        IBlockState b = world.getBlockState(pos);
        AxisAlignedBB bbox = b.getBoundingBox(world, pos);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        if (b.getBlock() instanceof BlockFire) {//look at bottom of fire when putting it out
            yDiff = 0;
        }
        return new Vec3d(
                pos.getX() + xDiff,
                pos.getY() + yDiff,
                pos.getZ() + zDiff
        );
    }

    /**
     * Gets the assumed center position of the given block position.
     * This is done by adding 0.5 to the X, Y, and Z axes.
     * <p>
     * TODO: We may want to consider replacing many usages of this method with #calculateBlockCenter(BlockPos)
     *
     * @param pos The block position
     * @return The assumed center of the position
     * @see #calculateBlockCenter(World, BlockPos)
     */
    public static Vec3d getBlockPosCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Gets the distance from the specified position to the assumed center of the specified block position.
     *
     * @param pos The block position
     * @param x   The x pos
     * @param y   The y pos
     * @param z   The z pos
     * @return The distance from the assumed block center to the position
     * @see #getBlockPosCenter(BlockPos)
     */
    public static double distanceToCenter(BlockPos pos, double x, double y, double z) {
        double xdiff = pos.getX() + 0.5 - x;
        double ydiff = pos.getY() + 0.5 - y;
        double zdiff = pos.getZ() + 0.5 - z;
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }

    /**
     * Gets the distance from the specified entity's position to the assumed
     * center of the specified block position.
     *
     * @param entity The entity
     * @param pos    The block position
     * @return The distance from the entity to the block's assumed center
     * @see #getBlockPosCenter(BlockPos)
     */
    public static double entityDistanceToCenter(Entity entity, BlockPos pos) {
        return distanceToCenter(pos, entity.posX, entity.posY, entity.posZ);
    }

    /**
     * Gets the distance from the specified entity's position to the assumed
     * center of the specified block position, ignoring the Y axis.
     *
     * @param entity The entity
     * @param pos    The block position
     * @return The horizontal distance from the entity to the block's assumed center
     * @see #getBlockPosCenter(BlockPos)
     */
    public static double entityFlatDistanceToCenter(Entity entity, BlockPos pos) {
        return distanceToCenter(pos, entity.posX, pos.getY() + 0.5, entity.posZ);
    }
}
