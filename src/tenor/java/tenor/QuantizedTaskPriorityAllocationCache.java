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
        Allocation allocation = allocationByQuantity.get(quantity);
        if (allocation == null || allocation.total != effectiveAllocationSize(quantity)) {
            allocation = allocationStrategy(quantity);
            allocationByQuantity.put(quantity, allocation);
        }
        return allocation.allocatedTo(child);
    }

    protected Allocation allocationStrategy(int quantity) { // overridable
        double amount = effectiveAllocationSize(quantity);
        Allocation alloc = new Allocation(amount);
        List<IQuantizedParentTaskRelationship> children = childTasks();
        if (children.size() < 2) {
            return alloc.distributeEqually(); // 0 or 1 cannot be anything but equal distribution
        }
        double[] costs = new double[children.size()];
        for (int i = 0; i < costs.length; i++) {
            costs[i] = children.get(i).cost().value(quantity);
        }
        applyAllocation(alloc, costs);
        return alloc;
    }

    protected void applyAllocation(Allocation alloc, double[] childCosts) { // overridable
        switch (type) {
            case ANY_ONE_OF:
                // by default, give it all to the lowest cost
                // this is stable
                int lowestInd = 0;
                for (int i = 1; i < childCosts.length; i++) {
                    if (childCosts[i] < childCosts[lowestInd]) {
                        lowestInd = i;
                    }
                }
                alloc.givePriority(lowestInd, alloc.unallocated());
            case SERIAL:
                // give it all to the first nonzero cost? maybe?
                for (int i = 1; i < childCosts.length; i++) {
                    if (childCosts[i] > 0) {
                        alloc.givePriority(i, alloc.unallocated());
                        return;
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    protected Allocation blend(Allocation previous, Allocation current) { // overridable
        if (previous.priorities.length != current.priorities.length) {
            return current;
        }
        Allocation blended = new Allocation(current.total);
        for (int i = 0; i < current.priorities.length; i++) {
            blended.givePriority(i, current.priorities[i] * 0.1d + previous.priorities[i] * 0.9d);
        }
        return blended;
    }

    protected double effectiveAllocationSize(int quantity) { // overridable
        return priority().value(quantity);
    }

    protected class Allocation {
        private final double[] priorities; // always sums up to 1 or less
        private final double total;

        public Allocation(double total) {
            this.priorities = new double[childTasks().size()];
            this.total = total;
        }

        public double unallocated() {
            return 1 - allocated();
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
