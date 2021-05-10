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
import java.util.*;

/**
 * Information about an IBlockState
 * <p>
 * There will be exactly one of these per valid IBlockState in the game
 */
public final class BlockStateCachedData {

    private static final BlockStateCachedData[] PER_STATE = Main.DATA_PROVIDER.all();
    public static final BlockStateCachedData SCAFFOLDING = new BlockStateCachedData(false, true, true, Half.EITHER, false);

    public final boolean canWalkOn;
    public final boolean isAir;
    public final boolean mustSneakWhenPlacingAgainstMe;
    private final boolean[] presentsTopHalfFaceForPlacement;
    private final boolean[] presentsBottomHalfFaceForPlacement;
    private final List<BlockStatePlacementOption> options;

    public static BlockStateCachedData get(int state) {
        return PER_STATE[state];
    }

    public BlockStateCachedData(boolean isAir, boolean canPlaceAgainstAtAll, boolean canWalkOn, Half half, boolean mustSneakWhenPlacingAgainstMe) {
        this.isAir = isAir;
        this.canWalkOn = canWalkOn;
        this.mustSneakWhenPlacingAgainstMe = mustSneakWhenPlacingAgainstMe;
        this.options = Collections.unmodifiableList(calcOptions(canPlaceAgainstAtAll, half));
        this.presentsTopHalfFaceForPlacement = new boolean[Face.VALUES.length];
        this.presentsBottomHalfFaceForPlacement = new boolean[Face.VALUES.length];
        setupFacesPresented(canPlaceAgainstAtAll, half);
        if (mustSneakWhenPlacingAgainstMe && half != Half.EITHER) {
            throw new IllegalArgumentException();
        }
    }

    private void setupFacesPresented(boolean canPlaceAgainstAtAll, Half half) {
        if (!canPlaceAgainstAtAll) {
            return;
        }
        Arrays.fill(presentsBottomHalfFaceForPlacement, true);
        Arrays.fill(presentsTopHalfFaceForPlacement, true);
        // TODO support case for placing against nub of stair on the one faced side
        switch (half) {
            case EITHER: {
                return;
            }
            case TOP: {
                // i am a top slab, or an upside down stair
                presentsBottomHalfFaceForPlacement[Face.DOWN.index] = false;
                presentsTopHalfFaceForPlacement[Face.DOWN.index] = false;
                for (Face face : Face.HORIZONTALS) {
                    presentsBottomHalfFaceForPlacement[face.index] = false; // top slab = can't place against the bottom half
                }
                break;
            }
            case BOTTOM: {
                // i am a bottom slab, or an normal stair
                presentsBottomHalfFaceForPlacement[Face.UP.index] = false;
                presentsTopHalfFaceForPlacement[Face.UP.index] = false;
                for (Face face : Face.HORIZONTALS) {
                    presentsTopHalfFaceForPlacement[face.index] = false; // bottom slab = can't place against the top half
                }
                break;
            }
        }
    }

    @Nullable
    private PlaceAgainstData presentsFace(Face face, Half half) {
        if ((face == Face.UP || face == Face.DOWN) && half != Half.EITHER) {
            throw new IllegalStateException();
        }
        boolean top = presentsTopHalfFaceForPlacement[face.index] && (half == Half.EITHER || half == Half.TOP);
        boolean bottom = presentsBottomHalfFaceForPlacement[face.index] && (half == Half.EITHER || half == Half.BOTTOM);
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

    private List<BlockStatePlacementOption> calcOptions(boolean canPlaceAgainstAtAll, Half half) {
        if (canPlaceAgainstAtAll) {
            List<BlockStatePlacementOption> ret = new ArrayList<>();
            for (Face face : Face.VALUES) {
                if (Main.STRICT_Y && face == Face.UP) {
                    continue;
                }
                Half overrideHalf = half;
                if (face == Face.DOWN) {
                    if (half == Half.TOP) {
                        continue;
                    } else {
                        overrideHalf = Half.EITHER;
                    }
                }
                if (face == Face.UP) {
                    if (half == Half.BOTTOM) {
                        continue;
                    } else {
                        overrideHalf = Half.EITHER;
                    }
                }
                ret.add(BlockStatePlacementOption.get(face, overrideHalf, Optional.empty()));
            }
            return ret;
        }
        return Collections.emptyList();
    }


    @Nullable
    public PlaceAgainstData canBeDoneAgainstMe(BlockStatePlacementOption placement) {
        if (Main.fakePlacementForPerformanceTesting) {
            return Main.RAND.nextInt(10) < 8 ? PlaceAgainstData.EITHER : null;
        }

        Face myFace = placement.against.opposite();
        return presentsFace(myFace, placement.half);
    }

    public List<BlockStatePlacementOption> placementOptions() {
        return options;
    }
}
