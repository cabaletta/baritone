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

package baritone.utils.locate;

import net.minecraft.core.BlockPos;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BiomeDestinationSearchTest {
    @Test
    public void workPerTickAndCandidateCountAreBounded() {
        AtomicInteger checks = new AtomicInteger();
        Set<BlockPos> visited = new HashSet<>();
        BiomeDestinationSearch search = new BiomeDestinationSearch(BlockPos.ZERO, new BlockPos(0, 1, 0), 0, 2, pos -> {
            checks.incrementAndGet();
            assertTrue(visited.add(pos));
            return true;
        });
        assertFalse(search.tick());
        assertEquals(BiomeDestinationSearch.CHECKS_PER_TICK, checks.get());
        assertTrue(search.tick());
        assertEquals(33 * 33 * 3, checks.get());
        assertEquals(16, search.candidates().size());
        assertEquals(new BlockPos(0, 1, 0), search.candidates().getFirst());
        assertTrue(search.tick());
        assertEquals(33 * 33 * 3, checks.get());
    }

    @Test
    public void preservesNegativeCoordinatesAndUndergroundHeight() {
        BlockPos target = new BlockPos(-10, -32, -10);
        BiomeDestinationSearch search = new BiomeDestinationSearch(new BlockPos(-16, 0, -16), target,
                -34, -30, target::equals);
        while (!search.tick()) { }
        assertEquals(java.util.List.of(target), search.candidates());
    }
}
