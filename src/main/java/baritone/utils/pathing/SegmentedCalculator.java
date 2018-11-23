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
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.SplicedPath;

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
            PathCalculationResult result = segment(soFar);
            switch (result.getType()) {
                case SUCCESS_SEGMENT:
                    break;
                case SUCCESS_TO_GOAL: // if we've gotten all the way to the goal, we're done
                case FAILURE: // if path calculation failed, we're done
                case EXCEPTION: // if path calculation threw an exception, we're done
                    return soFar;
                default: // CANCELLATION and null should not be possible, nothing else has access to this, so it can't have been canceled
                    throw new IllegalStateException();
            }
            IPath segment = result.getPath().get(); // path calculation result type is SUCCESS_SEGMENT, so the path must be present
            IPath combined = soFar.map(previous -> (IPath) SplicedPath.trySplice(previous, segment, true).get()).orElse(segment);
            soFar = Optional.of(combined);
        }
    }

    private PathCalculationResult segment(Optional<IPath> previous) {
        BetterBlockPos segmentStart = previous.map(IPath::getDest).orElse(start); // <-- e p i c
        AbstractNodeCostSearch search = PathingBehavior.createPathfinder(segmentStart, goal, previous.orElse(null), context);
        return search.calculate(Baritone.settings().primaryTimeoutMS.get(), Baritone.settings().failureTimeoutMS.get()); // use normal time settings, not the plan ahead settings, so as to not overwhelm the computer
    }

    public static void calculateSegmentsThreaded(BetterBlockPos start, Goal goal, CalculationContext context, Consumer<Optional<IPath>> onCompletion) {
        Baritone.getExecutor().execute(() -> {
            Optional<IPath> result;
            try {
                result = new SegmentedCalculator(start, goal, context).doCalc();
            } catch (Exception ex) {
                ex.printStackTrace();
                result = Optional.empty();
            }
            onCompletion.accept(result);
        });
    }
}
