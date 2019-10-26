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

public class MathUtils {

    // from https://stackoverflow.com/a/9557244.
    public static Vector2 getClosestPointOnSegment(Vector2 lineA, Vector2 lineB, Vector2 point) {
        Vector2 ap = point.minus(lineA);
        Vector2 ab = lineB.minus(lineA);

        double magnitudeABSqr = ab.magnitudeSqr();
        double abDotAP = ab.dot(ap);

        double distance = abDotAP / magnitudeABSqr;
        if (distance <= 0.0) {
            return lineA;
        } else if (distance >= 1.0) {
            return lineB;
        } else {
            return lineA.plus(ab.times(distance));
        }
    }

}
