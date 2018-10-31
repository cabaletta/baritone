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

package tenor;

import java.util.List;

public class ScarceParentPriorityAllocator {

    public static PriorityAllocation priorityAllocation(int quantity, List<IQuantizedChildTaskRelationship> parents) {
        if (quantity == 0) {
            return new PriorityAllocation(new int[parents.size()], 0D);
        }
        double[][] priorities = new double[parents.size()][quantity];
        for (int i = 0; i < parents.size(); i++) {
            for (int j = 1; j < quantity; j++) {
                priorities[i][j] = parents.get(i).allocatedPriority(j);
            }
        }

        int filled = 0;
        double totalPriority = 0;
        int[] taken = new int[parents.size()];
        while (true) {

            double bestRatio = 0;
            int bestParent = -1;
            int bestQuantity = -1;
            for (int i = 0; i < priorities.length; i++) {
                for (int j = 1; j < quantity - filled; j++) {
                    double ratio = priorities[i][j] / j;
                    if (ratio > bestRatio) {
                        bestRatio = ratio;
                        bestParent = i;
                        bestQuantity = j;
                    }
                }
            }

            if (bestParent == -1 || bestRatio == 0) {
                return new PriorityAllocation(taken, totalPriority);
            }
            taken[bestParent] += bestQuantity;
            filled += bestQuantity;
            double priorityTaken = priorities[bestParent][bestQuantity];
            totalPriority += priorityTaken;

            double[] repl = new double[priorities[bestParent].length - bestQuantity];
            for (int i = 0; i < repl.length; i++) {
                repl[i] = priorities[bestParent][i + bestQuantity] - priorityTaken;
            }
            priorities[bestParent] = repl;
        }
    }

    public static class PriorityAllocation {

        public final int[] parentAllocationQuantity;
        public final double totalPriority;

        public PriorityAllocation(int[] parentAllocationQuantity, double totalPriority) {
            this.parentAllocationQuantity = parentAllocationQuantity;
            this.totalPriority = totalPriority;
        }
    }
}
