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
import java.util.stream.Collectors;

/**
 * Traces rays
 */
public class Raytracer {

    public static Optional<Raytrace> runTrace(Vec3d playerEye, long againstPos, Face againstFace, Vec3d hit) {
        // TODO this totally could be cached tho...
        if (againstFace.offset(againstPos) != 0) {
            throw new IllegalStateException("sanity check - for now we assume that all placed blocks end up at 0,0,0");
        }
        if (toLong(playerEye) == 0) {
            throw new IllegalStateException("player eye is within the block we want to place, this is maybe possible? idk i suppose you could do this with, like, a torch? still seems weird. idk if this should be allowed");
        }
        if (toLong(playerEye) == againstPos) {
            throw new IllegalStateException("player eye is within the block we want to place against, this is DEFINITELY impossible");
        }
        //System.out.println(BetterBlockPos.fromLong(toLong(playerEye)) + " to " + BetterBlockPos.fromLong(toLong(hit)) + " aka " + playerEye + " to " + hit);
        if (Main.STRICT_Y && floor(playerEye.y) < 0) {
            throw new IllegalStateException("im lazy and dont want to fix occupancyCountByY");
        }
        long hitPos = toLong(hit);
        if (hitPos != againstPos && hitPos != 0) {
            throw new IllegalStateException("ambiguous or incorrect hitvec?");
        }
        LongArrayList trace = rayTrace(playerEye.x, playerEye.y, playerEye.z, hit.x, hit.y, hit.z, againstPos);
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

    private static long toLong(Vec3d hit) {
        return BetterBlockPos.toLong(floor(hit.x), floor(hit.y), floor(hit.z));
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
                print(trace);
                throw new IllegalStateException();
            }
            if (trace.getLong(trace.size() - 2) != 0) {
                throw new IllegalStateException();
            }
            for (int i = 0; i < trace.size() - 1; i++) {
                if (!adjacent(trace.getLong(i), trace.getLong(i + 1))) {
                    throw new IllegalStateException(BetterBlockPos.fromLong(trace.getLong(i)) + " to " + BetterBlockPos.fromLong(trace.getLong(i + 1)));
                }
            }
            trace.removeLong(trace.size() - 1); // againstPos doesn't ACTUALLY need to be air, so remove it. it was only there for sanity checking and confirming which face we collided with first
            trace.trim();
            if (trace.getLong(0) != toLong(playerEye)) {
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
                    if (list.size() == y) { // works because we removed the last trace element (against), which could have negative y
                        list.add(0);
                    }
                    if (list.size() != y + 1) { // this works because we go in reverse order
                        throw new IllegalStateException("nonconsecutive");
                    }
                    if (pos == freebieTop) {
                        // only here for correctness in spirit, technically not needed for comparison since it will exist in all of them
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
                int cmp = Double.compare(centerDistApprox(), o.centerDistApprox());
                if (cmp != 0) {
                    return cmp;
                }
            }
            { // if center status matches, finally tiebreak with simple ray length
                return Double.compare(distSq(), o.distSq());
            }
        }

        public double centerDistApprox() {
            // calculate distance to center of block but intentionally round it off
            // the intent is for LOOSE_CENTER to always tie with itself, even though floating point inaccuracy would make it unequal if we did a direct Double.compare
            double dx = playerEye.x - (floor(playerEye.x) + 0.5d);
            double dz = playerEye.z - (floor(playerEye.z) + 0.5d);
            double dist = dx * dx + dz * dz;
            dist = Math.round(dist * 1000000);
            return dist;
        }

        public double distSq() {
            return hit.distSq(playerEye);
        }
    }

    /**
     * If I add 10 to all the numbers I raytrace, then subtract them afterwards, then I can just use (int) instead of the nasty BRANCHING floor.
     * <p>
     * No difference raytracing from -2 to -1 as it is to raytrace from 8 to 9. Just add ten!
     */
    private static final int POSITIVITY_OFFSET = 10;

    private static final int NUM_STEPS = 10_000;

    public static int raytraceMode = 2;

    private static LongArrayList rayTrace(double rawStartX, double rawStartY, double rawStartZ, double endX, double endY, double endZ, long againstPos) {
        LongArrayList slow = raytraceMode == 0 || Main.SLOW_DEBUG ? rayTraceSlow(rawStartX, rawStartY, rawStartZ, endX, endY, endZ) : null;
        LongArrayList fast = raytraceMode == 1 || Main.SLOW_DEBUG ? rayTraceFast(rawStartX, rawStartY, rawStartZ, endX, endY, endZ) : null;
        LongArrayList faster = raytraceMode == 2 || Main.SLOW_DEBUG ? rayTraceZoomy(rawStartX, rawStartY, rawStartZ, endX, endY, endZ, againstPos) : null;
        if (Main.SLOW_DEBUG) {
            if (fast.equals(slow) && fast.equals(faster)) {
            } else {
                System.out.println(rawStartX + " " + rawStartY + " " + rawStartZ + " " + endX + " " + endY + " " + endZ + " " + againstPos);
                print(slow);
                print(fast);
                print(faster);
                throw new IllegalStateException();
            }
        }
        return fast == null ? slow == null ? faster : slow : fast;
    }

