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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ChunkTest {

    @Test
    public void setAndGetEveryBlockOfASection() {
        final Chunk chunk = new Chunk();
        for (int x = 0; x < 16; x++) for (int y = 48; y < 64; y++) for (int z = 0; z < 16; z++) {
            final boolean solid = ((x * 7 + y * 3 + z * 11) & 3) == 0;
            chunk.setBlock(x, y, z, solid);
        }
        for (int x = 0; x < 16; x++) for (int y = 48; y < 64; y++) for (int z = 0; z < 16; z++) {
            assertEquals(((x * 7 + y * 3 + z * 11) & 3) == 0, chunk.isSolid(x, y, z));
        }
        // clearing works too
        chunk.setBlock(3, 50, 4, false);
        assertFalse(chunk.isSolid(3, 50, 4));
        // untouched sections stay air and unallocated
        assertNull(chunk.section(0));
        assertFalse(chunk.isSolid(0, 0, 0));
    }

    @Test
    public void emptinessAtEveryLevel() {
        final Chunk chunk = new Chunk();
        assertTrue(chunk.isEmptyX16(64));
        chunk.setBlock(9, 70, 3, true);
        assertFalse(chunk.isEmptyX16(64));
        assertFalse(chunk.isEmptyX8(9, 70, 3));
        assertTrue(chunk.isEmptyX8(1, 70, 3));
        assertFalse(chunk.isEmptyX4(9, 70, 3));
        assertTrue(chunk.isEmptyX4(13, 70, 3));
        assertFalse(chunk.isEmptyX2(9, 70, 3));
        assertTrue(chunk.isEmptyX2(11, 70, 3));
        assertFalse(chunk.isEmpty(Size.X1, 9, 70, 3));
        assertTrue(chunk.isEmpty(Size.X1, 8, 70, 3));
        assertTrue(chunk.isEmpty(Size.X16, 0, 80, 0));

        // clearing a block empties its x8 and x16 again only once it was the last one there
        chunk.setBlock(10, 70, 3, true);
        chunk.setBlock(9, 70, 3, false);
        assertFalse(chunk.isEmptyX8(9, 70, 3));
        assertFalse(chunk.isEmptyX16(64));
        chunk.setBlock(10, 70, 3, false);
        // The blocks are gone, which the exact question sees. The x8 and x16 summaries are
        // allowed to lag behind a clear, and only ever towards "may hold a block".
        assertFalse(chunk.isSolid(9, 70, 3));
        assertFalse(chunk.isSolid(10, 70, 3));
        assertFalse(chunk.isEmptyX8(9, 70, 3));
        assertFalse(chunk.isEmptyX16(64));
        assertNotNull(chunk.section(64)); // the section stays allocated
    }

    @Test
    public void sharedChunksAreWhatTheySayAndReadOnly() {
        assertTrue(Chunk.SOLID.isSolid(0, 0, 0));
        assertTrue(Chunk.SOLID.isSolid(15, 383, 15));
        assertFalse(Chunk.SOLID.isEmptyX16(200));
        assertFalse(Chunk.AIR.isSolid(15, 383, 15));
        assertTrue(Chunk.AIR.isEmptyX16(200));
        try {
            Chunk.AIR.setBlock(0, 0, 0, true);
            throw new AssertionError("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // good
        }
    }

    @Test
    public void fillSection() {
        final Chunk chunk = new Chunk();
        chunk.fillSection(4, true);
        assertTrue(chunk.isSolid(0, 64, 0));
        assertTrue(chunk.isSolid(15, 79, 15));
        assertFalse(chunk.isSolid(0, 80, 0));
        assertFalse(chunk.isEmptyX8(15, 79, 15));
        assertTrue(chunk.isEmptyX8(0, 80, 0));
        chunk.fillSection(4, false);
        assertTrue(chunk.isEmptyX16(64));
        assertTrue(chunk.isEmptyX8(15, 79, 15));
        chunk.fillSection(4, true);
        chunk.setBlock(15, 79, 15, false);
        assertFalse("the section still has the other blocks", chunk.isEmptyX8(15, 79, 15));
    }

    @Test
    public void insertChunkDataUsesTheBlockStateContainerIndex() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        final boolean[] data = new boolean[16 * 16 * 256];
        data[(77 << 8) | (5 << 4) | 12] = true;
        ctx.insertChunkData(-3, 9, data);
        final Chunk chunk = ctx.getChunk(-3, 9);
        assertNotNull(chunk);
        assertTrue(chunk.isSolid(12, 77, 5));
        assertFalse(chunk.isSolid(5, 77, 12));
        assertTrue(ctx.hasChunkFromCaller(-3, 9));
        try {
            ctx.insertChunkData(0, 0, new boolean[10]);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // good
        }
    }

    @Test
    public void tableOperations() {
        final NetherPathfinder ctx = new NetherPathfinder(1, null, NetherPathfinder.Dimension.NETHER, 128);
        assertSame(Chunk.AIR, ctx.getChunkOrDefault(1, 1, false));
        assertSame(Chunk.SOLID, ctx.getChunkOrDefault(1, 1, true));
        assertNull(ctx.getChunk(1, 1));
        assertFalse(ctx.setChunkState(1, 1, true));

        final Chunk chunk = ctx.allocateAndInsertChunk(1, 1);
        assertSame(chunk, ctx.getChunk(1, 1));
        assertSame(chunk, ctx.getChunkOrDefault(1, 1, true));
        assertTrue(ctx.hasChunkFromCaller(1, 1));
        assertTrue(ctx.setChunkState(1, 1, false));
        assertFalse(ctx.hasChunkFromCaller(1, 1));
        // a fake chunk is a default to the search's real-chunk lookup
        assertSame(Chunk.SOLID, ctx.getRealChunkOrDefault(1, 1, true));
        assertSame(chunk, ctx.getChunkOrAir(1, 1).chunk);

        // replacing keeps the new one
        final Chunk again = ctx.allocateAndInsertChunk(1, 1);
        assertSame(again, ctx.getChunk(1, 1));

        ctx.allocateAndInsertChunk(40, 0);
        ctx.allocateAndInsertChunk(-40, 0);
        assertEquals(3, ctx.chunkCount());
        ctx.cullFarChunks(0, 0, 200); // 12 chunks
        assertEquals(1, ctx.chunkCount());
        assertNotNull(ctx.getChunk(1, 1));
        ctx.close();
        assertEquals(0, ctx.chunkCount());
    }

    @Test
    public void generatedChunksAreFakeUntilMarked() {
        final NetherPathfinder ctx = new NetherPathfinder(Oracle.SEED, null, NetherPathfinder.Dimension.NETHER, 128);
        final Chunk chunk = ctx.getOrGenChunk(0, 0);
        assertSame(chunk, ctx.getOrGenChunk(0, 0));
        assertFalse(ctx.hasChunkFromCaller(0, 0));
        assertSame(Chunk.AIR, ctx.getRealChunkFromCacheOrFakeChunkMaybeGen(0, 0, NetherPathfinder.CacheMiss.AIR));
        assertSame(chunk, ctx.getRealChunkFromCacheOrFakeChunkMaybeGen(0, 0, NetherPathfinder.CacheMiss.GENERATE));
        ctx.setChunkState(0, 0, true);
        assertSame(chunk, ctx.getRealChunkFromCacheOrFakeChunkMaybeGen(0, 0, NetherPathfinder.CacheMiss.AIR));
    }
}
