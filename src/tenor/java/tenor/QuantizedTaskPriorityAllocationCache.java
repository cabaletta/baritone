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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class QuantizedTaskPriorityAllocationCache extends QuantizedTaskNode {

    private final Map<Integer, Allocation> allocationByQuantity;

    public QuantizedTaskPriorityAllocationCache(Bot bot, DependencyType type) {
        super(bot, type);
        this.allocationByQuantity = new HashMap<>();
    }

    @Override
    public final double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity) {
        return allocationByQuantity.computeIfAbsent(quantity, this::allocationStrategy).allocatedTo(child);
    }

    protected Allocation allocationStrategy(int quantity) { // overridable
        double amount = effectiveAllocationSize(quantity);
        Allocation alloc = new Allocation(amount);
        List<IQuantizedParentTaskRelationship> children = childTasks();
        if (children.size() < 2) {
            return new Allocation(amount).distributeEqually(); // 0 or 1 cannot be anything but equal distribution
        }
        double[] costs = new double[children.size()];
        for (int i = 0; i < costs.length; i++) {
            costs[i] = children.get(i).cost().value(quantity);
        }
        switch (type) {
            case ANY_ONE_OF:
                // by default, give it all to the lowest cost
                // this is stable
                int lowestInd = 0;
                for (int i = 1; i < costs.length; i++) {
                    if (costs[i] < costs[lowestInd]) {
                        lowestInd = i;
                    }
                }
                alloc.givePriority(lowestInd, alloc.unallocated());
                return alloc;
            default:
                throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    protected double effectiveAllocationSize(int quantity) { // overridable
        return priority().value(quantity);
    }

    protected class Allocation {
        private final double[] priorities;
        private final double total;

        public Allocation(double total) {
            this.priorities = new double[childTasks().size()];
            this.total = total;
        }

        public double unallocated() {
            return total - allocated();
        }

        public double allocated() {
            double sum = 0;
            for (double d : priorities) {
                sum += d;
            }
            return sum;
        }

        public Allocation distributeEqually() {
            int count = priorities.length;
            double toDistribute = unallocated();
            double priorityToEach = toDistribute / count;
            for (int i = 0; i < count; i++) {
                priorities[i] += priorityToEach;
            }
            return this;
        }

        public void givePriority(int childTaskIndex, double amount) {
            if (amount > unallocated()) {
                throw new IllegalStateException("not enough space");
            }
            priorities[childTaskIndex] += amount;
        }

        public double allocatedTo(IQuantizedParentTaskRelationship childRelationship) {
            return priorities[childTasks().indexOf(childRelationship)];
        }
    }
}
