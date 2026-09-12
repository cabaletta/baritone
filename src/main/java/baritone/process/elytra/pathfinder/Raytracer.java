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

import java.util.ArrayList;
import java.util.List;

/**
 * A ray through the chunk octrees: "An efficient parametric algorithm for octree traversal"
 * (Revelles, Urena, Lastra), as the native Refiner.cpp had it. The traversal only walks positive
 * directions, so the ray is reflected around each x16 node's centre and the child indices are
 * flipped back with {@code a}.
 */
final class Raytracer {

    static final int MISS = 0;
    static final int FINISHED = 1;
    static final int HIT = 2;

    static final int PLANE_YZ = 0;
    static final int PLANE_XZ = 1;
    static final int PLANE_XY = 2;

    /** What one x16 step reported, beyond its return code. */
    private static final class Step {
        int exitPlane;
        double hitLen;
    }

    private Raytracer() {}

    private static double reflect(double x, double target) {
        return target - (x - target);
    }

    private static double max(double x, double y) {
        if (Double.isNaN(y)) {
            return x;
        }
        return x > y ? x : y;
    }

    private static double min(double x, double y) {
        if (Double.isNaN(y)) {
            return x;
        }
        return x < y ? x : y;
    }

    private static int exitPlane(double tx1, double ty1, double tz1) {
        if (tx1 < ty1) {
            if (tx1 < tz1) return PLANE_YZ;
        } else {
            if (ty1 < tz1) return PLANE_XZ;
        }
        return PLANE_XY;
    }

    private static int firstNode(double tx0, double ty0, double tz0, double txm, double tym, double tzm) {
        int answer = 0;
        if (tx0 > ty0) {
            if (tx0 > tz0) { // plane YZ
                if (tym < tx0) answer |= 2;
                if (tzm < tx0) answer |= 1;
                return answer;
            }
        } else {
            if (ty0 > tz0) { // plane XZ
                if (txm < ty0) answer |= 4;
                if (tzm < ty0) answer |= 1;
                return answer;
            }
        }
        // plane XY
        if (txm < tz0) answer |= 4;
        if (tym < tz0) answer |= 2;
        return answer;
    }

    private static int newNode(double txm, int x, double tym, int y, double tzm, int z) {
        int out = z; // XY plane
        if (txm < tym) {
            out = txm < tzm ? x : out; // YZ plane
        } else {
            out = tym < tzm ? y : out; // XZ plane
        }
        return out;
    }

