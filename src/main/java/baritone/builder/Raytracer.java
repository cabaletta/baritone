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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Optional;

/**
 * Traces rays
 */
public class Raytracer {

    public static Optional<Raytrace> runTrace(Vec3d playerEye, long againstPos, Face againstFace, Vec3d hit) {
        if (againstFace.offset(againstPos) != 0) {
            throw new IllegalStateException("sanity check - for now we assume that all placed blocks end up at 0,0,0");
        }
        if (Main.STRICT_Y && floor(playerEye.y) < 0) {
            throw new IllegalStateException("im lazy and dont want to fix occupancyCountByY");
        }
        long hitPos = BetterBlockPos.toLong(floor(hit.x), floor(hit.y), floor(hit.z));
        if (hitPos != againstPos && hitPos != 0) {
            throw new IllegalStateException("ambiguous or incorrect hitvec?");
        }
        LongArrayList trace = rayTrace(playerEye.x, playerEye.y, playerEye.z, hit.x, hit.y, hit.z);
        if (trace.size() < 2) {
            throw new IllegalStateException();
        }
        if (trace.getLong(trace.size() - 1) == 0 && trace.getLong(trace.size() - 2) == againstPos) {
            // placing through the block
            // for example, we might be standing to the south of the againstPos but we want to place against the north side of it
            // while we can raytrace to that face, the problem is that our raytrace is hitting some other side of againstPos (maybe south or up) first and then hitting the face we want second
            return Optional.empty();
        }
        return Optional.of(new Raytrace(playerEye, againstPos, againstFace, hit, trace));
    }

    public static class Raytrace implements Comparable<Raytrace> {

        public final long againstPos;
        public final Face againstFace;
        public final long[] passedThrough;

        public final Vec3d playerEye;
        public final Vec3d hit;

        public final int[] occupancyCounts;

        private Raytrace(Vec3d playerEye, long againstPos, Face againstFace, Vec3d hit, LongArrayList trace) {
            this.againstFace = againstFace;
            this.againstPos = againstPos;
            this.playerEye = playerEye;
            this.hit = hit;
            if (trace.getLong(trace.size() - 1) != againstPos) {
                throw new IllegalStateException();
            }
            if (trace.getLong(trace.size() - 2) != 0) {
                throw new IllegalStateException();
            }
            trace.removeLong(trace.size() - 1); // againstPos doesn't ACTUALLY need to be air, so remove it. it was only there for sanity checking and confirming which face we collided with first
            trace.trim();
            if (trace.getLong(0) != BetterBlockPos.toLong(floor(playerEye.x), floor(playerEye.y), floor(playerEye.z))) {
                throw new IllegalStateException();
            }
            this.passedThrough = trace.elements();
            this.occupancyCounts = computeOccupancyCount();
        }

        private int[] computeOccupancyCount() {
            long freebieTop = passedThrough[0]; // player eye
            long freebieBottom = Face.DOWN.offset(freebieTop); // player feet
            if (Main.STRICT_Y) {
                IntArrayList list = new IntArrayList();
                for (int i = passedThrough.length - 1; i >= 0; i--) {
                    long pos = passedThrough[i];
                    int y = BetterBlockPos.YfromLong(pos);
                    if (list.size() == y) {
                        list.add(0);
                    }
                    if (list.size() != y + 1) { // this works because we go in reverse order
                        throw new IllegalStateException("nonconsecutive");
                    }
                    if (pos == freebieTop) {
                        continue;
                    }
                    if (pos == freebieBottom) {
                        if (i != 1) {
                            throw new IllegalStateException();
                        }
                        continue;
                    }
                    list.elements()[y]++; // troll face
                }
                list.trim();
                return list.elements();
            } else {
                int cnt = 0;
                for (long pos : passedThrough) {
                    if (pos != freebieBottom && pos != freebieTop) {
                        cnt++;
                    }
                }
                return new int[]{cnt};
            }
        }

        @Override
        public int compareTo(Raytrace o) { // lower is better
            { // first, sort by occupancy counts. shortest ray wins. or rather, ray that is shortest on the bottom.
                if (occupancyCounts.length != o.occupancyCounts.length) {
                    // any two traces with the same src and dst MAY very well have different passedThrough.length
                    // HOWEVER they are guaranteed to have the same occupancyCounts.length, since they start at the same floor(y) and end at the same floor(y)
                    throw new IllegalStateException("comparing raytraces with unequal src/dst");
                }
                for (int i = 0; i < occupancyCounts.length; i++) {
                    int cmp = Integer.compare(occupancyCounts[i], o.occupancyCounts[i]);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
            }
            { // if occupancy counts match, tiebreak with strict center winning over loose center
                int cmp = Double.compare(centerDist(), o.centerDist());
                if (cmp != 0) {
                    return cmp;
                }
            }
            { // if center status matches, finally tiebreak with simple ray length
                return Double.compare(hit.distSq(playerEye), o.hit.distSq(o.playerEye));
            }
        }

        private double centerDist() {
            double dx = playerEye.x - (floor(playerEye.x) + 0.5d);
            double dz = playerEye.z - (floor(playerEye.z) + 0.5d);
            return dx * dx + dz * dz;
        }
    }

    private static LongArrayList rayTrace(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        // i'd love to use strictfp here and on floor, but it could unironically prevent a much needed JIT to native, so I won't, sadly
        double diffX = endX - startX;
        double diffY = endY - startY;
        double diffZ = endZ - startZ;

        int prevX = Integer.MIN_VALUE;
        int prevY = Integer.MIN_VALUE;
        int prevZ = Integer.MIN_VALUE;
        LongArrayList ret = new LongArrayList();
        for (int step = 0; step <= 10010; step++) { // not a typo 😈😈😈😈😈😈😈😈😈😈😈
            double frac = step / 10000.0d; // go THROUGH the face by a little bit, poke through into the block
            int x = floor(startX + diffX * frac);
            int y = floor(startY + diffY * frac);
            int z = floor(startZ + diffZ * frac);
            if (x == prevX && y == prevY && z == prevZ) {
                continue;
            }
            prevX = x;
            prevY = y;
            prevZ = z;
            long toAdd = BetterBlockPos.toLong(x, y, z);
            if (!ret.isEmpty()) {
                long prev = ret.getLong(ret.size() - 1);
                if (!adjacent(toAdd, prev)) {
                    throw new IllegalStateException(BetterBlockPos.fromLong(prev) + " to " + BetterBlockPos.fromLong(toAdd));
                }
            }
            ret.add(toAdd);
        }
        return ret;
    }

    private static boolean adjacent(long a, long b) {
        if (a == b) {
            throw new IllegalStateException();
        }
        for (Face face : Face.VALUES) {
            if (face.offset(a) == b) {
                return true;
            }
        }
        return false;
    }

    private static int floor(double value) {
        int rawCast = (int) value;
        if (value < (double) rawCast) {
            // casting rounded up (probably because value < 0)
            return rawCast - 1;
        } else {
            // casting rounded down
            return rawCast;
        }
    }
}
