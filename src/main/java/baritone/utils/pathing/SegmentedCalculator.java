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

package baritone.utils.pathing;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.cache.CachedWorld;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.SplicedPath;
import net.minecraft.util.EnumFacing;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Calculate and splice many path segments to reach a goal
 *
 * @author leijurv
 */
public class SegmentedCalculator {
    private final BetterBlockPos start;
    private final Goal goal;
    private final CalculationContext context;

    private SegmentedCalculator(BetterBlockPos start, Goal goal, CalculationContext context) {
        this.start = start;
        this.goal = goal;
        this.context = context;
    }

    private Optional<IPath> doCalc() {
        Optional<IPath> soFar = Optional.empty();
        while (true) {
            PathCalculationResult result = segment(soFar.orElse(null));
            switch (result.getType()) {
                case SUCCESS_SEGMENT:
                case SUCCESS_TO_GOAL:
                    break;
                case FAILURE: // if path calculation failed, we're done
                case EXCEPTION: // if path calculation threw an exception, we're done
                    return soFar;
                default: // CANCELLATION and null should not be possible, nothing else has access to this, so it can't have been canceled
                    throw new IllegalStateException();
            }
            IPath segment = result.getPath().orElseThrow(IllegalStateException::new); // path calculation result type is SUCCESS_SEGMENT, so the path must be present
            IPath combined = soFar.map(previous -> (IPath) SplicedPath.trySplice(previous, segment, true).orElseThrow(IllegalStateException::new)).orElse(segment);
            loadAdjacent(combined.getDest().getX(), combined.getDest().getZ());
            soFar = Optional.of(combined);
            if (result.getType() == PathCalculationResult.Type.SUCCESS_TO_GOAL) {
                return soFar;
            }
        }
    }

    private void loadAdjacent(int blockX, int blockZ) {
        BetterBlockPos bp = new BetterBlockPos(blockX, 64, blockZ);
        CachedWorld cached = (CachedWorld) context.getBaritone().getPlayerContext().worldData().getCachedWorld();
        for (int i = 0; i < 4; i++) {
            // pathing thread is not allowed to load new cached regions from disk
            // it checks if every chunk is loaded before getting blocks from it
            // so you see path segments ending at multiples of 512 (plus or minus one) on either x or z axis
            // this loads every adjacent chunk to the segment end, so it can continue into the next cached region
            BetterBlockPos toLoad = bp.offset(EnumFacing.byHorizontalIndex(i), 16);
            cached.tryLoadFromDisk(toLoad.x >> 9, toLoad.z >> 9);
        }
    }

    private PathCalculationResult segment(IPath previous) {
        BetterBlockPos segmentStart = previous != null ? previous.getDest() : start;
        AbstractNodeCostSearch search = new AStarPathFinder(segmentStart.x, segmentStart.y, segmentStart.z, goal, new Favoring(previous, context), context); // this is on another thread, so cannot include mob avoidances.
        return search.calculate(Baritone.settings().primaryTimeoutMS.value, Baritone.settings().failureTimeoutMS.value); // use normal time settings, not the plan ahead settings, so as to not overwhelm the computer
    }

    public static void calculateSegmentsThreaded(BetterBlockPos start, Goal goal, CalculationContext context, Consumer<IPath> onCompletion, Runnable onFailure) {
        Baritone.getExecutor().execute(() -> {
            Optional<IPath> result;
            try {
                result = new SegmentedCalculator(start, goal, context).doCalc();
            } catch (Exception ex) {
                ex.printStackTrace();
                result = Optional.empty();
            }
            if (result.isPresent()) {
                onCompletion.accept(result.get());
            } else {
                onFailure.run();
            }
        });
    }
}
