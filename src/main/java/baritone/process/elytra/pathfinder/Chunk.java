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

import java.util.Arrays;

/**
 * A 16 by 384 by 16 column of blocks, one bit each, laid out as an octree so that a cube of 2, 4,
 * 8 or 16 blocks can be tested for emptiness with a few long reads. The layout is the native
 * library's: 24 sections of 16x16x16 blocks (512 bytes each); a section is 8 x8 cubes of 64 bytes; an
 * x8 is 8 x4 cubes of 8 bytes; an x4 is 8 x2 cubes of one byte; the 8 bits of that byte are the
 * blocks. A section in which no block has been set is not allocated. Beside the sections, one byte per
 * section says which of its x8 cubes hold a block, so that the question a search asks first and a ray
 * asks of every x16 and x8 it enters, whether a cube that big is empty, is a bit test rather than a
 * scan of 64 or 8 longs.
 * <p>
 * Nothing here is synchronized, as nothing was in the native library, and nothing needs to be:
 * the callers keep writers and readers apart. NetherPathfinderContext holds its write lock to pack
 * a chunk, apply a block update, cull the table or run a search that generates terrain, and its
 * read lock for every other search and for every ray, and a chunk generated or read from a region
 * file is complete before the table publishes it.
 */
public final class Chunk {

    public static final int HEIGHT = 384;
    public static final int SECTIONS = HEIGHT / 16;
    static final int SECTION_LONGS = 64; // 512 bytes
    static final int X8_BYTES = 64;
    static final int X4_BYTES = 8;

    /** A chunk with no blocks. Shared; writes to it throw. */
    public static final Chunk AIR = new Chunk(false);
    /** A chunk with every block solid. Shared; writes to it throw. */
    public static final Chunk SOLID = new Chunk(true);

    private final long[][] sections = new long[SECTIONS][];
    /** Bit i of entry s says whether x8 cube i of section s holds a block; a section that is null has 0. */
    private final int[] filled = new int[SECTIONS];
    private final boolean shared;

    public Chunk() {
        this.shared = false;
    }

    private Chunk(boolean solid) {
        this.shared = true;
        if (solid) {
            for (int i = 0; i < SECTIONS; i++) {
                this.sections[i] = new long[SECTION_LONGS];
                Arrays.fill(this.sections[i], -1L);
                this.filled[i] = 0xFF;
            }
        }
    }

    // index math, the same as the native Chunk.h
    static int x8Index(int x, int y, int z) {
        return ((x & 8) >> 1) | ((y & 8) >> 2) | ((z & 8) >> 3);
    }

    static int x4Index(int x, int y, int z) {
        return ((x & 4)) | ((y & 4) >> 1) | ((z & 4) >> 2);
    }

    static int x2Index(int x, int y, int z) {
        return ((x & 2) << 1) | ((y & 2)) | ((z & 2) >> 1);
    }

    static int bitIndex(int x, int y, int z) {
        return ((x & 1) << 2) | ((y & 1) << 1) | ((z & 1));
    }

    /** Byte offset, within its section, of the x2 cube that holds (x, y, z). */
    static int x2Offset(int x, int y, int z) {
        return x8Index(x, y, z) * X8_BYTES + x4Index(x, y, z) * X4_BYTES + x2Index(x, y, z);
    }

    /** The byte at {@code off} in a section. */
    static int byteAt(long[] section, int off) {
        return (int) (section[off >>> 3] >>> ((off & 7) << 3)) & 0xFF;
    }

    static boolean allZero(long[] section, int from, int longs) {
        long acc = 0;
        for (int i = 0; i < longs; i++) {
            acc |= section[from + i];
        }
        return acc == 0;
    }

    /** The section holding y, or null if nothing has been set in it (or y is outside the chunk). */
    long[] section(int y) {
        final int i = y >> 4;
        return i >= 0 && i < SECTIONS ? this.sections[i] : null;
    }

