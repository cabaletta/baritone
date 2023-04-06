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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaceTest {
    @Test
    public void testOpts() {
        for (int i = 0; i < Face.OPTS.size(); i++) {
            assertEquals(i, Face.OPTS.indexOf(Face.OPTS.get(i)));
            assertEquals(i, (int) Face.OPTS.get(i).map(face -> face.index).orElse(Face.NUM_FACES));
        }
    }

    @Test
    public void ensureValid() {
        for (Face face : Face.VALUES) {
            assertEquals(face.x, face.toMC().getXOffset());
            assertEquals(face.y, face.toMC().getYOffset());
            assertEquals(face.z, face.toMC().getZOffset());
        }
        assertEquals(Face.UP.offset, BetterBlockPos.toLong(0, 1, 0));
        assertEquals(Face.DOWN.offset, BetterBlockPos.toLong(0, -1, 0));
        assertEquals(Face.NORTH.offset, BetterBlockPos.toLong(0, 0, -1));
        assertEquals(Face.SOUTH.offset, BetterBlockPos.toLong(0, 0, 1));
        assertEquals(Face.EAST.offset, BetterBlockPos.toLong(1, 0, 0));
        assertEquals(Face.WEST.offset, BetterBlockPos.toLong(-1, 0, 0));
    }
}
