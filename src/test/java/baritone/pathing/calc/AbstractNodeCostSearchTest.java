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

package baritone.pathing.calc;

import org.junit.Test;

//import static org.junit.Assert.*;

public class AbstractNodeCostSearchTest {
    @Test
    public void testPosHash() {

        System.out.println(Long.toBinaryString(mix(0, 0)));
        System.out.println(Long.toBinaryString(mix(0, 1)));
        System.out.println(Long.toBinaryString(mix(1, 0)));
        System.out.println(Long.toBinaryString(mix(0, -1)));
        System.out.println(Long.toBinaryString(mix(-1, 0)));
        System.out.println(Long.toBinaryString(mix(1, -1)));
        System.out.println(Long.toBinaryString(mix(-1, 1)));

        System.out.println(Long.toBinaryString(mix(-1, -    1)));
        System.out.println(Long.toBinaryString(mix(-30000000, -1)));
        System.out.println(Long.toBinaryString(mix(-15000000, -1)));
        System.out.println(Long.toBinaryString(mix(30000000, -1)));
        System.out.println(Long.toBinaryString(mix(15000000, -1)));
        System.out.println(Long.toBinaryString(mix(0, -1)));

    }

    public long mix(int x, int z) {
        long xzmask = (1L << 26L) - 1; // 38 zero bits, 26 one bits
        return (((long) x) & xzmask) | ((((long) z) & xzmask) << 26);
    }
}