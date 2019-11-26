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

public class GeometryHelper {

    // from https://stackoverflow.com/a/9557244.
    public static Vector2 getClosestPointOnSegment(Vector2 a, Vector2 b, Vector2 point) {
        Vector2 ap = point.minus(a);
        Vector2 ab = b.minus(a);

        double magnitudeABSqr = ab.magnitudeSqr();
        double abDotAP = ab.dot(ap);

        double distance = abDotAP / magnitudeABSqr;
        if (distance <= 0.0) {
            return a;
        } else if (distance >= 1.0) {
            return b;
        } else {
            return a.plus(ab.times(distance));
        }
    }

}
