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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CachedRegionTest {

    @Test
    public void blockPosSaving() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    byte part1 = (byte) (z << 4 | x);
                    byte part2 = (byte) (y);
                    byte xz = part1;
                    int X = xz & 0x0f;
                    int Z = (xz >>> 4) & 0x0f;
                    int Y = part2 & 0xff;
                    if (x != X || y != Y || z != Z) {
                        System.out.println(x + " " + X + " " + y + " " + Y + " " + z + " " + Z);
                    }
                    assertEquals(x, X);
                    assertEquals(y, Y);
                    assertEquals(z, Z);
                }
            }
        }
    }
}
