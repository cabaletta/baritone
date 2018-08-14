/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.utils.pathing;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

public class BetterBlockPosTest {

    @Test
    public void benchMulti() {
        for (int i = 0; i < 10; i++) {
            // eliminate any advantage to going first
            benchOne();
        }
    }

    public void benchOne() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BetterBlockPos pos2 = new BetterBlockPos(1, 2, 3);
        try {
            Thread.sleep(1000); // give GC some time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long before1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            pos.up();
        }
        long after1 = System.currentTimeMillis();
        try {
            Thread.sleep(1000); // give GC some time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long before2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            pos2.up();
        }
        long after2 = System.currentTimeMillis();
        System.out.println((after1 - before1) + " " + (after2 - before2));
    }
}