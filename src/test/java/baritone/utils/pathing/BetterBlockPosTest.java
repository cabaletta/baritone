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

import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BetterBlockPosTest {

    // disabled since this is a benchmark, not really a test. also including it makes the tests take 50 seconds for no reason
    /*@Test
    public void benchMulti() {
        System.out.println("Benching up()");
        for (int i = 0; i < 10; i++) {
            // eliminate any advantage to going first
            benchOne();
        }
        System.out.println("Benching up(int)");
        for (int i = 0; i < 10; i++) {
            // eliminate any advantage to going first
            benchN();
            assertTrue(i<10);
        }
    }*/

    /**
     * Make sure BetterBlockPos behaves just like BlockPos
     */
    @Test
    public void testSimple() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BetterBlockPos better = new BetterBlockPos(1, 2, 3);
        assertEquals(pos, better);
        assertEquals(pos.up(), better.up());
        assertEquals(pos.down(), better.down());
        assertEquals(pos.north(), better.north());
        assertEquals(pos.south(), better.south());
        assertEquals(pos.east(), better.east());
        assertEquals(pos.west(), better.west());
        for (EnumFacing dir : EnumFacing.values()) {
            assertEquals(pos.offset(dir), better.offset(dir));
            assertEquals(pos.offset(dir, 0), pos);
            assertEquals(better.offset(dir, 0), better);
            for (int i = -10; i < 10; i++) {
                assertEquals(pos.offset(dir, i), better.offset(dir, i));
            }
            assertTrue(better.offset(dir, 0) == better);
        }
        for (int i = -10; i < 10; i++) {
            assertEquals(pos.up(i), better.up(i));
            assertEquals(pos.down(i), better.down(i));
            assertEquals(pos.north(i), better.north(i));
            assertEquals(pos.south(i), better.south(i));
            assertEquals(pos.east(i), better.east(i));
            assertEquals(pos.west(i), better.west(i));
        }
        assertTrue(better.offset(null, 0) == better);
    }

    public void benchOne() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BetterBlockPos pos2 = new BetterBlockPos(1, 2, 3);
        try {
            Thread.sleep(1000); // give GC some time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long before1 = System.nanoTime() / 1000000L;
        for (int i = 0; i < 1000000; i++) {
            pos.up();
        }
        long after1 = System.nanoTime() / 1000000L;
        try {
            Thread.sleep(1000); // give GC some time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long before2 = System.nanoTime() / 1000000L;
        for (int i = 0; i < 1000000; i++) {
            pos2.up();
        }
        long after2 = System.nanoTime() / 1000000L;
        System.out.println((after1 - before1) + " " + (after2 - before2));
    }

    public void benchN() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BetterBlockPos pos2 = new BetterBlockPos(1, 2, 3);
        try {
            Thread.sleep(1000); // give GC some time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long before1 = System.nanoTime() / 1000000L;
        for (int i = 0; i < 1000000; i++) {
            pos.up(0);
            pos.up(1);
            pos.up(2);
            pos.up(3);
            pos.up(4);
        }
        long after1 = System.nanoTime() / 1000000L;
        try {
            Thread.sleep(1000); // give GC some time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long before2 = System.nanoTime() / 1000000L;
        for (int i = 0; i < 1000000; i++) {
            pos2.up(0);
            pos2.up(1);
            pos2.up(2);
            pos2.up(3);
            pos2.up(4);
        }
        long after2 = System.nanoTime() / 1000000L;
        System.out.println((after1 - before1) + " " + (after2 - before2));
    }
}
