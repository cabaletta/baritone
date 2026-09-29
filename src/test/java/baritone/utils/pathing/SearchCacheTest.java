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

package baritone.utils.pathing;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SearchCacheTest {

    @Test
    public void clampAndRoundCapacity() {
        int[][] cases = {{Integer.MIN_VALUE, 10}, {0, 10}, {1024, 10}, {1025, 11},
                {16384, 14}, {16385, 15}, {65536, 16}, {Integer.MAX_VALUE, 16}};
        for (int[] pair : cases) {
            assertEquals(pair[1], SearchCache.bitsForSize(pair[0]));
        }
    }

    @Test
    public void reuseClearsBothMiningKeySets() {
        SearchCache.MiningBuffers first = SearchCache.acquireMining(16384);
        Arrays.fill(first.keys, 42L);
        Arrays.fill(first.fallingKeys, 43L);
        SearchCache.release(first);
        SearchCache.MiningBuffers next = SearchCache.acquireMining(16384);
        try {
            assertSame(first, next);
            for (int i = 0; i < next.keys.length; i++) {
                assertEquals(-1L, next.keys[i]);
                assertEquals(-1L, next.fallingKeys[i]);
            }
        } finally {
            SearchCache.release(next);
        }
    }

    @Test
    public void nestedMiningClaimKeepsOuterBuffer() {
        SearchCache.MiningBuffers outer = SearchCache.acquireMining(16384);
        try {
            outer.keys[0] = 42L;
            SearchCache.MiningBuffers inner = SearchCache.acquireMining(16384);
            try {
                assertNotSame(outer, inner);
                assertNotSame(outer.values, inner.values);
                inner.keys[0] = 43L;
                assertEquals(42L, outer.keys[0]);
            } finally {
                SearchCache.release(inner);
            }
        } finally {
            SearchCache.release(outer);
        }
        SearchCache.MiningBuffers next = SearchCache.acquireMining(16384);
        try {
            assertSame(outer, next);
        } finally {
            SearchCache.release(next);
        }
    }

    @Test
    public void blockReuseAndNestedResizeKeepOuterBuffer() {
        SearchCache.BlockBuffers outer = SearchCache.acquireBlocks(1024);
        try {
            Arrays.fill(outer.keys, 42L);
            SearchCache.BlockBuffers inner = SearchCache.acquireBlocks(65536);
            try {
                assertEquals(65536, inner.keys.length);
                assertNotSame(outer.values, inner.values);
                assertEquals(42L, outer.keys[0]);
            } finally {
                SearchCache.release(inner);
            }
        } finally {
            SearchCache.release(outer);
        }
        SearchCache.BlockBuffers next = SearchCache.acquireBlocks(1024);
        try {
            assertSame(outer, next);
            for (long key : next.keys) {
                assertEquals(-1L, key);
            }
        } finally {
            SearchCache.release(next);
        }
        SearchCache.BlockBuffers resized = SearchCache.acquireBlocks(1025);
        try {
            assertNotSame(outer, resized);
            assertEquals(2048, resized.keys.length);
        } finally {
            SearchCache.release(resized);
        }
    }

    @Test
    public void foreignReleaseCannotFreeActiveBuffers() throws Exception {
        SearchCache.MiningBuffers mining = SearchCache.acquireMining(1024);
        SearchCache.BlockBuffers blocks = SearchCache.acquireBlocks(1024);
        try {
            FutureTask<Void> other = new FutureTask<>(() -> {
                SearchCache.release(mining);
                SearchCache.release(blocks);
                SearchCache.MiningBuffers otherMining = SearchCache.acquireMining(1024);
                SearchCache.BlockBuffers otherBlocks = SearchCache.acquireBlocks(1024);
                try {
                    assertNotSame(mining, otherMining);
                    assertNotSame(blocks, otherBlocks);
                } finally {
                    SearchCache.release(otherMining);
                    SearchCache.release(otherBlocks);
                }
                return null;
            });
            new Thread(other).start();
            other.get(10, TimeUnit.SECONDS);
            SearchCache.MiningBuffers nestedMining = SearchCache.acquireMining(1024);
            SearchCache.BlockBuffers nestedBlocks = SearchCache.acquireBlocks(1024);
            try {
                assertNotSame(mining, nestedMining);
                assertNotSame(blocks, nestedBlocks);
            } finally {
                SearchCache.release(nestedMining);
                SearchCache.release(nestedBlocks);
            }
        } finally {
            SearchCache.release(mining);
            SearchCache.release(blocks);
        }
    }
}
