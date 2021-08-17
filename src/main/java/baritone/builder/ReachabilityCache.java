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
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public class ReachabilityCache implements IReachabilityProvider {

    private final Long2ObjectOpenHashMap<LongArrayList> playerPositionToBlock;

    public ReachabilityCache(DependencyGraphScaffoldingOverlay overlay, PlayerReachSphere sphere) throws SchematicIsTooDenseForThisToMakeSenseException {
        playerPositionToBlock = new Long2ObjectOpenHashMap<>();
        int maxReasonableCacheSize = overlay.bounds().size;
        int[] cnt = {0};
        overlay.forEachReal(blockPos -> { // by only iterating through real blocks, this will be a much faster and better option for sparse schematics (e.g. staircased map art)
            for (long offset : sphere.positions) {
                long playerEyeVoxel = (blockPos + offset) & BetterBlockPos.POST_ADDITION_MASK;
                if (overlay.bounds().inRangePos(playerEyeVoxel)) {
                    LongArrayList blocks = playerPositionToBlock.get(playerEyeVoxel);
                    if (blocks == null) {
                        blocks = new LongArrayList();
                        playerPositionToBlock.put(playerEyeVoxel, blocks);
                    }
                    blocks.add(blockPos);
                    if (cnt[0]++ > maxReasonableCacheSize) {
                        throw new SchematicIsTooDenseForThisToMakeSenseException(); // although, of course, it's perfectly possible for this to NOT be a good idea too
                    }
                }
            }
        });
    }

    @Override
    public LongList candidates(long playerEyeVoxel) {
        return playerPositionToBlock.get(playerEyeVoxel);
    }

    public static class SchematicIsTooDenseForThisToMakeSenseException extends RuntimeException {

    }
}
