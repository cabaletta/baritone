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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BaritoneRegionTest {

    /**
     * A nether region with one chunk at (3, 5), written the way Baritone's CachedRegion writes
     * one: the magic, then for each of the 32 by 32 chunks a byte saying whether it is present
     * and, if it is, the chunk's two bits per block as {@code BitSet.toByteArray()} lays them
     * out -- bit n in byte n / 8 at position n % 8, the lowest bit first. Block (x, y, z) owns
     * bits {@code (x << 1) | (z << 5) | (y << 9)} and the one after, CachedChunk.getPositionIndex.
     * The chunk has block (1, 2, 3) solid (both bits) and block (4, 2, 3) water (one bit), which
     * the pathfinder also flies around.
     */
    private static byte[] region() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bytes); DataOutputStream out = new DataOutputStream(gz)) {
            out.writeInt(456022911);
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (x == 3 && z == 5) {
                        out.writeByte(1);
                        final int size = 2 * 16 * 16 * 256;
                        final BitSet bits = new BitSet(size);
                        bits.set(positionIndex(1, 2, 3));
                        bits.set(positionIndex(1, 2, 3) + 1);
                        bits.set(positionIndex(4, 2, 3));
                        final byte[] data = new byte[size / 8];
                        final byte[] written = bits.toByteArray();
                        System.arraycopy(written, 0, data, 0, written.length); // CachedRegion pads with zeros
                        out.write(data);
                    } else {
                        out.writeByte(0);
                    }
                }
            }
        }
        return bytes.toByteArray();
    }

    private static int positionIndex(int x, int y, int z) {
        return (x << 1) | (z << 5) | (y << 9);
    }

    @Test
    public void parsesAChunk() throws Exception {
        final Chunk[] found = new Chunk[1];
        final int[] at = new int[2];
        try (java.io.InputStream in = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(region()))) {
            BaritoneRegion.parse(in, -1, 2, NetherPathfinder.Dimension.NETHER, (x, z, chunk) -> {
                found[0] = chunk;
                at[0] = x;
                at[1] = z;
            });
        }
        assertNotNull(found[0]);
        assertEquals(3 - 32, at[0]);
        assertEquals(5 + 64, at[1]);
        assertTrue(found[0].isSolid(1, 2, 3));
        assertTrue(found[0].isSolid(4, 2, 3));
        // the other three blocks of each aligned run of four along x are air: a reader that takes
        // a byte's bits from the top down mirrors every such run, and would put these at 2 and 7
        assertFalse(found[0].isSolid(2, 2, 3));
        assertFalse(found[0].isSolid(7, 2, 3));
        assertFalse(found[0].isSolid(3, 2, 1));
        assertTrue(found[0].isEmptyX16(64));
    }

    @Test
    public void aSearchLoadsTheRegionItReaches() throws Exception {
        final Path dir = Files.createTempDirectory("np-region");
        final Path regions = dir.resolve("the_nether_128").resolve("cache");
        Files.createDirectories(regions);
        Files.write(regions.resolve("r.0.0.bcr"), region());
        try {
            final NetherPathfinder ctx = new NetherPathfinder(1, regions.toString(), NetherPathfinder.Dimension.NETHER, 128);
            assertNull(ctx.getChunk(3, 5));
            assertTrue(ctx.tryLoadRegion(20, 20) > 0);
            assertNotNull(ctx.getChunk(3, 5));
            assertTrue(ctx.hasChunkFromCaller(3, 5));
            assertTrue(ctx.getChunk(3, 5).isSolid(1, 2, 3));
            // a region is only read once, and one that does not exist costs nothing
            assertEquals(0, ctx.tryLoadRegion(21, 21));
            assertEquals(0, ctx.tryLoadRegion(100, 100));
            // a chunk the game gave before the file is read wins
            final NetherPathfinder again = new NetherPathfinder(1, regions.toString(), NetherPathfinder.Dimension.NETHER, 128);
            final Chunk mine = again.allocateAndInsertChunk(3, 5);
            again.tryLoadRegion(0, 0);
            assertEquals(mine, again.getChunk(3, 5));
            // and a search reaching the region loads it by itself
            final NetherPathfinder searched = new NetherPathfinder(1, regions.toString(), NetherPathfinder.Dimension.NETHER, 128);
            searched.pathFind(0, 60, 0, 200, 60, 200, true, false, 1000, true, 8.0);
            assertNotNull(searched.getChunk(3, 5));
        } finally {
            Files.deleteIfExists(regions.resolve("r.0.0.bcr"));
            Files.deleteIfExists(regions);
            Files.deleteIfExists(regions.getParent());
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void aCulledRegionIsReadAgain() throws Exception {
        final Path dir = Files.createTempDirectory("np-cull");
        final Path regions = dir.resolve("the_nether_128").resolve("cache");
        Files.createDirectories(regions);
        Files.write(regions.resolve("r.0.0.bcr"), region());
        try {
            final NetherPathfinder ctx = new NetherPathfinder(1, regions.toString(), NetherPathfinder.Dimension.NETHER, 128);
            ctx.tryLoadRegion(3, 5);
            assertNotNull(ctx.getChunk(3, 5));
            // fly far away, the chunk gets culled
            ctx.cullFarChunks(10000, 10000, 512);
            assertNull(ctx.getChunk(3, 5));
            // and coming back reads the file again instead of believing it already has
            ctx.tryLoadRegion(3, 5);
            assertNotNull(ctx.getChunk(3, 5));
            assertTrue(ctx.getChunk(3, 5).isSolid(1, 2, 3));
            // a chunk that survives the cull stays the one we had
            final NetherPathfinder near = new NetherPathfinder(1, regions.toString(), NetherPathfinder.Dimension.NETHER, 128);
            near.tryLoadRegion(3, 5);
            near.cullFarChunks(3, 5, 512);
            final Chunk kept = near.getChunk(3, 5);
            near.tryLoadRegion(3, 5);
            assertEquals(kept, near.getChunk(3, 5));
        } finally {
            Files.deleteIfExists(regions.resolve("r.0.0.bcr"));
            Files.deleteIfExists(regions);
            Files.deleteIfExists(regions.getParent());
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void directoryNamesDecideTheDimension() {
        assertEquals(NetherPathfinder.Dimension.NETHER, BaritoneRegion.dimensionOf("/a/b/the_nether_128/cache"));
        assertEquals(NetherPathfinder.Dimension.OVERWORLD, BaritoneRegion.dimensionOf("/a/b/overworld_384/cache"));
        assertEquals(NetherPathfinder.Dimension.END, BaritoneRegion.dimensionOf("/a/b/the_end_256/cache"));
        assertNull(BaritoneRegion.dimensionOf("/a/b/somewhere/cache"));
    }
}
