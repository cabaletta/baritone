package baritone.bot.pathing.movement;

import org.junit.Test;

import static baritone.bot.pathing.movement.ActionCostsButOnlyTheOnesThatMakeMickeyDieInside.FALL_N_BLOCKS_COST;
import static org.junit.Assert.assertEquals;

public class ActionCostsButOnlyTheOnesThatMakeMickeyDieInsideTest {
    @Test
    public void testFallNBlocksCost() {
        assertEquals(FALL_N_BLOCKS_COST.length, 257); // Fall 0 blocks through fall 256 blocks
        for (int i = 0; i < 256; i++) {
            double t = FALL_N_BLOCKS_COST[i];
            double fallDistance = 3.92 * (99 - 49.50 * (Math.pow(0.98, t) + 1) - t);
            assertEquals(fallDistance, -i, 0.000000000001); // If you add another 0 the test fails at i=43 LOL
        }
    }

}