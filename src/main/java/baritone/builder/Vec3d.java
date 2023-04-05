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

package baritone.builder;

import baritone.api.utils.BetterBlockPos;

public class Vec3d {

    public final double x;
    public final double y;
    public final double z;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d(double[] vec) {
        this(vec[0], vec[1], vec[2]);
        if (vec.length != 3) {
            throw new IllegalArgumentException();
        }
    }

    public boolean inOriginUnitVoxel() {
        return x >= 0 && x <= 1 && y >= 0 && y <= 1 && z >= 0 && z <= 1;
    }

    public Vec3d plus(double x, double y, double z) {
        return new Vec3d(this.x + x, this.y + y, this.z + z);
    }

    public long getRoundedToZeroPositionUnsafeDontUse() {
        return BetterBlockPos.toLong((int) x, (int) y, (int) z);
    }

    public double distSq(Vec3d other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Face flatDirectionTo(Vec3d dst) {
        return new Vec3d(dst.x - x, dst.y - y, dst.z - z).flatDirection();
    }

    private static final double AMBIGUITY_TOLERANCE = 0.01;

    public Face flatDirection() {
        if (Math.abs(Math.abs(x) - Math.abs(z)) < AMBIGUITY_TOLERANCE) {
            throw new IllegalStateException("ambiguous");
        }
        if (Math.abs(x) > Math.abs(z)) {
            if (x > 0) {
                return Face.EAST;
            } else {
                return Face.WEST;
            }
        } else {
            if (z > 0) {
                return Face.SOUTH;
            } else {
                return Face.NORTH;
            }
        }
    }

    @Override
    public String toString() {
        return "Vec3d{" + x + "," + y + "," + z + "}";
    }
}
