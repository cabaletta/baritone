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

package baritone.utils;

import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import static baritone.behavior.impl.LookBehaviorUtils.calcVec3dFromRotation;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class RayTraceUtils implements Helper {

    private RayTraceUtils() {}

    public static RayTraceResult simulateRayTrace(float yaw, float pitch) {
        RayTraceResult oldTrace = mc.objectMouseOver;
        float oldYaw = mc.player.rotationYaw;
        float oldPitch = mc.player.rotationPitch;

        mc.player.rotationYaw = yaw;
        mc.player.rotationPitch = pitch;

        mc.entityRenderer.getMouseOver(1.0F);
        RayTraceResult result = mc.objectMouseOver;
        mc.objectMouseOver = oldTrace;

        mc.player.rotationYaw = oldYaw;
        mc.player.rotationPitch = oldPitch;

        return result;
    }

    public static RayTraceResult rayTraceTowards(Rotation rotation) {
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
}
