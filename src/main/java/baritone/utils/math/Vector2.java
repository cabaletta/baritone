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

package baritone.utils.math;

public class Vector2 {

    public double x;
    public double y;

    public Vector2() {
        this(0.0, 0.0);
    }

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 plus(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }

    public Vector2 minus(Vector2 other) {
        return new Vector2(x - other.x, y - other.y);
    }

    public Vector2 times(double factor) {
        return new Vector2(x * factor, y * factor);
    }

    public Vector2 dividedBy(double divisor) {
        return new Vector2(x / divisor, y / divisor);
    }

    public double magnitudeSqr() {
        return x * x + y * y;
    }

    public double magnitude() {
        return Math.sqrt(magnitudeSqr());
    }

    public Vector2 normalize() {
        return dividedBy(magnitude());
    }

    public double distanceToSqr(Vector2 other) {
        return other.minus(this).magnitudeSqr();
    }

    public double dot(Vector2 other) {
        return x * other.x + y * other.y;
    }

}
