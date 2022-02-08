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

package baritone.pathing.movement;

import org.junit.Test;

import static baritone.api.pathing.movement.ActionCosts.*;
import static org.junit.Assert.assertEquals;

public class ActionCostsTest {

    @Test
    public void testFallNBlocksCost() {
        assertEquals(FALL_N_BLOCKS_COST.length, 4097); // Fall 0 blocks through fall 4096 blocks
        for (int i = 0; i < 4097; i++) {
            double blocks = ticksToBlocks(FALL_N_BLOCKS_COST[i]);
            assertEquals(blocks, i, 0.00000000001); // If you add another 0 the test fails at i=989 LOL
        }
        assertEquals(FALL_1_25_BLOCKS_COST, 6.2344, 0.00001);
        assertEquals(FALL_0_25_BLOCKS_COST, 3.0710, 0.00001);
        assertEquals(JUMP_ONE_BLOCK_COST, 3.1634, 0.00001);
    }

    public double ticksToBlocks(double ticks) {
        double fallDistance = 0;
        int integralComponent = (int) Math.floor(ticks);
        for (int tick = 0; tick < integralComponent; tick++) {
            fallDistance += velocity(tick);
        }
        double partialTickComponent = ticks - Math.floor(ticks);
        double finalPartialTickVelocity = velocity(integralComponent);
        double finalPartialTickDistance = finalPartialTickVelocity * partialTickComponent;
        fallDistance += finalPartialTickDistance;
        return fallDistance;
    }

}
