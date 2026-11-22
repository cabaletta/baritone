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

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChunkGeneratorTest {

    @Test
    public void everyChunkMatchesTheNativeGeneratorBitForBit() throws Exception {
        final List<String[]> expected = Oracle.chunkHashes();
        assertEquals(25 * 25, expected.size());
        final ChunkGeneratorHell generator = ChunkGeneratorHell.fromSeed(Oracle.SEED);
        int wrong = 0;
        for (String[] line : expected) {
            final int x = Integer.parseInt(line[0]);
            final int z = Integer.parseInt(line[1]);
            final long hash = Long.parseUnsignedLong(line[2], 16);
            if (Oracle.fnv1a(generator.generateChunk(x, z).toBytes()) != hash) wrong++;
        }
        assertEquals("chunks that differ from the native generator", 0, wrong);
    }

    @Test
    public void generationIsDeterministic() {
        final ChunkGeneratorHell a = ChunkGeneratorHell.fromSeed(Oracle.SEED);
        final ChunkGeneratorHell b = ChunkGeneratorHell.fromSeed(Oracle.SEED);
        assertEquals(Oracle.fnv1a(a.generateChunk(100, -7).toBytes()), Oracle.fnv1a(b.generateChunk(100, -7).toBytes()));
    }
}
