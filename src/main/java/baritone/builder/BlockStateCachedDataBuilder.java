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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BlockStateCachedDataBuilder {

    // should all these be optionals? like maybe Boolean? that start as null? and each has to be set explicitly?
    private boolean isAir;
    private boolean canPlaceAgainstMe;
    private boolean fullyWalkableTop;
    private boolean collidesWithPlayer;
    private boolean mustSneakWhenPlacingAgainstMe;
    private boolean falling;
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
    private Face playerMustBeHorizontalFacingInOrderToPlaceMe;
    private Integer collisionHeightBlips;
    private Face canOnlyPlaceAgainst;
    private boolean fakeLessThanFullHeight; // snow layers and soul sand
    private boolean placementLogicNotImplementedYet;
    private Face playerMustBeEntityFacingInOrderToPlaceMe;

    public BlockStateCachedDataBuilder() {
    }

    /**
     * Really just air. This is a fully open block that won't collide with object mouse over raytrace.
     */
    public BlockStateCachedDataBuilder setAir() {
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

    public BlockStateCachedDataBuilder collisionHeight(double y) {
        for (int h = 1; h <= Blip.PER_BLOCK + Blip.HALF_BLOCK; h++) {
            if (y == h * Blip.RATIO) {
                collisionHeightBlips = h;
                return this;
            }
        }
        throw new IllegalStateException();
    }

    public Integer collisionHeightBlips() { // e.g. slabs are 0.5, soul sand is 0.875, normal blocks are 1, fences are 1.5
        return collisionHeightBlips;
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

    public boolean isCollidesWithPlayer() {
        return collidesWithPlayer;
    }

    public BlockStateCachedDataBuilder collidesWithPlayer(boolean val) {
        collidesWithPlayer = val;
        return this;
    }

    public BlockStateCachedDataBuilder playerMustBeHorizontalFacingInOrderToPlaceMe(Face face) {
        playerMustBeHorizontalFacingInOrderToPlaceMe = face;
        return this;
    }

    public BlockStateCachedDataBuilder playerMustBeEntityFacingInOrderToPlaceMe(Face face) {
        playerMustBeEntityFacingInOrderToPlaceMe = face;
        return this;
    }

    public BlockStateCachedDataBuilder mustBePlacedAgainst(Half half) {
        if (half == null) {
            throw new IllegalArgumentException();
        }
        mustBePlacedAgainst = half;
        return this;
    }

    public BlockStateCachedDataBuilder falling() {
        falling = true;
        return this;
    }

    public BlockStateCachedDataBuilder canOnlyPlaceAgainst(Face face) {
        canOnlyPlaceAgainst = face;
        return this;
    }

    public BlockStateCachedDataBuilder placementLogicNotImplementedYet() {
        placementLogicNotImplementedYet = true;
        return this;
    }

    public BlockStateCachedDataBuilder fakeLessThanFullHeight() {
        fakeLessThanFullHeight = true;
        return this;
    }

    public List<BlockStatePlacementOption> howCanIBePlaced() {
        if (mustBePlacedAgainst == null || placementLogicNotImplementedYet) {
            return Collections.emptyList();
        }
        List<BlockStatePlacementOption> ret = new ArrayList<>();
        for (Face face : Face.VALUES) {
            if (Main.STRICT_Y && face == Face.UP) {
                continue; // TODO don't do this...
            }
            if (playerMustBeHorizontalFacingInOrderToPlaceMe == face.opposite()) { // obv, this won't happen if playerMustBeHorizontalFacing is null
                continue;
            }
            if (playerMustBeEntityFacingInOrderToPlaceMe == face) {
                continue;
            }
            if (falling && face != Face.DOWN) {
                continue;
            }
            if (canOnlyPlaceAgainst != null && face != canOnlyPlaceAgainst) {
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
            ret.add(BlockStatePlacementOption.get(face, overrideHalf, Optional.ofNullable(playerMustBeHorizontalFacingInOrderToPlaceMe), Optional.ofNullable(playerMustBeEntityFacingInOrderToPlaceMe)));
        }
        return ret;
    }

    public PlaceAgainstData[] placeAgainstMe() {
        PlaceAgainstData[] data = new PlaceAgainstData[Face.NUM_FACES];
        if (!canPlaceAgainstMe) {
            return data;
        }
        for (int i = 0; i < Face.NUM_FACES; i++) {
            data[i] = placeAgainstFace(Face.VALUES[i]);
        }
        return data;
    }

    protected PlaceAgainstData placeAgainstFace(Face face) {
        // TODO this makes the stair/slab assumption that the same half is the mustBePlacedAgainst as the faces offered for placement... counterexample is daylight sensor
        if (mustBePlacedAgainst == Half.TOP && face == Face.DOWN) {
            return null;
        }
        if (mustBePlacedAgainst == Half.BOTTOM && face == Face.UP) {
            return null;
        }
        return new PlaceAgainstData(face, face.vertical ? Half.EITHER : mustBePlacedAgainst, mustSneakWhenPlacingAgainstMe);
    }

    public void sanityCheck() {
        if (isAir()) {
            if (!howCanIBePlaced().isEmpty()) {
                throw new IllegalStateException();
            }
            if (isFullyWalkableTop()) {
                throw new IllegalStateException();
            }
            if (collidesWithPlayer) {
                throw new IllegalStateException();
            }
        }
        if (mustBePlacedAgainst == null ^ isAir()) {
            throw new IllegalStateException();
        }
        if (howCanIBePlaced().isEmpty()) {
            if (mustBePlacedAgainst != null && !placementLogicNotImplementedYet) {
                throw new IllegalStateException();
            }
            if (playerMustBeHorizontalFacingInOrderToPlaceMe != null || playerMustBeEntityFacingInOrderToPlaceMe != null) {
                throw new IllegalStateException();
            }
            if (canOnlyPlaceAgainst != null) {
                throw new IllegalStateException();
            }
        }
        if (isMustSneakWhenPlacingAgainstMe() && mustBePlacedAgainst != Half.EITHER) {
            throw new IllegalArgumentException();
        }
        if ((playerMustBeHorizontalFacingInOrderToPlaceMe != null || playerMustBeEntityFacingInOrderToPlaceMe != null) && mustBePlacedAgainst == null) {
            throw new IllegalStateException();
        }
        if (isFullyWalkableTop() ^ collisionHeightBlips != null) {
            if (!isFullyWalkableTop() && collisionHeightBlips > Blip.PER_BLOCK) {
                // exception for fences, walls
            } else {
                throw new IllegalStateException();
            }
        }
        if (collisionHeightBlips != null && (collisionHeightBlips > Blip.FULL_BLOCK + Blip.HALF_BLOCK || collisionHeightBlips <= 0)) { // playerphysics assumes this is never true
            throw new IllegalStateException();
        }
        if (fullyWalkableTop && !collidesWithPlayer) {
            throw new IllegalStateException();
        }
        if (canPlaceAgainstMe && !collidesWithPlayer) {
            throw new IllegalStateException();
        }
        if (playerMustBeHorizontalFacingInOrderToPlaceMe != null && playerMustBeHorizontalFacingInOrderToPlaceMe.vertical) {
            throw new IllegalStateException();
        }
        if (Main.STRICT_Y && howCanIBePlaced().stream().anyMatch(opt -> opt.against == Face.UP)) {
            throw new IllegalStateException();
        }
        PlaceAgainstData[] data = placeAgainstMe();
        if (data.length != Face.NUM_FACES) {
            throw new IllegalStateException();
        }
        boolean any = false;
        for (int i = 0; i < Face.NUM_FACES; i++) {
            if (data[i] != null) {
                if (data[i].against != Face.VALUES[i]) {
                    throw new IllegalStateException();
                }
                if (!canPlaceAgainstMe) {
                    throw new IllegalStateException();
                }
                any = true;
            }
        }
        if (canPlaceAgainstMe && !any) {
            throw new IllegalStateException();
        }
        if (collisionHeightBlips != null && !fakeLessThanFullHeight) {
            for (PlaceAgainstData d : data) {
                if (d == null) {
                    continue;
                }
                d.streamRelativeToMyself().forEach(hit -> {
                    if (hit.y > collisionHeightBlips * Blip.RATIO) {
                        throw new IllegalStateException(d.against + " " + hit.y + " " + collisionHeightBlips * Blip.RATIO);
                    }
                });
            }
        }
    }

    static {
        new BlockStateCachedDataBuilder().sanityCheck();
    }
}
