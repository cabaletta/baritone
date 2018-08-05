package baritone.bot.pathing.movement;

import org.junit.Test;

import static baritone.bot.pathing.movement.ActionCostsButOnlyTheOnesThatMakeMickeyDieInside.FALL_N_BLOCKS_COST;
import static baritone.bot.pathing.movement.ActionCostsButOnlyTheOnesThatMakeMickeyDieInside.velocity;
import static org.junit.Assert.assertEquals;

public class ActionCostsButOnlyTheOnesThatMakeMickeyDieInsideTest {
    @Test
    public void testFallNBlocksCost() {
        assertEquals(FALL_N_BLOCKS_COST.length, 257); // Fall 0 blocks through fall 256 blocks
        for (int i = 0; i < 257; i++) {
            double blocks = ticksToBlocks(FALL_N_BLOCKS_COST[i]);
            assertEquals(blocks, i, 0.01);
        }
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