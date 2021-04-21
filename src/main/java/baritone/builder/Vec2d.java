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

public class Vec2d {

    public final double x;
    public final double z;

    public Vec2d(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public Vec2d plus(double dx, double dz) {
        return new Vec2d(this.x + dx, this.z + dz);
    }

    public double[] toArr() {
        return new double[]{x, z};
    }

    public static final Vec2d HALVED_CENTER = new Vec2d(0.5d, 0.5d);
}
