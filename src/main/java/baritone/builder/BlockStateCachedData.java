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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Information about an IBlockState
 * <p>
 * There will be exactly one of these per valid IBlockState in the game
 */
public final class BlockStateCachedData {

    private static final BlockStateCachedData[] PER_STATE = Main.DATA_PROVIDER.allNullable();
    public static final BlockStateCachedData SCAFFOLDING = new BlockStateCachedData(new BlockStateCachedDataBuilder().collidesWithPlayer(true).fullyWalkableTop().height(1).canPlaceAgainstMe());

    public final boolean fullyWalkableTop;
    public final Double supportedPlayerY;
    public final boolean isAir;

    public final boolean collidesWithPlayer;


    public final boolean mustSneakWhenPlacingAgainstMe;
    private final boolean[] presentsTopHalfFaceForPlacement;
    private final boolean[] presentsBottomHalfFaceForPlacement;

    public final List<BlockStatePlacementOption> options;

    public static BlockStateCachedData get(int state) {
        return PER_STATE[state];
    }

    public BlockStateCachedData(BlockStateCachedDataBuilder builder) {
        builder.sanityCheck();
        this.isAir = builder.isAir();
        this.fullyWalkableTop = builder.isFullyWalkableTop();
        this.collidesWithPlayer = builder.isCollidesWithPlayer();
        this.supportedPlayerY = builder.supportedPlayerY();

        this.mustSneakWhenPlacingAgainstMe = builder.isMustSneakWhenPlacingAgainstMe();
        this.options = builder.howCanIBePlaced();

        boolean[][] presented = builder.facesIPresentForPlacementAgainst();
        this.presentsTopHalfFaceForPlacement = presented[1];
        this.presentsBottomHalfFaceForPlacement = presented[0];
    }

    @Nullable
    public PlaceAgainstData tryAgainstMe(BlockStatePlacementOption placement) {
        if (Main.fakePlacementForPerformanceTesting) {
            return Main.RAND.nextInt(10) < 8 ? PlaceAgainstData.EITHER : null;
        }

        Face myFace = placement.against.opposite();
        Half theirHalf = placement.half;
        if ((myFace == Face.UP || myFace == Face.DOWN) && theirHalf != Half.EITHER) {
            throw new IllegalStateException();
        }
        boolean top = presentsTopHalfFaceForPlacement[myFace.index] && (theirHalf == Half.EITHER || theirHalf == Half.TOP);
        boolean bottom = presentsBottomHalfFaceForPlacement[myFace.index] && (theirHalf == Half.EITHER || theirHalf == Half.BOTTOM);
        Half intersectedHalf; // the half that both we present, and they accept. not necessarily equal to either. slab-against-block and block-against-slab will both have this as top/bottom, not either.
        if (top && bottom) {
            intersectedHalf = Half.EITHER;
        } else if (top) {
            intersectedHalf = Half.TOP;
        } else if (bottom) {
            intersectedHalf = Half.BOTTOM;
        } else {
            return null;
        }
        return PlaceAgainstData.get(intersectedHalf, mustSneakWhenPlacingAgainstMe);
    }
}