    private static void print(LongArrayList trace) {
        System.out.println(trace.stream().map(BetterBlockPos::fromLong).collect(Collectors.toList()));
    }

    private static LongArrayList rayTraceFast(double rawStartX, double rawStartY, double rawStartZ, double endX, double endY, double endZ) {
        if (willFlipSign(rawStartX) || willFlipSign(rawStartY) || willFlipSign(rawStartZ) || willFlipSign(endX) || willFlipSign(endY) || willFlipSign(endZ)) {
            throw new IllegalStateException("I suppose this could happen if you set the block reach distance absurdly high? Don't do that.");
        }
        double diffX = endX - rawStartX;
        double diffY = endY - rawStartY;
        double diffZ = endZ - rawStartZ;
        if (Math.abs(diffX) < 0.1 && Math.abs(diffY) < 0.1 && Math.abs(diffZ) < 0.1) {
            // need more checks than before because now the tightest inner do-while does NOT check step against any upper limit at all
            // therefore, if diff was zero, it would truly get stuck indefinitely, unlike previously where it would bail out at 10010
            throw new IllegalArgumentException("throwing exception instead of entering infinite inner loop");
        }
        double startX = rawStartX + POSITIVITY_OFFSET;
        double startY = rawStartY + POSITIVITY_OFFSET;
        double startZ = rawStartZ + POSITIVITY_OFFSET;

        int x = Integer.MIN_VALUE;
        int y = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        LongArrayList voxelsIntersected = new LongArrayList();
        int step = 0;
        double mult = 1.0d / NUM_STEPS;
        double frac;
        while (step < NUM_STEPS) {
            do frac = ++step * mult;
            while (((x ^ (x = (int) (startX + diffX * frac))) | (y ^ (y = (int) (startY + diffY * frac))) | (z ^ (z = (int) (startZ + diffZ * frac)))) == 0);
            voxelsIntersected.add(BetterBlockPos.toLong(x - POSITIVITY_OFFSET, y - POSITIVITY_OFFSET, z - POSITIVITY_OFFSET));
        }
        if (step > NUM_STEPS + 1) {
            throw new IllegalStateException("No floating point inaccuracies allowed. Or, at least, no more than 2 parts in 10,000 of wiggle room lol. " + step);
        }
        return voxelsIntersected;
    }

    /**
     * Here's an alternate implementation of the above that is functionally the same, just simpler (and slower)
     * <p>
     * The inner loop branches seven times instead of one, for example.
     */
    private static LongArrayList rayTraceSlow(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        // i'd love to use strictfp here and on floor, but it could unironically prevent a much needed JIT to native, so I won't, sadly
        double diffX = endX - startX;
        double diffY = endY - startY;
        double diffZ = endZ - startZ;

        int prevX = Integer.MIN_VALUE;
        int prevY = Integer.MIN_VALUE;
        int prevZ = Integer.MIN_VALUE;
        LongArrayList ret = new LongArrayList();
        int ourLimit = NUM_STEPS + 1;
        for (int step = 0; step <= ourLimit; step++) { // 1 branch (step <= ourLimit)
            double frac = step / (double) NUM_STEPS; // go THROUGH the face by a little bit, poke through into the block
            int x = floor(startX + diffX * frac); // 1 branch in floor
            int y = floor(startY + diffY * frac); // 1 branch in floor
            int z = floor(startZ + diffZ * frac); // 1 branch in floor
            if (x == prevX && y == prevY && z == prevZ) { // 3 branches (due to && short circuiting)
                continue;
            }
            prevX = x;
            prevY = y;
            prevZ = z;
            ret.add(BetterBlockPos.toLong(x, y, z));
        }
        return ret;
    }

    private static boolean willFlipSign(double pos) {
        return flippedSign(pos + POSITIVITY_OFFSET);
    }

