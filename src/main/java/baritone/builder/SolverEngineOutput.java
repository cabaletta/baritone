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

import java.util.List;

public class SolverEngineOutput {

    private final List<SolvedActionStep> steps;

    public SolverEngineOutput(List<SolvedActionStep> steps) {
        this.steps = steps;
    }

    public void sanityCheck(SolverEngineInput in) {
        for (SolvedActionStep step : steps) {
            step.placeAt().ifPresent(pos -> {
                if (!in.graph.bounds().inRangePos(pos)) {
                    throw new IllegalStateException();
                }
            });
            if (!in.graph.bounds().inRangePos(step.playerMovesTo())) {
                throw new IllegalStateException();
            }
        }
        long prev = in.player;
        for (SolvedActionStep step : steps) {
            long curr = step.playerMovesTo();
            sanityCheckMovement(prev, curr);
            prev = curr;
        }
    }

    private static void sanityCheckMovement(long from, long to) {
        int dx = BetterBlockPos.XfromLong(from) - BetterBlockPos.XfromLong(to);
        int dz = BetterBlockPos.ZfromLong(from) - BetterBlockPos.ZfromLong(to);
        if (Math.abs(dx) + Math.abs(dz) > 1) {
            throw new IllegalStateException();
        }
    }
}
