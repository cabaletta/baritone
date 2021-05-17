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
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public class ReachabilityLive implements IReachabilityProvider {

    private final DependencyGraphScaffoldingOverlay overlay;
    private final PlayerReachSphere sphere;

    public ReachabilityLive(DependencyGraphScaffoldingOverlay overlay, PlayerReachSphere sphere) {
        this.overlay = overlay;
        this.sphere = sphere;
    }

    @Override
    public LongList candidates(long playerEyeVoxel) {
        LongArrayList ret = new LongArrayList();
        for (long offset : sphere.positions) {
            long block = (playerEyeVoxel + offset) & BetterBlockPos.POST_ADDITION_MASK;
            if (overlay.bounds().inRangePos(block)) {
                ret.add(block);
            }
        }
        return ret;
    }
}
