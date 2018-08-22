/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior.impl;

import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.Rotation;
import baritone.utils.Utils;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.*;

import java.util.Optional;

import static baritone.utils.Utils.DEG_TO_RAD;

public final class LookBehaviorUtils implements Helper {

    /**
     * Offsets from the root block position to the center of each side.
     */
    private static final Vec3d[] BLOCK_SIDE_MULTIPLIERS = new Vec3d[]{
            new Vec3d(0.5, 0, 0.5), // Down
            new Vec3d(0.5, 1, 0.5), // Up
            new Vec3d(0.5, 0.5, 0), // North
            new Vec3d(0.5, 0.5, 1), // South
            new Vec3d(0, 0.5, 0.5), // West
            new Vec3d(1, 0.5, 0.5)  // East
    };

    /**
     * Calculates a vector given a rotation array
     *
     * @param rotation {@link LookBehavior#target}
     * @return vector of the rotation
     */
    public static Vec3d calcVec3dFromRotation(Rotation rotation) {
        float f = MathHelper.cos(-rotation.getFirst() * (float) DEG_TO_RAD - (float) Math.PI);
        float f1 = MathHelper.sin(-rotation.getFirst() * (float) DEG_TO_RAD - (float) Math.PI);
        float f2 = -MathHelper.cos(-rotation.getSecond() * (float) DEG_TO_RAD);
        float f3 = MathHelper.sin(-rotation.getSecond() * (float) DEG_TO_RAD);
        return new Vec3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public static Optional<Rotation> reachable(BlockPos pos) {
        if (pos.equals(getSelectedBlock().orElse(null))) {
            /*
             * why add 0.0001?
             * to indicate that we actually have a desired pitch
             * the way we indicate that the pitch can be whatever and we only care about the yaw
             * is by setting the desired pitch to the current pitch
             * setting the desired pitch to the current pitch + 0.0001 means that we do have a desired pitch, it's
             * just what it currently is
             */
            return Optional.of(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch + 0.0001f));
        }
        Optional<Rotation> possibleRotation = reachableCenter(pos);
        System.out.println("center: " + possibleRotation);
        if (possibleRotation.isPresent())
            return possibleRotation;

        IBlockState state = BlockStateInterface.get(pos);
        AxisAlignedBB aabb = state.getBoundingBox(mc.world, pos);
        for (Vec3d sideOffset : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = aabb.minX * sideOffset.x + aabb.maxX * (1 - sideOffset.x);
            double yDiff = aabb.minY * sideOffset.y + aabb.maxY * (1 - sideOffset.y);
            double zDiff = aabb.minZ * sideOffset.z + aabb.maxZ * (1 - sideOffset.z);
            possibleRotation = reachableOffset(pos, new Vec3d(pos).add(xDiff, yDiff, zDiff));
            if (possibleRotation.isPresent())
                return possibleRotation;
        }
        return Optional.empty();
    }

    private static RayTraceResult rayTraceTowards(Rotation rotation) {
        double blockReachDistance = mc.playerController.getBlockReachDistance();
        Vec3d start = mc.player.getPositionEyes(1.0F);
        Vec3d direction = calcVec3dFromRotation(rotation);
        Vec3d end = start.add(
                direction.x * blockReachDistance,
                direction.y * blockReachDistance,
                direction.z * blockReachDistance
        );
        return mc.world.rayTraceBlocks(start, end, false, false, true);
    }

    /**
     * Checks if coordinate is reachable with the given block-face rotation offset
     *
     * @param pos
     * @param offsetPos
     * @return
     */
    protected static Optional<Rotation> reachableOffset(BlockPos pos, Vec3d offsetPos) {
        Rotation rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F), offsetPos);
        RayTraceResult result = rayTraceTowards(rotation);
        System.out.println(result);
        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
            if (result.getBlockPos().equals(pos)) {
                return Optional.of(rotation);
            }
            if (BlockStateInterface.get(pos).getBlock() instanceof BlockFire) {
                if (result.getBlockPos().equals(pos.down())) {
                    return Optional.of(rotation);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if center of block at coordinate is reachable
     *
     * @param pos
     * @return
     */
    protected static Optional<Rotation> reachableCenter(BlockPos pos) {
        return reachableOffset(pos, Utils.calcCenterFromCoords(pos, mc.world));
    }

    /**
     * The currently highlighted block.
     * Updated once a tick by Minecraft.
     *
     * @return the position of the highlighted block
     */
    public static Optional<BlockPos> getSelectedBlock() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            return Optional.of(mc.objectMouseOver.getBlockPos());
        }
        return Optional.empty();
    }
}
