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

public class AquireItemTask extends QuantizedTaskNode implements IClaimProvider, IQuantizedDependentCostCalculator<IQuantizedChildTaskRelationship> {

    HashMap<IQuantizedChildTaskRelationship, Integer> allocation; // allocation of what tasks have claim over what items in our inventory i guess

    public AquireItemTask() {
        super(DependencyType.ANY_ONE_OF);
    }

    @Override
    public int quantityCompletedForParent(IQuantizedChildTaskRelationship relationship) {
        return allocation.get(relationship);
    }

    public void reallocate() {
        List<IQuantizedChildTaskRelationship> parents = (List<IQuantizedChildTaskRelationship>) (Object) parentTasks();

        allocation.clear();
        int amountToAllocate = getCurrentQuantityInInventory();
        int[] newAmounts = ScarceParentPriorityAllocator.priorityAllocation(amountToAllocate, parents);
        for (int i = 0; i < parents.size(); i++) {
            allocation.put(parents.get(i), newAmounts[i]);
        }
    }

    public int getCurrentQuantityInInventory() {
        throw new UnsupportedOperationException("oppa");
    }

    @Override
    public IQuantityRelationship priority() {
        return x -> {
            double sum = 0;
            for (Map.Entry<IQuantizedChildTaskRelationship, Integer> entry : allocation.entrySet()) {

                IQuantizedChildTaskRelationship parentRelationship = entry.getKey();
                int quantityAssigned = entry.getValue();

                sum += parentRelationship.allocatedPriority(quantityAssigned);
            }
            return sum;
        };
    }

    @Override
    public IQuantityRelationship cost() {
        return IQuantizedDependentCostCalculator.super.cost(); // oppa
    }

    @Override
    public double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity) {
        // how much of our priority would go to this child if it could provide us with quantity of the item we need
        return priority().value(quantity);
    }
}