    private static boolean flippedSign(double pos) {
        return pos < 1d;
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

    public static LongArrayList rayTraceZoomy(double startX, double startY, double startZ, double endX, double endY, double endZ, long againstPos) {
        if (endX < 0 || endX > 1 || endY < 0 || endY > 1 || endZ < 0 || endZ > 1) {
            throw new IllegalStateException("won't work");
        }
        if (flippedSign(startX += POSITIVITY_OFFSET) | flippedSign(startY += POSITIVITY_OFFSET) | flippedSign(startZ += POSITIVITY_OFFSET) | flippedSign(endX += POSITIVITY_OFFSET) | flippedSign(endY += POSITIVITY_OFFSET) | flippedSign(endZ += POSITIVITY_OFFSET)) {
            throw new IllegalStateException("I suppose this could happen if you set the block reach distance absurdly high? Don't do that.");
        }
        int voxelEndX = BetterBlockPos.XfromLong(againstPos) + POSITIVITY_OFFSET;
        int voxelEndY = BetterBlockPos.YfromLong(againstPos) + POSITIVITY_OFFSET;
        int voxelEndZ = BetterBlockPos.ZfromLong(againstPos) + POSITIVITY_OFFSET;

        int voxelInX = (int) startX;
        int voxelInY = (int) startY;
        int voxelInZ = (int) startZ;
        if (startX == (double) voxelInX || startY == (double) voxelInY || startZ == (double) voxelInZ) {
            throw new IllegalStateException("Integral starting coordinates not supported ever since I removed the -0.0d check");
        }

        LongArrayList voxelsIntersected = new LongArrayList();
        int steps = 64; // default is 200
        while (steps-- >= 0) {
            long posAsLong = BetterBlockPos.toLong(voxelInX - POSITIVITY_OFFSET, voxelInY - POSITIVITY_OFFSET, voxelInZ - POSITIVITY_OFFSET);
            voxelsIntersected.add(posAsLong);
            if (posAsLong == againstPos) {
                if (voxelsIntersected.size() == 1 || voxelsIntersected.getLong(voxelsIntersected.size() - 2) != 0) {
                    voxelsIntersected.add(0);
                }
                return voxelsIntersected;
            }
            double nextIntegerX, nextIntegerY, nextIntegerZ;
            // potentially more based branchless impl?
            nextIntegerX = voxelInX + ((voxelInX - voxelEndX) >>> 31); // if voxelEnd > voxelIn, then voxelIn-voxelEnd will be negative, meaning the sign bit is 1
            nextIntegerY = voxelInY + ((voxelInY - voxelEndY) >>> 31); // if we do an unsigned right shift by 31, that sign bit becomes the LSB
            nextIntegerZ = voxelInZ + ((voxelInZ - voxelEndZ) >>> 31); // therefore, this increments nextInteger iff EndX>inX, otherwise it leaves it alone
            // remember: don't have to worry about the case when voxelEnd == voxelIn, because nextInteger value wont be used

            double fracIfSkipX = 100;
            double fracIfSkipY = 100;
            double fracIfSkipZ = 100;
            double distanceFromStartToEndX = endX - startX;
            double distanceFromStartToEndY = endY - startY;
            double distanceFromStartToEndZ = endZ - startZ;
            if (voxelEndX != voxelInX) {
                fracIfSkipX = (nextIntegerX - startX) / distanceFromStartToEndX;
            }
            if (voxelEndY != voxelInY) {
                fracIfSkipY = (nextIntegerY - startY) / distanceFromStartToEndY;
            }
            if (voxelEndZ != voxelInZ) {
                fracIfSkipZ = (nextIntegerZ - startZ) / distanceFromStartToEndZ;
            }
            int dx = 0;
            int dy = 0;
            int dz = 0;
            if (fracIfSkipX < fracIfSkipY && fracIfSkipX < fracIfSkipZ) {
                // note: voxelEndX == voxelInX is impossible because allowSkip would be set to false in that case, meaning that the elapsed distance would stay at default
                dx = (voxelEndX - voxelInX) >>> 31; // TODO should i set this "dx" way up top at the same time as i set nextIntegerX?
                startX = nextIntegerX;
                startY += distanceFromStartToEndY * fracIfSkipX;
                startZ += distanceFromStartToEndZ * fracIfSkipX;
            } else if (fracIfSkipY < fracIfSkipZ) {
                dy = (voxelEndY - voxelInY) >>> 31; // we want dy=1 when endY < inY
                startX += distanceFromStartToEndX * fracIfSkipY;
                startY = nextIntegerY;
                startZ += distanceFromStartToEndZ * fracIfSkipY;
            } else {
                dz = (voxelEndZ - voxelInZ) >>> 31;
                startX += distanceFromStartToEndX * fracIfSkipZ;
                startY += distanceFromStartToEndY * fracIfSkipZ;
                startZ = nextIntegerZ;
            }

            voxelInX = ((int) startX) - dx; // TODO is it faster to paste this block of 3 lines into each of the 3 if branches? we know 2/3 subtracts will be zero, so it would save two subtracts, but is that worth the longer bytecode?
            voxelInY = ((int) startY) - dy;
            voxelInZ = ((int) startZ) - dz;
        }
        print(voxelsIntersected);
        throw new IllegalStateException();
    }
}
