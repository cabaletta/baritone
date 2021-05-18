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

    private static final BlockStateCachedData[] PER_STATE = Main.DATA_PROVIDER.allNullable();
    public static final BlockStateCachedData SCAFFOLDING = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).fullyWalkableTop().collisionHeight(1).canPlaceAgainstMe());

    public final boolean fullyWalkableTop;
    public final Integer collisionHeightBlips;
    public final boolean isAir;

    public final boolean collidesWithPlayer;


    public final boolean mustSneakWhenPlacingAgainstMe;

    public final List<BlockStatePlacementOption> options;

    public final PlaceAgainstData[] againstMe;

    public static BlockStateCachedData get(int state) {
        return PER_STATE[state];
    }

    public BlockStateCachedData(BlockStateCachedDataBuilder builder) {
        builder.sanityCheck();
        this.isAir = builder.isAir();
        this.fullyWalkableTop = builder.isFullyWalkableTop();
        this.collidesWithPlayer = builder.isCollidesWithPlayer();
        this.collisionHeightBlips = builder.collisionHeightBlips();

        this.mustSneakWhenPlacingAgainstMe = builder.isMustSneakWhenPlacingAgainstMe();
        this.options = Collections.unmodifiableList(builder.howCanIBePlaced());

        this.againstMe = builder.placeAgainstMe();
    }

    public boolean possibleAgainstMe(BlockStatePlacementOption placement) {
        if (Main.fakePlacementForPerformanceTesting) {
            return Main.RAND.nextInt(10) < 8;
        }
        PlaceAgainstData against = againstMe[placement.against.oppositeIndex];
        return against != null && possible(placement, against);
    }

    public PlaceAgainstData againstMe(BlockStatePlacementOption placement) {
        return againstMe[placement.against.oppositeIndex];
    }

    public static boolean possible(BlockStatePlacementOption placement, PlaceAgainstData against) {
        if (placement.against != against.against.opposite()) {
            throw new IllegalArgumentException();
        }
        return
                (against.presentsAnOptionStrictlyInTheBottomHalfOfTheStandardVoxelPlane() && placement.half != Half.TOP) ||
                        (against.presentsAnOptionStrictlyInTheTopHalfOfTheStandardVoxelPlane() && placement.half != Half.BOTTOM);
    }
}
