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

package baritone.api.pathing.movement;

public interface ActionCosts {

    /**
     * These costs are measured roughly in ticks btw
     */
    double WALK_ONE_BLOCK_COST = 20 / 4.317; // 4.633
    double WALK_ONE_IN_WATER_COST = 20 / 2.2; // 9.091
    double WALK_ONE_OVER_SOUL_SAND_COST = WALK_ONE_BLOCK_COST * 2; // 0.4 in BlockSoulSand but effectively about half
    double LADDER_UP_ONE_COST = 20 / 2.35; // 8.511
    double LADDER_DOWN_ONE_COST = 20 / 3.0; // 6.667
    double SNEAK_ONE_BLOCK_COST = 20 / 1.3; // 15.385
    double SPRINT_ONE_BLOCK_COST = 20 / 5.612; // 3.564
    double SPRINT_MULTIPLIER = SPRINT_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST; // 0.769
    /**
     * To walk off an edge you need to walk 0.5 to the edge then 0.3 to start falling off
     */
    double WALK_OFF_BLOCK_COST = WALK_ONE_BLOCK_COST * 0.8; // 3.706
    /**
     * To walk the rest of the way to be centered on the new block
     */
    double CENTER_AFTER_FALL_COST = WALK_ONE_BLOCK_COST - WALK_OFF_BLOCK_COST; // 0.927

    /**
     * don't make this Double.MAX_VALUE because it's added to other things, maybe other COST_INFs,
     * and that would make it overflow to negative
     */
    double COST_INF = 1000000;

    double[] FALL_N_BLOCKS_COST = generateFallNBlocksCost();

    double FALL_1_25_BLOCKS_COST = distanceToTicks(1.25);
    double FALL_0_25_BLOCKS_COST = distanceToTicks(0.25);
    /**
     * When you hit space, you get enough upward velocity to go 1.25 blocks
     * Then, you fall the remaining 0.25 to get on the surface, on block higher.
     * Since parabolas are symmetric, the amount of time it takes to ascend up from 1 to 1.25
     * will be the same amount of time that it takes to fall back down from 1.25 to 1.
     * And the same applies to the overall shape, if it takes X ticks to fall back down 1.25 blocks,
     * it will take X ticks to reach the peak of your 1.25 block leap.
     * Therefore, the part of your jump from y=0 to y=1.25 takes distanceToTicks(1.25) ticks,
     * and the sub-part from y=1 to y=1.25 takes distanceToTicks(0.25) ticks.
     * Therefore, the other sub-part, from y=0 to y-1, takes distanceToTicks(1.25)-distanceToTicks(0.25) ticks.
     * That's why JUMP_ONE_BLOCK_COST = FALL_1_25_BLOCKS_COST - FALL_0_25_BLOCKS_COST
     */
    double JUMP_ONE_BLOCK_COST = FALL_1_25_BLOCKS_COST - FALL_0_25_BLOCKS_COST;


    static double[] generateFallNBlocksCost() {
        double[] costs = new double[4097];
        for (int i = 0; i < 4097; i++) {
            costs[i] = distanceToTicks(i);
        }
        return costs;
    }

    static double velocity(int ticks) {
        return (Math.pow(0.98, ticks) - 1) * -3.92;
    }

    static double oldFormula(double ticks) {
        return -3.92 * (99 - 49.5 * (Math.pow(0.98, ticks) + 1) - ticks);
    }

    static double distanceToTicks(double distance) {
        if (distance == 0) {
            return 0; // Avoid 0/0 NaN
        }
        double tmpDistance = distance;
        int tickCount = 0;
        while (true) {
            double fallDistance = velocity(tickCount);
            if (tmpDistance <= fallDistance) {
                return tickCount + tmpDistance / fallDistance;
            }
            tmpDistance -= fallDistance;
            tickCount++;
        }
    }
}
