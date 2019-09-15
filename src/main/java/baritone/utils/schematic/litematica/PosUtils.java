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

package baritone.utils.schematic.litematica;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class PosUtils {
    public static BlockPos getRelativeEndPos(Vec3i size) {
        int x = size.getX();
        int y = size.getY();
        int z = size.getZ();

        BlockPos result = new BlockPos(
                x >= 0 ? x - 1 : x + 1,
                y >= 0 ? y - 1 : y + 1,
                z >= 0 ? z - 1 : z + 1);

        return result;
    }
    public static BlockPos getMinCorner(BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        return new BlockPos(minX, minY, minZ);
    }

    public static BlockPos getMaxCorner(BlockPos pos1, BlockPos pos2) {
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return new BlockPos(maxX, maxY, maxZ);
    }
}
