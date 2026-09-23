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

package baritone.process.elytra;

import baritone.api.utils.BetterBlockPos;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class UnpackedSegmentTest {

    // one letter per spot on a line, so the test reads like the comment in collect
    private static List<BetterBlockPos> path(String letters) {
        return letters.chars().mapToObj(c -> new BetterBlockPos(c - 'A', 64, 0)).collect(Collectors.toList());
    }

    private static List<BetterBlockPos> collect(String letters) {
        return new UnpackedSegment(path(letters).stream(), true).collect();
    }

    @Test
    public void keepsTheEndAfterALoop() {
        assertEquals(path("ABDC"), collect("ABCBDC"));
    }

    @Test
    public void cutsLoops() {
        assertEquals(path("ABE"), collect("ABCDBE"));
        assertEquals(path("A"), collect("ABCA"));
        assertEquals(path("ACD"), collect("ABACBCD")); // loop inside what's left after a loop
    }

    @Test
    public void leavesStraightPathsAlone() {
        assertEquals(path("ABCDEF"), collect("ABCDEF"));
        assertEquals(path(""), collect(""));
    }

    @Test
    public void worksAcrossAppend() {
        UnpackedSegment seg = new UnpackedSegment(path("ABC").stream(), false).append(path("BDC").stream(), true);
        assertEquals(path("ABDC"), seg.collect());
        assertEquals(true, seg.isFinished());
    }
}
