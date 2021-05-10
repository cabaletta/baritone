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

import java.util.*;

public class BlockStateCachedDataBuilder {

    // should all these be optionals? like maybe Boolean? that start as null? and each has to be set explicitly?
    private boolean isAir;
    private boolean canPlaceAgainstMe;
    private boolean fullyWalkableTop;
    private boolean mustSneakWhenPlacingAgainstMe;
    /**
     * Examples:
     * <p>
     * Upside down stairs must be placed against TOP
     * <p>
     * Bottom slabs must be placed against BOTTOM
     * <p>
     * Normal blocks must be placed against EITHER
     */
    private Half mustBePlacedAgainst = Half.EITHER;
    private boolean stair;
    private Face playerMustBeFacingInOrderToPlaceMe;
    private double height = Double.NaN;

    public BlockStateCachedDataBuilder() {
    }

    public BlockStateCachedDataBuilder normalFullBlock() {
        return fullyWalkableTop().height(1).canPlaceAgainstMe();
    }

    public BlockStateCachedDataBuilder stair(boolean rightsideUp, Face facing) {
        stair = true;
        playerMustBeFacingInOrderToPlaceMe = facing;
        return slab(rightsideUp);
    }

    public BlockStateCachedDataBuilder slab(boolean bottom) {
        mustBePlacedAgainst = bottom ? Half.BOTTOM : Half.TOP;
        return bottom ? this : fullyWalkableTop().height(1).canPlaceAgainstMe();
    }

    /**
     * Really just air. This is a fully open block that won't collide with object mouse over raytrace.
     */
    public BlockStateCachedDataBuilder air() {
        isAir = true;
        mustBePlacedAgainst = null;
        return this;
    }

    public boolean isAir() {
        return isAir;
    }

    /**
     * does the top face of this block fully support the player from 0.0,0.0 to 1.0,1.0? true for most normal blocks. false for, for example, fences
     */
    public BlockStateCachedDataBuilder fullyWalkableTop() {
        fullyWalkableTop = true;
        return this;
    }

    public boolean isFullyWalkableTop() {
        return fullyWalkableTop;
    }

    public BlockStateCachedDataBuilder height(double y) {
        if (!isFullyWalkableTop()) {
            throw new IllegalStateException();
        }
        height = y;
        return this;
    }

    public double supportedPlayerY() { // e.g. slabs are 0.5, soul sand is 0.875, normal blocks are 1, fences are 1.5
        if (!isFullyWalkableTop() || Double.isNaN(height)) {
            throw new IllegalStateException(); // e.g. rightside-up stairs aren't fully supporting of the player so this doesn't count for them
        }
        return height;
    }

    public BlockStateCachedDataBuilder mustSneakWhenPlacingAgainstMe() {
        mustSneakWhenPlacingAgainstMe = true;
        return this;
    }

    public boolean isMustSneakWhenPlacingAgainstMe() {
        return mustSneakWhenPlacingAgainstMe;
    }

    public BlockStateCachedDataBuilder canPlaceAgainstMe() {
        canPlaceAgainstMe = true;
        return this;
    }

    public List<BlockStatePlacementOption> howCanIBePlaced() {
        if (mustBePlacedAgainst == null) {
            return Collections.emptyList();
        }
        List<BlockStatePlacementOption> ret = new ArrayList<>();
        for (Face face : Face.VALUES) {
            if (Main.STRICT_Y && face == Face.UP) {
                continue;
            }
            if (playerMustBeFacingInOrderToPlaceMe == face.opposite()) { // obv, this won't happen if playerMustBeFacing is null
                continue;
            }
            Half overrideHalf = mustBePlacedAgainst;
            if (face == Face.DOWN) {
                if (mustBePlacedAgainst == Half.TOP) {
                    continue;
                } else {
                    overrideHalf = Half.EITHER;
                }
            }
            if (face == Face.UP) {
                if (mustBePlacedAgainst == Half.BOTTOM) {
                    continue;
                } else {
                    overrideHalf = Half.EITHER;
                }
            }
            ret.add(BlockStatePlacementOption.get(face, overrideHalf, Optional.ofNullable(playerMustBeFacingInOrderToPlaceMe)));
        }
        return Collections.unmodifiableList(ret);
    }

    public boolean[][] facesIPresentForPlacementAgainst() {
        boolean[] presentsBottomHalfFaceForPlacement = new boolean[Face.VALUES.length];
        boolean[] presentsTopHalfFaceForPlacement = new boolean[Face.VALUES.length];
        if (canPlaceAgainstMe) {
            Arrays.fill(presentsBottomHalfFaceForPlacement, true);
            Arrays.fill(presentsTopHalfFaceForPlacement, true);
            switch (mustBePlacedAgainst) {
                case EITHER: {
                    break;
                }
                case TOP: {
                    // i am a top slab, or an upside down stair
                    presentsBottomHalfFaceForPlacement[Face.DOWN.index] = false;
                    presentsTopHalfFaceForPlacement[Face.DOWN.index] = false;
                    for (Face face : Face.HORIZONTALS) {
                        if (stair && face == playerMustBeFacingInOrderToPlaceMe) {
                            continue; // little nub of the stair on the faced side
                        }
                        presentsBottomHalfFaceForPlacement[face.index] = false; // top slab = can't place against the bottom half
                    }
                    break;
                }
                case BOTTOM: {
                    // i am a bottom slab, or an normal stair
                    presentsBottomHalfFaceForPlacement[Face.UP.index] = false;
                    presentsTopHalfFaceForPlacement[Face.UP.index] = false;
                    for (Face face : Face.HORIZONTALS) {
                        if (stair && face == playerMustBeFacingInOrderToPlaceMe) {
                            continue; // little nub of the stair on the faced side
                        }
                        presentsTopHalfFaceForPlacement[face.index] = false; // bottom slab = can't place against the top half
                    }
                    break;
                }
            }
        }
        return new boolean[][]{presentsBottomHalfFaceForPlacement, presentsTopHalfFaceForPlacement};
    }

    public void sanityCheck() {
        if (isAir()) {
            if (!howCanIBePlaced().isEmpty()) {
                throw new IllegalStateException();
            }
            if (isFullyWalkableTop()) {
                throw new IllegalStateException();
            }
        }
        if (mustBePlacedAgainst == null ^ isAir()) {
            throw new IllegalStateException();
        }
        if (isMustSneakWhenPlacingAgainstMe() && mustBePlacedAgainst != Half.EITHER) {
            throw new IllegalArgumentException();
        }
        if (stair ^ (playerMustBeFacingInOrderToPlaceMe != null && mustBePlacedAgainst != Half.EITHER)) {
            throw new IllegalStateException();
        }
        if (isFullyWalkableTop() == Double.isNaN(height)) {
            throw new IllegalStateException();
        }
    }

    static {
        new BlockStateCachedDataBuilder().sanityCheck();
    }
}
