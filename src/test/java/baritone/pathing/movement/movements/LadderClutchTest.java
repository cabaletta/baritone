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

package baritone.pathing.movement.movements;

import org.junit.Test;

import static org.junit.Assert.*;

public class LadderClutchTest {

    // the closed form of y += vy, vy = (vy - 0.08) * 0.98 from vy = -0.0784: vy after n ticks is -3.92 * (1 - 0.98^(n+1))
    private static double expectedStart(double drop, int tick) {
        return drop - 3.92 * (tick - 0.98 * (1 - Math.pow(0.98, tick)) / 0.02);
    }

    @Test
    public void tickStartsFollowTheFall() {
        double[] ys = LadderClutch.tickStarts(40);
        // the first tick is exactly the floor we walked off of, and then the very first airborne move is 0.0784
        assertEquals(40, ys[0], 0);
        assertEquals(40 - 0.0784, ys[1], 1e-6);
        for (int i = 0; i < ys.length; i++) {
            assertEquals(expectedStart(40, i), ys[i], 1e-4);
        }
        // the last one is still above the floor, the one after it would be through it
        assertTrue(ys[ys.length - 1] > 0);
        assertTrue(expectedStart(40, ys.length) <= 0);
    }

    @Test
    public void plainFallCountsEverythingButTheLandingMove() {
        double[] ys = LadderClutch.tickStarts(10);
        // nothing placed: this is just the fall, and landing is what checks, so the last move isn't in the distance
        LadderClutch.Landing none = LadderClutch.play(10, LadderClutch.WALK_OFF_VY, 0, 0, 2, 999, 0);
        assertEquals(10 - ys[ys.length - 1], none.fall(), 1e-9);
        assertTrue(none.fall() > 3);
    }

    @Test
    public void landingCellClutchesAnyHeight() {
        // a vine in the cell we land in, any height. at speed it's the move onto the floor crossing the cell, nothing slows down
        for (int drop : new int[]{4, 5, 8, 12, 20, 30, 64, 100, 200}) {
            LadderClutch.Plan p = LadderClutch.plan(drop, 0b1, 4.5);
            assertNotNull("drop " + drop, p);
            assertEquals(0, p.cell());
            assertTrue(p.fall() <= LadderClutch.SAFE_FALL);
            assertTrue(p.place() >= LadderClutch.FIRST_PLACE);
        }
        // at terminal velocity the first tick that's close enough to click is the last or next to last
        double[] ys = LadderClutch.tickStarts(100);
        assertTrue(LadderClutch.plan(100, 0b1, 4.5).place() >= ys.length - 2);
    }

    @Test
    public void pickerHasToLandInsideTheCell() {
        // 13 blocks up the last tick starts 0.998 above the floor, right on the edge between cell 0 and cell 1. neither
        // the tick start rule nor the move rule can say for sure, so it won't bet on cell 0 alone
        double[] ys = LadderClutch.tickStarts(13);
        assertEquals(1, ys[ys.length - 1], 0.01);
        assertNull(LadderClutch.plan(13, 0b01, 4.5));
        // but a wall next to cell 1 is fine, that one has the whole last move through it
        LadderClutch.Plan p = LadderClutch.plan(13, 0b11, 4.5);
        assertNotNull(p);
        assertEquals(1, p.cell());
    }

    @Test
    public void wallHighUpIsNoGood() {
        // a vine 4 above the floor wipes the fall distance, but there's still 30 blocks of it to land with (well,
        // however much is left after that tick) so it's never safe
        assertNull(LadderClutch.plan(30, 0b10000, 4.5));
        // while the cells near the floor are
        assertNotNull(LadderClutch.plan(30, 0b00100, 4.5));
    }

    @Test
    public void noWallNoClutch() {
        assertNull(LadderClutch.plan(30, 0, 4.5));
    }

    @Test
    public void reachBoundsHowFastItCanBeDone() {
        // 4.5 gets there. 1.5 never does, the eye is 1.62 above the feet, so the ray to the floor would be longer than
        // that on the very last tick even if we were standing on it
        assertNotNull(LadderClutch.plan(30, 0b1, 4.5));
        assertNull(LadderClutch.plan(30, 0b1, 1.5));
        // the cell has to be at or below our feet
        assertTrue(LadderClutch.canReach(1.5, 1, 4.5));
        assertFalse(LadderClutch.canReach(1.5, 2, 4.5));
        // and the eye has to be near enough to the face, right at the edge of reach
        assertTrue(LadderClutch.canReach(2.5, 0, 4.5));
        assertFalse(LadderClutch.canReach(4.5, 0, 4.5));
    }

    @Test
    public void slidingStartsFromWhereItGrabbed() {
        // slow fall, a ladder 7 up: you grab it at once and ride it down, but then it's 7 more blocks and that kills you
        double fall = LadderClutch.play(10, LadderClutch.WALK_OFF_VY, 0, 0, 7, 0, LadderClutch.SLOP).fall();
        assertTrue(fall > LadderClutch.SAFE_FALL);
        // two up, and the drop off the bottom of it is nothing
        assertTrue(LadderClutch.play(10, LadderClutch.WALK_OFF_VY, 0, 0, 2, 0, LadderClutch.SLOP).fall() < 2.2);
    }

    @Test
    public void existingClimbablesAreGrabbedExactly() {
        // what "only grab when under 11 blocks" was approximating: nothing is skipped up to 11, then ticks start jumping
        // over cells
        for (int start = 5; start <= 11; start++) {
            for (int cell = 0; cell < start - 2; cell++) {
                assertTrue("start " + start + " cell " + cell, LadderClutch.grabs(start, cell));
            }
        }
        int skipped = 0;
        for (int cell = 0; cell < 90; cell++) {
            if (!LadderClutch.grabs(100, cell)) {
                skipped++;
            }
        }
        assertTrue(skipped > 30);
        // the last few cells under where we walked off are slow enough that nobody gets skipped
        for (int cell = 95; cell < 98; cell++) {
            assertTrue(LadderClutch.grabs(100, cell));
        }
    }
}
