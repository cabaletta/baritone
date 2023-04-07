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

import java.util.Collections;
import java.util.List;

/**
 * Information about an IBlockState
 * <p>
 * There will be exactly one of these per valid IBlockState in the game
 */
public final class BlockStateCachedData {
    public final boolean fullyWalkableTop;
    private final int collisionHeightBlips;
    public final boolean isAir;

    public final boolean collidesWithPlayer;

    public final boolean mustSneakWhenPlacingAgainstMe;

    public final List<BlockStatePlacementOption> placeMe; // list because of unknown size with no obvious indexing

    public final PlaceAgainstData[] placeAgainstMe; // array because of fixed size with obvious indexing (no more than one per face, so, index per face)

    public BlockStateCachedData(BlockStateCachedDataBuilder builder) {
        builder.sanityCheck();
        this.isAir = builder.isAir();
        this.fullyWalkableTop = builder.isFullyWalkableTop();
        this.collidesWithPlayer = builder.isCollidesWithPlayer();
        if (collidesWithPlayer) {
            this.collisionHeightBlips = builder.collisionHeightBlips();
        } else {
            this.collisionHeightBlips = -1;
        }

        this.mustSneakWhenPlacingAgainstMe = builder.isMustSneakWhenPlacingAgainstMe();
        this.placeMe = Collections.unmodifiableList(builder.howCanIBePlaced());

        this.placeAgainstMe = builder.placeAgainstMe();
    }

    public int collisionHeightBlips() {
        if (Main.DEBUG && !collidesWithPlayer) { // confirmed and tested: when DEBUG is false, proguard removes this if in the first pass, then inlines the calls in the second pass, making this just as good as a field access in release builds
            throw new IllegalStateException();
        }
        return collisionHeightBlips;
    }

    public boolean possibleAgainstMe(BlockStatePlacementOption placement) {
        PlaceAgainstData against = againstMe(placement);
        return against != null && possible(placement, against);
    }

    public PlaceAgainstData againstMe(BlockStatePlacementOption placement) {
        return placeAgainstMe[placement.against.oppositeIndex];
    }

    public static boolean possible(BlockStatePlacementOption placement, PlaceAgainstData against) {
        if (placement.against != against.against.opposite()) {
            throw new IllegalArgumentException();
        }
        if (placement.against.vertical) {
            return true;
        }
        return
                (against.presentsAnOptionStrictlyInTheBottomHalfOfTheStandardVoxelPlane() && placement.half != Half.TOP) ||
                        (against.presentsAnOptionStrictlyInTheTopHalfOfTheStandardVoxelPlane() && placement.half != Half.BOTTOM);
    }
}
