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
import baritone.builder.utils.com.github.btrekkie.connectivity.ConnGraph;

import java.util.OptionalInt;

public class NavigableSurface {
    // the encapsulation / separation of concerns is not great, but this is better for testing purposes than the fully accurate stuff in https://github.com/cabaletta/baritone/tree/builder-2/src/main/java/baritone/builder lol
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;

    private final boolean[][][] blocks;

    private final ConnGraph connGraph;

    public NavigableSurface(int x, int y, int z) {
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        this.blocks = new boolean[x][y][z];
        this.connGraph = new ConnGraph(Attachment::new);
    }

    public static class Attachment {
        public final int surfaceSize;

        public Attachment(Object a, Object b) {
            this((Attachment) a, (Attachment) b);
        }

        public Attachment(Attachment a, Attachment b) {
            this.surfaceSize = a.surfaceSize + b.surfaceSize;
        }

        public Attachment() {
            this.surfaceSize = 1;
        }
    }

    public OptionalInt surfaceSize(BetterBlockPos pos) { // how big is the navigable surface from here? how many distinct coordinates can i walk to (in the future, the augmentation will probably have a list of those coordinates or something?)
        Object data = connGraph.getComponentAugmentation(pos.toLong());
        if (data != null) { // i disagree with the intellij suggestion here i think it makes it worse
            return OptionalInt.of(((Attachment) data).surfaceSize);
        } else {
            return OptionalInt.empty();
        }
    }

    // so the idea is that as blocks are added and removed, we'll maintain where the player can stand, and what connections that has to other places
    public void placeOrRemoveBlock(BetterBlockPos where, boolean place) {
        // i think it makes sense to only have a single function, as both placing and breaking blocks can create/remove places the player could stand, as well as creating/removing connections between those places
        boolean previously = getBlock(where);
        if (previously == place) {
            return; // this is already the case
        }
        blocks[where.x][where.y][where.z] = place;
        // first let's set some vertex info for where the player can and cannot stand
        for (int dy = -1; dy <= 1; dy++) {
            BetterBlockPos couldHaveChanged = where.up(dy);
            boolean currentlyAllowed = canPlayerStandIn(couldHaveChanged);
            if (currentlyAllowed) {
                // i'm sure this will get more complicated later
                connGraph.setVertexAugmentation(couldHaveChanged.toLong(), new Attachment());
            } else {
                connGraph.removeVertexAugmentation(couldHaveChanged.toLong());
            }
        }
        // then let's set the edges
        for (int dy = -2; dy <= 1; dy++) { // -2 because of the jump condition for ascending
            // i guess some of these can be skipped based on whether "place" is false or true, but, whatever this is just for testing
            BetterBlockPos couldHaveChanged = where.up(dy);
            computePossibleMoves(couldHaveChanged);
        }
    }

    public boolean canPlayerStandIn(BetterBlockPos where) {
        return getBlockOrAir(where.down()) && !getBlockOrAir(where) && !getBlockOrAir(where.up());
    }

    public void computePossibleMoves(BetterBlockPos feet) {
        boolean anySuccess = canPlayerStandIn(feet);
        // even if all are fail, need to remove those edges from the graph, so don't return early
        for (int[] move : MOVES) {
            BetterBlockPos newFeet = new BetterBlockPos(feet.x + move[0], feet.y + move[1], feet.z + move[2]);
            boolean thisSuccess = anySuccess;
            thisSuccess &= canPlayerStandIn(newFeet);
            if (move[1] == -1) {
                // descend movement requires the player head to move through one extra block (newFeet must be 3 high not 2 high)
                thisSuccess &= !getBlockOrAir(newFeet.up(2));
            }
            if (move[1] == 1) {
                // same idea but ascending instead of descending
                thisSuccess &= !getBlockOrAir(feet.up(2));
            }
            if (thisSuccess) {
                if (connGraph.addEdge(feet.toLong(), newFeet.toLong())) {
                    //System.out.println("Player can now move between " + feet + " and " + newFeet);
                }
            } else {
                if (connGraph.removeEdge(feet.toLong(), newFeet.toLong())) {
                    //System.out.println("Player can no longer move between " + feet + " and " + newFeet);
                }
            }
        }
    }

    public int requireSurfaceSize(int x, int y, int z) {
        return surfaceSize(new BetterBlockPos(x, y, z)).getAsInt();
    }

    public boolean inRange(int x, int y, int z) {
        return (x | y | z | (sizeX - (x + 1)) | (sizeY - (y + 1)) | (sizeZ - (z + 1))) >= 0; // ">= 0" is used here in the sense of "most significant bit is not set"
    }

    public boolean getBlock(BetterBlockPos where) {
        return blocks[where.x][where.y][where.z];
    }

    public boolean getBlockOrAir(BetterBlockPos where) {
        if (!inRange(where.x, where.y, where.z)) {
            return false;
        }
        return getBlock(where);
    }

    public boolean connected(BetterBlockPos a, BetterBlockPos b) {
        return connGraph.connected(a.toLong(), b.toLong());
    }

    public void placeBlock(BetterBlockPos where) {
        placeOrRemoveBlock(where, true);
    }

    public void placeBlock(int x, int y, int z) {
        placeBlock(new BetterBlockPos(x, y, z));
    }

    public void removeBlock(BetterBlockPos where) {
        placeOrRemoveBlock(where, false);
    }

    public void removeBlock(int x, int y, int z) {
        removeBlock(new BetterBlockPos(x, y, z));
    }

    private static final int[][] MOVES = {
            {1, -1, 0},
            {-1, -1, 0},
            {0, -1, 1},
            {0, -1, -1},

            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1},

            {1, 1, 0},
            {-1, 1, 0},
            {0, 1, 1},
            {0, 1, -1},
    };
}
