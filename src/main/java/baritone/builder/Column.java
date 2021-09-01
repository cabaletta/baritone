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

import java.util.stream.IntStream;

import static baritone.api.utils.BetterBlockPos.Y_MASK;
import static baritone.api.utils.BetterBlockPos.Y_SHIFT;

/**
 * A mutable class representing a 1x5x1 column of blocks
 * <p>
 * Mutable because allocations are not on the table for the core solver loop
 */
public class Column {

    public long pos;
    public BlockStateCachedData underneath;
    public BlockStateCachedData feet;
    public BlockStateCachedData head;
    public BlockStateCachedData above;
    public BlockStateCachedData aboveAbove;
    public PlayerPhysics.VoxelResidency voxelResidency;
    public Integer feetBlips;

    public void initFrom(long pos, WorldState worldState, SolverEngineInput engineInput) {
        this.pos = pos;
        this.underneath = engineInput.at((pos + DOWN_1) & BetterBlockPos.POST_ADDITION_MASK, worldState);
        this.feet = engineInput.at(pos, worldState);
        this.head = engineInput.at((pos + UP_1) & BetterBlockPos.POST_ADDITION_MASK, worldState);
        this.above = engineInput.at((pos + UP_2) & BetterBlockPos.POST_ADDITION_MASK, worldState);
        this.aboveAbove = engineInput.at((pos + UP_3) & BetterBlockPos.POST_ADDITION_MASK, worldState);
        this.voxelResidency = PlayerPhysics.canPlayerStand(underneath, feet);
        this.feetBlips = boxNullable(PlayerPhysics.determinePlayerRealSupportLevel(underneath, feet, voxelResidency));
    }

    public boolean playerCanExistAtFootBlip(int blipWithinFeet) {
        if (head.collidesWithPlayer) {
            return false;
        }
        if (PlayerPhysics.protrudesIntoThirdBlock(blipWithinFeet) && above.collidesWithPlayer) {
            return false;
        }
        return true;
    }

    public boolean okToSneakIntoHereAtHeight(int blips) {
        return playerCanExistAtFootBlip(blips) // no collision at head level
                && PlayerPhysics.highestCollision(underneath, feet) < blips; // and at foot level, we only collide strictly below where the feet will be
    }

    public boolean standing() {
        return feetBlips != null;
    }

    private static final long DOWN_1 = Y_MASK << Y_SHIFT;
    private static final long UP_1 = 1L << Y_SHIFT;
    private static final long UP_2 = 2L << Y_SHIFT;
    private static final long UP_3 = 3L << Y_SHIFT;

    static {
        if (DOWN_1 != BetterBlockPos.toLong(0, -1, 0)) {
            throw new IllegalStateException();
        }
        if (UP_1 != BetterBlockPos.toLong(0, 1, 0)) {
            throw new IllegalStateException();
        }
        if (UP_2 != BetterBlockPos.toLong(0, 2, 0)) {
            throw new IllegalStateException();
        }
        if (UP_3 != BetterBlockPos.toLong(0, 4, 0)) {
            throw new IllegalStateException();
        }
    }

    private static final Integer[] BLIPS = IntStream.range(-1, Blip.PER_BLOCK).boxed().toArray(Integer[]::new);

    static {
        BLIPS[0] = null;
    }

    private static Integer boxNullable(int blips) {
        return BLIPS[blips + 1]; // map -1 to [0] which is null
    }
}
