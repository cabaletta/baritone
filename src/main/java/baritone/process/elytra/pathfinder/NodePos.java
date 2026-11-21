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

package baritone.process.elytra.pathfinder;

import net.minecraft.core.BlockPos;

/** A cube of the octree: its size and its position in units of that size. */
final class NodePos {

    final Size size;
    /** absolute position divided by the width */
    private final int x;
    private final int y;
    private final int z;

    /** The cube of the given size that holds the block at (x, y, z). */
    NodePos(Size size, int x, int y, int z) {
        this.size = size;
        final int shift = size.shift();
        this.x = x >> shift;
        this.y = y >> shift;
        this.z = z >> shift;
    }

    /** The absolute coordinates of the cube's lowest corner. */
    int minX() {
        return this.x << this.size.shift();
    }

    int minY() {
        return this.y << this.size.shift();
    }

    int minZ() {
        return this.z << this.size.shift();
    }

    /** The block at the cube's centre, which is what a path is made of. */
    BlockPos absolutePosCenter() {
        final int half = this.size.width() / 2;
        return new BlockPos(this.minX() + half, this.minY() + half, this.minZ() + half);
    }

    /** The squared distance from the block at the cube's centre to pos, as BlockPos.distSqr has it. */
    double centerDistSqr(BlockPos pos) {
        final int half = this.size.width() / 2;
        final double dx = this.minX() + half - pos.getX();
        final double dy = this.minY() + half - pos.getY();
        final double dz = this.minZ() + half - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodePos)) {
            return false;
        }
        final NodePos n = (NodePos) o;
        return n.size == this.size && n.x == this.x && n.y == this.y && n.z == this.z;
    }

    @Override
    public int hashCode() {
        long hash = 3241;
        hash = 6406146L * hash + this.size.ordinal();
        hash = 3457689L * hash + this.x;
        hash = 8734625L * hash + this.y;
        hash = 2873465L * hash + this.z;
        return (int) (hash ^ (hash >>> 32));
    }
}
