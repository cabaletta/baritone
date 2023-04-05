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

public class PlaceOptions {

    double blockReachDistance = 4;
    DependencyGraphScaffoldingOverlay overlay;
    IReachabilityProvider provider = IReachabilityProvider.get(overlay, new PlayerReachSphere(blockReachDistance));

    public void whatCouldIDo(int playerX, int playerFeetBlips, int playerZ) {
        int playerEyeBlips = playerFeetBlips + Blip.FEET_TO_EYE_APPROX;
        // TODO ugh how tf to deal with sneaking UGH. maybe like if (playerEyeBlips % 16 < 2) { also run all candidates from one voxel lower down because if we snuck our eye would be in there}
        int voxelY = playerEyeBlips / Blip.FULL_BLOCK;
        long pos = BetterBlockPos.toLong(playerX, voxelY, playerZ);
        for (long blockPos : provider.candidates(pos)) {
            BlockStateCachedData placingAgainst = overlay.data(blockPos);
            outer:
            for (Face againstToPlace : Face.VALUES) {
                Face placeToAgainst = againstToPlace.opposite();
                if (overlay.outgoingEdge(blockPos, againstToPlace)) {
                    long placingBlockAt = againstToPlace.offset(blockPos);
                    BlockStateCachedData blockBeingPlaced = overlay.data(placingBlockAt);
                    for (BlockStatePlacementOption option : blockBeingPlaced.placeMe) {
                        if (option.against == placeToAgainst) {
                            PlaceAgainstData againstData = placingAgainst.againstMe(option);
                            int relativeX = playerX - BetterBlockPos.XfromLong(placingBlockAt);
                            int relativeY = playerFeetBlips - Blip.FULL_BLOCK * BetterBlockPos.YfromLong(placingBlockAt);
                            int relativeZ = playerZ - BetterBlockPos.ZfromLong(placingBlockAt);
                            for (Raytracer.Raytrace trace : option.computeTraceOptions(againstData, relativeX, relativeY, relativeZ, PlayerVantage.LOOSE_CENTER, blockReachDistance)) {
                                // yay, gold star
                            }
                            continue outer;
                        }
                    }
                    throw new IllegalStateException();
                }
            }
        }
    }
}
