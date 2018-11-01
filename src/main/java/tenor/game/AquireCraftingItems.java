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

public class AquireCraftingItems extends QuantizedTaskNode implements IClaimProvider, ISingleParentQuantizedPriorityAllocator {

    final CraftingTask parent;

    public AquireCraftingItems(CraftingTask parent) {
        super(parent.bot, DependencyType.PARALLEL_ALL);
        this.parent = parent;
        addParent(parent);
    }

    @Override
    public double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity) {
        AquireItemTask resource = (AquireItemTask) child.childTask(); // all our dependents are aquire item tasks
        int amount = parent.inputSizeFor(resource);

        // they could provide us with quantity
        int actualQuantity = (int) Math.ceil(quantity * 1.0D / amount);
        // so we could do the crafting recipe this many times
        // how good would that be?
        return priority().value(actualQuantity);
    }

    @Override
    public IQuantityRelationship cost() {
        return x -> {
            // cost to get x copies of these items
            double sum = 0;
            for (IQuantizedParentTaskRelationship resource : childTasks()) {
                int amountPerCraft = parent.inputSizeFor((AquireItemTask) resource.childTask());
                int totalAmountNeeded = x * amountPerCraft;

                int amountForUs = ((IQuantizedChildTaskRelationship) resource).quantityCompleted();
                totalAmountNeeded -= amountForUs;

                if (totalAmountNeeded <= 0) {
                    continue;
                }

                sum += resource.cost().value(totalAmountNeeded);
            }
            return sum;
        };
    }

    @Override
    public int quantityCompletedForParent(IQuantizedChildTaskRelationship relationship) {
        if (relationship != parentTasks().get(0)) {
            throw new IllegalStateException();
        }
        // our only parent is the crafting task
        int minCompletion = Integer.MAX_VALUE;
        for (IQuantizedParentTaskRelationship resource : childTasks()) {
            int amountForUs = ((IQuantizedChildTaskRelationship) resource).quantityCompleted();

            int amountPerCraft = parent.inputSizeFor((AquireItemTask) resource.childTask());

            int actualQuantity = (int) Math.ceil(amountForUs * 1.0D / amountPerCraft);

            if (actualQuantity < minCompletion || minCompletion == Integer.MAX_VALUE) {
                minCompletion = actualQuantity; // any missing ingredient and we aren't really done
            }
        }
        return minCompletion;
    }
}
