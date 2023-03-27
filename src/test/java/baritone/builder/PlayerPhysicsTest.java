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

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerPhysicsTest {

    @Test
    public void testBasic() {
        assertEquals(16, PlayerPhysics.highestCollision(BlockStateCachedData.SCAFFOLDING, BlockStateCachedData.SCAFFOLDING));

        Column normal = new Column();
        normal.underneath = BlockStateCachedData.SCAFFOLDING;
        normal.feet = BlockStateCachedData.AIR;
        normal.head = BlockStateCachedData.AIR;
        normal.above = BlockStateCachedData.AIR;
        normal.aboveAbove = BlockStateCachedData.SCAFFOLDING;
        normal.init();

        Column up = new Column();
        up.underneath = BlockStateCachedData.SCAFFOLDING;
        up.feet = BlockStateCachedData.SCAFFOLDING;
        up.head = BlockStateCachedData.AIR;
        up.above = BlockStateCachedData.AIR;
        up.aboveAbove = BlockStateCachedData.SCAFFOLDING;
        up.init();

        assertEquals(PlayerPhysics.Collision.VOXEL_LEVEL, PlayerPhysics.playerTravelCollides(normal, normal));
        assertEquals(PlayerPhysics.Collision.JUMP_TO_VOXEL_UP, PlayerPhysics.playerTravelCollides(normal, up));
    }

    private static final BlockStateCachedData[] BY_HEIGHT;

    static {
        BY_HEIGHT = new BlockStateCachedData[Blip.FULL_BLOCK + 1];
        for (int height = 1; height <= Blip.FULL_BLOCK; height++) {
            BY_HEIGHT[height] = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).fullyWalkableTop().collisionHeight(height * Blip.RATIO));
        }
        BY_HEIGHT[0] = BlockStateCachedData.AIR;
    }

    private static BlockStateCachedData[] makeColToHeight(int height) {
        height += Blip.FULL_BLOCK * 3; // i don't truck with negative division / modulo
        if (height < 0) {
            throw new IllegalStateException();
        }
        int fullBlocks = height / Blip.FULL_BLOCK;
        BlockStateCachedData[] ret = new BlockStateCachedData[7];
        for (int i = 0; i < fullBlocks; i++) {
            ret[i] = BlockStateCachedData.SCAFFOLDING;
        }
        ret[fullBlocks] = BY_HEIGHT[height % Blip.FULL_BLOCK];
        for (int i = fullBlocks + 1; i < ret.length; i++) {
            ret[i] = BlockStateCachedData.AIR;
        }
        return ret;
    }

    private static Column toCol(BlockStateCachedData[] fromCol) {
        Column col = new Column();
        col.underneath = fromCol[2];
        col.feet = fromCol[3];
        col.head = fromCol[4];
        col.above = fromCol[5];
        col.aboveAbove = fromCol[6];
        col.init();
        return col;
    }

    @Test
    public void testMakeCol() {
        for (int i = 0; i < Blip.FULL_BLOCK; i++) {
            Column col = toCol(makeColToHeight(i));
            assertTrue(col.standing());
            assertEquals(i, (int) col.feetBlips);
        }
        assertFalse(toCol(makeColToHeight(-1)).standing());
        assertFalse(toCol(makeColToHeight(Blip.FULL_BLOCK)).standing());
    }

    @Test
    public void testPlayerPhysics() {
        for (int startHeight = 0; startHeight < Blip.FULL_BLOCK; startHeight++) {
            for (int startCeil = 5; startCeil <= 7; startCeil++) {
                BlockStateCachedData[] fromCol = makeColToHeight(startHeight);
                if (startCeil < fromCol.length) {
                    fromCol[startCeil] = BlockStateCachedData.SCAFFOLDING;
                }
                Column from = toCol(fromCol);
                assertEquals(!from.standing(), startCeil == 5 && startHeight > 3);
                if (!from.standing()) {
                    System.out.println("Not standing " + startHeight + " " + startCeil);
                    continue;
                }

                for (int endHeight = startHeight - 3 * Blip.PER_BLOCK; endHeight <= startHeight + 3 * Blip.PER_BLOCK; endHeight++) {
                    for (int endCeil = 5; endCeil <= 7; endCeil++) {
                        BlockStateCachedData[] toCol = makeColToHeight(endHeight);
                        if (endCeil < toCol.length) {
                            toCol[endCeil] = BlockStateCachedData.SCAFFOLDING;
                        }
                        Column to = toCol(toCol);
                        int endVoxel = (endHeight + Blip.FULL_BLOCK * 10) / Blip.FULL_BLOCK - 10; // hate negative division rounding to zero, punch negative division rounding to zero

                        PlayerPhysics.Collision col = PlayerPhysics.playerTravelCollides(from, to);
                        Integer dy = PlayerPhysics.bidirectionalPlayerTravel(from, to, toCol[1], toCol[0]);
                        System.out.println(startHeight + " " + startCeil + " " + endCeil + " " + (endHeight - startHeight) + " " + col + " " + endVoxel + " " + dy);

                        int minCeilRelative = Math.min(startCeil, endCeil) - 3;
                        int maxBlip = Math.max(startHeight, endHeight);
                        if (maxBlip + Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE > minCeilRelative * Blip.PER_BLOCK) {
                            assertEquals(PlayerPhysics.Collision.BLOCKED, col);
                            continue;
                        }

                        assertEquals(col == PlayerPhysics.Collision.BLOCKED, endHeight > startHeight + Blip.JUMP);
                        assertEquals(col == PlayerPhysics.Collision.FALL, endVoxel < 0);
                        assertEquals(dy == null, endHeight > startHeight + Blip.JUMP || endHeight < startHeight - Blip.JUMP);

                        if (dy != null) {
                            assertEquals(endVoxel, (int) dy);
                            Integer reverse = -dy;
                            BlockStateCachedData[] shiftedTo = shift(toCol, reverse);
                            BlockStateCachedData[] shiftedFrom = shift(fromCol, reverse);
                            // make sure it's actually bidirectional
                            assertEquals(reverse, PlayerPhysics.bidirectionalPlayerTravel(toCol(shiftedTo), toCol(shiftedFrom), shiftedFrom[1], shiftedFrom[0]));
                        }
                        if (col != PlayerPhysics.Collision.BLOCKED && col != PlayerPhysics.Collision.FALL) {
                            assertEquals(col.voxelVerticalOffset(), endVoxel);
                            assertEquals(col.requiresJump(), endHeight > startHeight + Blip.HALF_BLOCK);
                        }
                    }
                }
            }
        }
    }

    private static BlockStateCachedData[] shift(BlockStateCachedData[] col, int dy) {
        BlockStateCachedData[] ret = new BlockStateCachedData[col.length];
        for (int i = 0; i < ret.length; i++) {
            int j = i - dy;
            if (j >= 0 && j < col.length) {
                ret[i] = col[j];
            } else {
                ret[i] = BlockStateCachedData.OUT_OF_BOUNDS;
            }
        }
        return ret;
    }
}
