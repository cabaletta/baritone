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

public class NavigableSurfaceBlipTest {
    @Test
    public void testBasicSolo() {
        CountingSurface surface = new CountingSurface(10, 10, 10);
        for (int i = 0; i <= Blip.FULL_BLOCK; i++) {
            surface.setBlock(0, 0, 0, FakeStates.BY_HEIGHT[i]);
            assertEquals(i != 0 && i != Blip.FULL_BLOCK, surface.surfaceSize(new BetterBlockPos(0, 0, 0)).isPresent());
        }
    }

    @Test
    public void testBasicConnected() {
        CountingSurface surface = new CountingSurface(10, 10, 10);
        surface.setBlock(1, 0, 0, FakeStates.SOLID);
        for (int i = 0; i <= Blip.FULL_BLOCK; i++) {
            surface.setBlock(0, 0, 0, FakeStates.BY_HEIGHT[i]);
            assertEquals(i == 0 ? 1 : 2, surface.requireSurfaceSize(1, 1, 0));
        }
        for (int i = 0; i <= Blip.FULL_BLOCK; i++) {
            surface.setBlock(0, 1, 0, FakeStates.BY_HEIGHT[i]);
            assertEquals(2, surface.requireSurfaceSize(1, 1, 0));
        }
        for (int i = 0; i <= Blip.FULL_BLOCK; i++) {
            surface.setBlock(0, 2, 0, FakeStates.BY_HEIGHT[i]);
            assertEquals(i <= Blip.JUMP - Blip.FULL_BLOCK ? 2 : 1, surface.requireSurfaceSize(1, 1, 0));
        }
    }

    @Test
    public void testStaircaseWithoutBlips() {
        CountingSurface surface = new CountingSurface(10, 10, 10);
        surface.setBlock(0, 0, 1, FakeStates.SOLID);
        int startY = 0;
        for (int x = 0; x < 5; x++) {
            // increase height of the (x,0) column as much as possible
            int y = startY;
            while (true) {
                surface.setBlock(x, y, 0, FakeStates.SOLID);
                if (!surface.connected(new BetterBlockPos(0, 1, 1), new BetterBlockPos(x, y + 1, 0))) {
                    surface.setBlock(x, y, 0, FakeStates.AIR);
                    y--;
                    break;
                }
                y++;
            }
            startY = y;
            assertEquals(x + 1, y); // only +1 because blocks start at zero, so (0, 1, 0) is already two blocks high, since (0, 0, 1) is the one block high starting point
        }
    }

    private static void fillColumnToHeight(CountingSurface surface, int x, int z, int yHeightBlips) {
        for (int i = 0; i < yHeightBlips / Blip.FULL_BLOCK; i++) {
            surface.setBlock(x, i, z, FakeStates.SOLID);
        }
        surface.setBlock(x, yHeightBlips / Blip.FULL_BLOCK, z, FakeStates.BY_HEIGHT[yHeightBlips % Blip.FULL_BLOCK]);
    }

    @Test
    public void testStaircaseWithFullBlockBlips() {
        CountingSurface surface = new CountingSurface(10, 10, 10);
        surface.setBlock(0, 0, 1, FakeStates.SOLID);
        int step = Blip.FULL_BLOCK;
        int startY = step;
        for (int x = 0; x < 5; x++) {
            // increase height of the (x,0) column as much as possible
            int y = startY;
            while (true) {
                fillColumnToHeight(surface, x, 0, y);
                if (!surface.connected(new BetterBlockPos(0, 1, 1), new BetterBlockPos(x, y / Blip.FULL_BLOCK, 0))) {
                    fillColumnToHeight(surface, x, 0, y - step);
                    y -= step;
                    break;
                }
                y += step;
            }
            startY = y;
            assertEquals((x + 2) * Blip.FULL_BLOCK, y); // +2 because we start at 16 blips due to (0,0,1) being full, so (0,1,0) being full on top of (0,0,0) being full means x=0 should be 32 blips high
        }
    }

    @Test
    public void testStaircaseBlips() {
        CountingSurface surface = new CountingSurface(10, 10, 10);
        surface.setBlock(0, 0, 1, FakeStates.SOLID);
        int startY = 1;
        for (int x = 0; x < 5; x++) {
            // increase height of the (x,0) column as much as possible
            int y = startY;
            while (true) {
                fillColumnToHeight(surface, x, 0, y);
                if (!surface.connected(new BetterBlockPos(0, 1, 1), new BetterBlockPos(x, y / Blip.FULL_BLOCK, 0))) {
                    fillColumnToHeight(surface, x, 0, y - 1);
                    y--;
                    break;
                }
                y++;
            }
            startY = y;
            assertEquals((x + 1) * Blip.JUMP + Blip.FULL_BLOCK, y); // we start on a full block at (0,0,1) then everything after that (inclusive of x=0, therefore the +1) adds one more jump
        }
    }
}
