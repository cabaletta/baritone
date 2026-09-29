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

package baritone.cache;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class CachedChunkHeightMapTest {

    @BeforeClass
    public static void bootstrap() {
        // CachedChunk has a set of Blocks in it, so the registries have to exist
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // the old bit at a time version, kept as the reference
    private static int[] slow(BitSet data, int height) {
        int[] heightMap = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = height; y >= 0; y--) {
                    int i = CachedChunk.getPositionIndex(x, y, z);
                    if (data.get(i) || data.get(i + 1)) {
                        heightMap[z << 4 | x] = y;
                        break;
                    }
                }
            }
        }
        return heightMap;
    }

    private static void check(BitSet data, int height) {
        int[] fast = new int[256];
        CachedChunk.calculateHeightMap(data, height, fast);
        assertArrayEquals(slow(data, height), fast);
    }

    @Test
    public void matchesTheSlowWay() {
        Random rand = new Random(5501);
        for (int height : new int[]{128, 256, 384}) {
            check(new BitSet(), height); // all air
            for (int trial = 0; trial < 200; trial++) {
                BitSet data = new BitSet(CachedChunk.size(height));
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (rand.nextInt(8) == 0) {
                            continue; // leave some columns empty
                        }
                        int top = rand.nextInt(height);
                        for (int y = 0; y <= top; y++) {
                            if (y != top && rand.nextInt(3) == 0) {
                                continue; // caves
                            }
                            int i = CachedChunk.getPositionIndex(x, y, z);
                            // any of water (01), avoid (10), solid (11), including the top block
                            int type = 1 + rand.nextInt(3);
                            data.set(i, (type & 1) != 0);
                            data.set(i + 1, (type & 2) != 0);
                        }
                    }
                }
                check(data, height);
            }
            // the very top layer, where the trailing words matter
            BitSet top = new BitSet(CachedChunk.size(height));
            top.set(CachedChunk.getPositionIndex(15, height - 1, 15) + 1);
            top.set(CachedChunk.getPositionIndex(0, height - 1, 0));
            check(top, height);
        }
    }
}
