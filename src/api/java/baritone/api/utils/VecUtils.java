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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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
    public static Vec3 calculateBlockCenter(Level world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        VoxelShape shape = b.getCollisionShape(world, pos);
        if (shape.isEmpty()) {
            return getBlockPosCenter(pos);
        }
        double xDiff = (shape.min(Direction.Axis.X) + shape.max(Direction.Axis.X)) / 2;
        double yDiff = (shape.min(Direction.Axis.Y) + shape.max(Direction.Axis.Y)) / 2;
        double zDiff = (shape.min(Direction.Axis.Z) + shape.max(Direction.Axis.Z)) / 2;
        if (Double.isNaN(xDiff) || Double.isNaN(yDiff) || Double.isNaN(zDiff)) {
            throw new IllegalStateException(b + " " + pos + " " + shape);
        }
        if (b.getBlock() instanceof BaseFireBlock) {//look at bottom of fire when putting it out
            yDiff = 0;
        }
        return new Vec3(
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
     * @see #calculateBlockCenter(Level, BlockPos)
     */
    public static Vec3 getBlockPosCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
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
        return distanceToCenter(pos, entity.position().x, entity.position().y, entity.position().z);
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
        return distanceToCenter(pos, entity.position().x, pos.getY() + 0.5, entity.position().z);
    }

    public static Vec3 vDivide (Vec3 A, Vec3 B) {
        return new Vec3(A.x / B.x, A.y / B.y, A.z / B.z);
    }
}
