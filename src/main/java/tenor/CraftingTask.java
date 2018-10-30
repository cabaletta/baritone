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

import net.minecraft.util.Tuple;

import java.util.List;

public class CraftingTask extends QuantizedTaskNode {

    int outputQuantity;
    List<Tuple<AquireItemTask, Integer>> recipe;
    AquireCraftingItems inputs;

    public CraftingTask() {
        super(DependencyType.SERIAL);
    }

    @Override
    public IQuantityRelationship cost() {
        return x -> {
            int actualQuantity = (int) Math.ceil(x * 1.0D / outputQuantity);
            return inputs.cost().value(actualQuantity);
        };
    }

    public IQuantityRelationship priority() {
        if (parentTasks().size() != 1) {
            throw new IllegalStateException();
        }
        // TODO this is a short circuit
        return ((IQuantizedTask) (parentTasks().get(0).parentTask())).priority();
    }

    @Override
    public double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity) {
        // how much priority would we give this child if they could provide us this quantity?
        int amountWeWouldProvide = quantity * outputQuantity;
        double desirability = priority().value(amountWeWouldProvide);
        return desirability;
    }

    public int inputSizeFor(AquireItemTask task) {
        for (Tuple<AquireItemTask, Integer> tup : recipe) {
            if (tup.getFirst().equals(task)) {
                return tup.getSecond();
            }
        }
        throw new IllegalStateException();
    }
}
