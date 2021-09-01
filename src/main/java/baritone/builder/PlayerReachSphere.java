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
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class PlayerReachSphere {

    public final long[] positions;

    public PlayerReachSphere(double playerReachDistance) {
        int ceiledPlusOne = 1 + (int) Math.ceil(playerReachDistance);
        double realSq = playerReachDistance * playerReachDistance;
        int ceiledSq = (int) Math.ceil(realSq);
        LongArrayList sphere = new LongArrayList();
        for (int x = -ceiledPlusOne; x <= ceiledPlusOne; x++) {
            for (int y = -ceiledPlusOne; y <= ceiledPlusOne; y++) {
                for (int z = -ceiledPlusOne; z <= ceiledPlusOne; z++) {
                    int d = closestPossibleDist(x, y, z);
                    if (d <= ceiledSq && d < realSq) { // int comparison short circuits before floating point
                        sphere.add(BetterBlockPos.toLong(x, y, z));
                    }
                }
            }
        }
        sphere.trim();
        positions = sphere.elements();
    }

    public static int closestPossibleDist(int dx, int dy, int dz) { // player eye is within the origin voxel (0,0,0)
        dx = lower(dx);
        dy = lower(dy);
        dz = lower(dz);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Bring closer to 0 by one if not already zero
     */
    private static int lowerBranchy(int v) {
        if (v > 0) {
            return v - 1;
        } else if (v < 0) {
            return v + 1;
        } else {
            return v;
        }
    }

    private static int lower(int v) {
        return v + (v >>> 31) - ((-v) >>> 31);
    }

    static {
        for (int i = -10; i <= 10; i++) {
            if (lowerBranchy(i) != lower(i)) {
                throw new IllegalStateException();
            }
        }
    }
}
