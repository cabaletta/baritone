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

package tenor.game;

import tenor.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AquireItemTask extends QuantizedTaskNode implements IClaimProvider, IQuantizedDependentCostCalculator {

    HashMap<IQuantizedChildTaskRelationship, Integer> allocation; // allocation of what tasks have claim over what items in our inventory i guess
    String item;

    public AquireItemTask(Bot bot, String item) {
        super(bot, DependencyType.ANY_ONE_OF);
        this.item = item;
        registry().registerItemBased(this, item);
    }

    @Override
    public int quantityCompletedForParent(IQuantizedChildTaskRelationship relationship) {
        return allocation.get(relationship);
    }

    public void reallocate() {
        int amountToAllocate = getCurrentQuantityInInventory();
        if (amountToAllocate == cachedCurrentQuantity()) {
            // no change
            return;
        }
        List<IQuantizedChildTaskRelationship> parents = parentTasks();

        allocation = null;
        HashMap<IQuantizedChildTaskRelationship, Integer> tmp = new HashMap<>();

        int[] newAmounts = ScarceParentPriorityAllocator.priorityAllocation(amountToAllocate, parents).parentAllocationQuantity;
        for (int i = 0; i < parents.size(); i++) {
            tmp.put(parents.get(i), newAmounts[i]);
        }
        allocation = tmp;
    }

    public int getCurrentQuantityInInventory() {
        return bot.getCurrentQuantityInInventory(item);
    }

    public int cachedCurrentQuantity() {
        return allocation.entrySet().stream().mapToInt(Map.Entry::getValue).sum();
    }

    @Override
    public IQuantityRelationship priority() { // TODO cache
        return x -> ScarceParentPriorityAllocator.priorityAllocation(x, parentTasks()).totalPriority;
    }

    @Override
    public double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity) {
        // how much of our priority would go to this child if it could provide us with quantity of the item we need

        // here's the thing honey, we *already have* some, so you're really asking what's the priority of getting quantity MORE
        int curr = cachedCurrentQuantity();

        return priority().value(quantity + curr) - priority().value(curr);
    }
}
