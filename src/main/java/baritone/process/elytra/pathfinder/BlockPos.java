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

/** An integer position. Immutable. */
final class BlockPos {

    final int x;
    final int y;
    final int z;

    BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    int chunkX() {
        return this.x >> 4;
    }

    int chunkZ() {
        return this.z >> 4;
    }

    double distanceToSq(BlockPos pos) {
        final double dx = pos.x - this.x;
        final double dy = pos.y - this.y;
        final double dz = pos.z - this.z;
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

    double distanceTo(BlockPos pos) {
        return Math.sqrt(this.distanceToSq(pos));
    }

    BlockPos offset(Face face, int n) {
        switch (face) {
            case UP: return up(n);
            case DOWN: return down(n);
            case NORTH: return north(n);
            case SOUTH: return south(n);
            case EAST: return east(n);
            default: return west(n);
        }
    }

    BlockPos up(int n) {
        return new BlockPos(this.x, this.y + n, this.z);
    }

    BlockPos down(int n) {
        return new BlockPos(this.x, this.y - n, this.z);
    }

    BlockPos east(int n) {
        return new BlockPos(this.x + n, this.y, this.z);
    }

    BlockPos west(int n) {
        return new BlockPos(this.x - n, this.y, this.z);
    }

    BlockPos north(int n) {
        return new BlockPos(this.x, this.y, this.z - n);
    }

    BlockPos south(int n) {
        return new BlockPos(this.x, this.y, this.z + n);
    }

    BlockPos plus(int n) {
        return new BlockPos(this.x + n, this.y + n, this.z + n);
    }

    BlockPos shiftRight(int n) {
        return new BlockPos(this.x >> n, this.y >> n, this.z >> n);
    }

    BlockPos shiftLeft(int n) {
        return new BlockPos(this.x << n, this.y << n, this.z << n);
    }

    static int floor(double v) {
        final int i = (int) v;
        return v < i ? i - 1 : i;
    }

    static BlockPos of(double x, double y, double z) {
        return new BlockPos(floor(x), floor(y), floor(z));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockPos)) {
            return false;
        }
        final BlockPos p = (BlockPos) o;
        return p.x == this.x && p.y == this.y && p.z == this.z;
    }

    @Override
    public int hashCode() {
        return (this.x * 8734625) ^ (this.y * 2873465) ^ (this.z * 3457689);
    }

    @Override
    public String toString() {
        return "{" + this.x + ", " + this.y + ", " + this.z + "}";
    }
}