    /** Which x8 cubes of the section holding y hold a block, one bit each in x8Index order; 0 outside the chunk. */
    int filled(int y) {
        final int i = y >> 4;
        return i >= 0 && i < SECTIONS ? this.filled[i] : 0;
    }

    /** Coordinates are chunk relative: x and z in 0..15, y in 0..383. */
    public boolean isSolid(int x, int y, int z) {
        final long[] s = this.sections[y >> 4];
        if (s == null) {
            return false;
        }
        final int off = x2Offset(x, y, z);
        return ((s[off >>> 3] >>> (((off & 7) << 3) + bitIndex(x, y, z))) & 1L) != 0;
    }

    /**
     * Coordinates are chunk relative: x and z in 0..15, y in 0..383. A clear that empties its x8
     * cube also clears the cube's bit in filled, which costs a scan of the cube's eight longs; the
     * callers that fill a whole chunk only ever set blocks, so they never pay it.
     */
    public void setBlock(int x, int y, int z, boolean solid) {
        if (this.shared) {
            throw new UnsupportedOperationException("the shared air and solid chunks are read only");
        }
        long[] s = this.sections[y >> 4];
        if (s == null) {
            if (!solid) {
                return;
            }
            s = this.sections[y >> 4] = new long[SECTION_LONGS];
        }
        final int off = x2Offset(x, y, z);
        final long mask = 1L << (((off & 7) << 3) + bitIndex(x, y, z));
        final int x8 = x8Index(x, y, z);
        if (solid) {
            this.filled[y >> 4] |= 1 << x8;
            s[off >>> 3] |= mask;
        } else {
            s[off >>> 3] &= ~mask;
            if (allZero(s, x8 * (X8_BYTES / 8), X8_BYTES / 8)) {
                this.filled[y >> 4] &= ~(1 << x8);
            }
        }
    }

    /** Sets every block of one 16x16x16 section (0..23) at once. */
    public void fillSection(int section, boolean solid) {
        if (this.shared) {
            throw new UnsupportedOperationException("the shared air and solid chunks are read only");
        }
        if (!solid) {
            this.filled[section] = 0;
            this.sections[section] = null;
            return;
        }
        long[] s = this.sections[section];
        if (s == null) {
            s = this.sections[section] = new long[SECTION_LONGS];
        }
        this.filled[section] = 0xFF;
        Arrays.fill(s, -1L);
    }

    public boolean isEmpty(Size size, int x, int y, int z) {
        switch (size) {
            case X1: return !isSolid(x, y, z);
            case X2: return isEmptyX2(x, y, z);
            case X4: return isEmptyX4(x, y, z);
            case X8: return isEmptyX8(x, y, z);
            default: return isEmptyX16(y);
        }
    }

    public boolean isEmptyX16(int y) {
        return this.filled[y >> 4] == 0;
    }

    public boolean isEmptyX8(int x, int y, int z) {
        return (this.filled[y >> 4] & (1 << x8Index(x, y, z))) == 0;
    }

    public boolean isEmptyX4(int x, int y, int z) {
        final long[] s = this.sections[y >> 4];
        return s == null || s[x8Index(x, y, z) * (X8_BYTES / 8) + x4Index(x, y, z)] == 0;
    }

    public boolean isEmptyX2(int x, int y, int z) {
        final long[] s = this.sections[y >> 4];
        return s == null || byteAt(s, x2Offset(x, y, z)) == 0;
    }

    /** The chunk's 12288 bytes in the native layout (little endian words), for hashing in tests. */
    byte[] toBytes() {
        final byte[] out = new byte[SECTIONS * SECTION_LONGS * 8];
        for (int i = 0; i < SECTIONS; i++) {
            final long[] s = this.sections[i];
            if (s == null) {
                continue;
            }
            for (int j = 0; j < SECTION_LONGS; j++) {
                long v = s[j];
                for (int k = 0; k < 8; k++) {
                    out[(i * SECTION_LONGS + j) * 8 + k] = (byte) (v >>> (k * 8));
                }
            }
        }
        return out;
    }
}