    private static double m(double origin, int nodeMin, int nodeMax, double t0, double t1) {
        if (t0 == Double.POSITIVE_INFINITY || t1 == Double.POSITIVE_INFINITY) {
            // 3.3
            final int center = (nodeMin + nodeMax) / 2;
            return origin < center ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        return 0.5 * (t0 + t1);
    }

    /** Bytes of a child of a node at each level: x16 (4) has x8 children of 64 bytes, and so on. x2's children are bits. */
    private static final int[] CHILD_BYTES = {0, 0, 1, 8, 64};

    private static boolean emptyNode(long[] slab, int filled, int off, int level) {
        switch (level) {
            case 4: return filled == 0;
            case 3: return (filled & (1 << (off >>> 6))) == 0; // the x8 at byte offset off is x8 number off / 64
            case 2: return slab[off >>> 3] == 0;
            default: return Chunk.byteAt(slab, off) == 0;
        }
    }

    /**
     * Walks the ray through one node. level 4 is an x16 (the whole slab), 0 a block; a node is
     * the slab plus a byte offset, and at level 0 {@code bit} says which bit of the byte. {@code
     * filled} is the slab's summary of which of its x8 cubes hold a block, see {@link Chunk#filled}.
     * Returns NaN for a miss, positive infinity when the ray ended inside without hitting, and
     * otherwise the length along the ray at which it hit.
     */
    private static double procSubtree(int a, double ox, double oy, double oz, double targetLen,
                                      double tx0, double ty0, double tz0, double tx1, double ty1, double tz1,
                                      int level, int nx, int ny, int nz, long[] slab, int filled, int off, int bit) {
        // if this node is behind us
        if (tx1 < 0.0 || ty1 < 0.0 || tz1 < 0.0) {
            return Double.NaN;
        }
        if (tx0 >= targetLen || ty0 >= targetLen || tz0 >= targetLen) {
            return Double.POSITIVE_INFINITY;
        }
        if (level == 0) { // leaf
            if (((Chunk.byteAt(slab, off) >> bit) & 1) == 0) {
                return Double.NaN;
            }
            if (tx0 > ty0 && tx0 > tz0) {
                return tx0; // plane YZ
            } else if (ty0 > tz0) {
                return ty0; // plane XZ
            } else {
                return tz0; // plane XY
            }
        }
        if (slab == null || emptyNode(slab, filled, off, level)) {
            // we know that all the leafs are empty
            return Double.NaN;
        }
        final int w = 1 << level;
        final double txm = m(ox, nx, nx + w, tx0, tx1);
        final double tym = m(oy, ny, ny + w, ty0, ty1);
        final double tzm = m(oz, nz, nz + w, tz0, tz1);

        final int half = w >> 1;
        final int childLevel = level - 1;
        final int childBytes = CHILD_BYTES[level];
        int currNode = firstNode(tx0, ty0, tz0, txm, tym, tzm);
        do {
            final int i;
            final double r;
            switch (currNode) {
                case 0:
                    i = a;
                    r = procSubtree(a, ox, oy, oz, targetLen, tx0, ty0, tz0, txm, tym, tzm, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(txm, 4, tym, 2, tzm, 1);
                    break;
                case 1:
                    i = 1 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, tx0, ty0, tzm, txm, tym, tz1, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(txm, 5, tym, 3, tz1, 8);
                    break;
                case 2:
                    i = 2 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, tx0, tym, tz0, txm, ty1, tzm, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(txm, 6, ty1, 8, tzm, 3);
                    break;
                case 3:
                    i = 3 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, tx0, tym, tzm, txm, ty1, tz1, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(txm, 7, ty1, 8, tz1, 8);
                    break;
                case 4:
                    i = 4 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, txm, ty0, tz0, tx1, tym, tzm, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(tx1, 8, tym, 6, tzm, 5);
                    break;
                case 5:
                    i = 5 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, txm, ty0, tzm, tx1, tym, tz1, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(tx1, 8, tym, 7, tz1, 8);
                    break;
                case 6:
                    i = 6 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, txm, tym, tz0, tx1, ty1, tzm, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    currNode = newNode(tx1, 8, ty1, 8, tzm, 7);
                    break;
                default:
                    i = 7 ^ a;
                    r = procSubtree(a, ox, oy, oz, targetLen, txm, tym, tzm, tx1, ty1, tz1, childLevel, nx + ((i & 4) != 0 ? half : 0), ny + ((i & 2) != 0 ? half : 0), nz + ((i & 1) != 0 ? half : 0), slab, filled, level == 1 ? off : off + i * childBytes, i);
                    if (!Double.isNaN(r)) return r;
                    return Double.NaN;
            }
        } while (currNode < 8);
        return Double.NaN;
    }

    /** One x16 node. The ray is already reflected. */
    private static int raytrace16(int a, double rox, double roy, double roz, double rdx, double rdy, double rdz, double targetLen,
                                  int nx, int ny, int nz, long[] slab, int filled, Step step) {
        // IEEE stability fix
        final double divx = 1 / rdx;
        final double divy = 1 / rdy;
        final double divz = 1 / rdz;

        final double tx0 = (nx - rox) * divx;
        final double tx1 = (nx + 16 - rox) * divx;
        final double ty0 = (ny - roy) * divy;
        final double ty1 = (ny + 16 - roy) * divy;
        final double tz0 = (nz - roz) * divz;
        final double tz1 = (nz + 16 - roz) * divz;

        // condition 10
        final double tmin = max(max(tx0, ty0), tz0);
        final double tmax = min(min(tx1, ty1), tz1);
        if (tmin <= tmax) {
            final double r = procSubtree(a, rox, roy, roz, targetLen, tx0, ty0, tz0, tx1, ty1, tz1, 4, nx, ny, nz, slab, filled, 0, 0);
            if (r == Double.POSITIVE_INFINITY) {
                return FINISHED;
            }
            if (!Double.isNaN(r)) {
                step.hitLen = r;
                return HIT;
            }
            step.exitPlane = exitPlane(tx1, ty1, tz1);
            return MISS;
        }
        // The ray only touches this node along an edge or at a corner, and rounding made the
        // interval empty. Either the ray ends there, or it continues into the node on the other
        // side of that edge; either way there is nothing to test in this node.
        if (tmin >= targetLen) {
            return FINISHED;
        }
        step.exitPlane = exitPlane(tx1, ty1, tz1);
        return MISS;
    }

    /**
     * Traces the segment from (fx, fy, fz) to (tx, ty, tz). Returns true if it hit a solid block;
     * the hit position is then written to {@code hitOut} at {@code hitIndex}, if the array is not null.
     * The two points must be inside 0 <= y < 384, as must be the segment between them. A segment
     * of no length is a point, which is a hit if it is inside a block; a coordinate that is not a
     * finite number is refused.
     */
    static boolean raytrace(NetherPathfinder ctx, double fx, double fy, double fz, double tx, double ty, double tz, int fakeChunkMode, double[] hitOut, int hitIndex) {
        final double vx = tx - fx;
        final double vy = ty - fy;
        final double vz = tz - fz;
        final double targetLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (!(targetLen < Double.POSITIVE_INFINITY)) {
            // NaN, or an infinite coordinate: dividing by it would make the direction NaN and the
            // walk below would step from node to node for ever. The native library did just that.
            throw new IllegalArgumentException("ray with a coordinate that is not a finite number: ("
                    + fx + ", " + fy + ", " + fz + ") to (" + tx + ", " + ty + ", " + tz + ")");
        }
        if (targetLen == 0.0) {
            // A point. The same division would make the direction NaN here too, and the native
            // library exited the process. A ray that starts inside a block reports a hit at its
            // origin, so a point does the same, and one in the air hits nothing.
            final int bx = BlockPos.floor(fx);
            final int by = BlockPos.floor(fy);
            final int bz = BlockPos.floor(fz);
            final Chunk chunk = ctx.getRealChunkFromCacheOrFakeChunkMaybeGen(bx >> 4, bz >> 4, fakeChunkMode);
            if (by < 0 || by >= Chunk.HEIGHT || !chunk.isSolid(bx & 15, by, bz & 15)) {
                return false;
            }
            if (hitOut != null) {
                hitOut[hitIndex] = fx;
                hitOut[hitIndex + 1] = fy;
                hitOut[hitIndex + 2] = fz;
            }
            return true;
        }
        final double dx = vx / targetLen;
        final double dy = vy / targetLen;
        final double dz = vz / targetLen;
        int a = 0;
        if (dx < 0.0) a |= 4;
        if (dy < 0.0) a |= 2;
        if (dz < 0.0) a |= 1;

        int nx = BlockPos.floor(fx) & ~15;
        int ny = BlockPos.floor(fy) & ~15;
        int nz = BlockPos.floor(fz) & ~15;
        final Step step = new Step();
        while (true) {
            final Chunk chunk = ctx.getRealChunkFromCacheOrFakeChunkMaybeGen(nx >> 4, nz >> 4, fakeChunkMode);
            final long[] slab = chunk.slab(ny);
            // reflect around the node's centre so the traversal only has to walk positive directions
            double rox = fx, roy = fy, roz = fz, rdx = dx, rdy = dy, rdz = dz;
            if ((a & 4) != 0) { rox = reflect(fx, nx + 8); rdx = -dx; }
            if ((a & 2) != 0) { roy = reflect(fy, ny + 8); rdy = -dy; }
            if ((a & 1) != 0) { roz = reflect(fz, nz + 8); rdz = -dz; }
            final int result = raytrace16(a, rox, roy, roz, rdx, rdy, rdz, targetLen, nx, ny, nz, slab, chunk.filled(ny), step);
            if (result == FINISHED) {
                return false;
            }
            if (result == HIT) {
                if (hitOut != null) {
                    if (step.hitLen < 0.0) {
                        // the origin was inside an occupied block, so the hit is the origin
                        hitOut[hitIndex] = fx;
                        hitOut[hitIndex + 1] = fy;
                        hitOut[hitIndex + 2] = fz;
                    } else {
                        hitOut[hitIndex] = fx + dx * step.hitLen;
                        hitOut[hitIndex + 1] = fy + dy * step.hitLen;
                        hitOut[hitIndex + 2] = fz + dz * step.hitLen;
                    }
                }
                return true;
            }
            switch (step.exitPlane) {
                case PLANE_XY:
                    nz += (a & 1) != 0 ? -16 : 16;
                    break;
                case PLANE_XZ:
                    ny += (a & 2) != 0 ? -16 : 16;
                    break;
                default:
                    nx += (a & 4) != 0 ? -16 : 16;
                    break;
            }
        }
    }

    private static int lastVisibleNode(NetherPathfinder ctx, List<BlockPos> path, int currentNode) {
        if (currentNode == path.size() - 1) {
            return currentNode;
        }
        final BlockPos fromBlock = path.get(currentNode);
        int lastVisible = currentNode + 1; // can assume that the next node is always visible from the previous
        for (int i = lastVisible + 1; i < path.size(); i++) {
            final BlockPos currentBlock = path.get(i);
            if (fromBlock.equals(currentBlock)) continue; // the pathfinder can produce 2 consecutive equal points and that breaks the raytracer
            if (raytrace(ctx, fromBlock.x, fromBlock.y, fromBlock.z, currentBlock.x, currentBlock.y, currentBlock.z, NetherPathfinder.CACHE_MISS_GENERATE, null, 0)) {
                return lastVisible;
            }
            lastVisible = i;
        }
        return lastVisible;
    }

    /** Drops the points of a path that are visible from an earlier one. */
    static List<BlockPos> refine(NetherPathfinder ctx, List<BlockPos> path) {
        final List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < path.size(); i = lastVisibleNode(ctx, path, i)) {
            out.add(path.get(i));
            if (i == path.size() - 1) break;
        }
        return out;
    }
}
