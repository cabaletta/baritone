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

import net.minecraft.util.Tuple;
import tenor.*;

import java.util.List;

public class CraftingTask extends QuantizedTaskNode implements ISingleParentQuantizedPriorityAllocator {

    int outputQuantity;
    List<Tuple<AquireItemTask, Integer>> recipe;

    final AquireCraftingItems inputs;
    final GetToCraftingTableTask step2;
    final ActuallyCraftTask step3;

    final AquireItemTask parent;

    public CraftingTask(AquireItemTask parent) {
        super(parent.bot, DependencyType.SERIAL);
        this.inputs = new AquireCraftingItems(this); // this adds the relationship
        this.step2 = registry().getSingleton(GetToCraftingTableTask.class);
        step2.addParent(this);
        this.step3 = new ActuallyCraftTask(this);

        this.parent = parent;
        addParent(parent);

    }

    @Override
    public IQuantityRelationship cost() {
        return x -> {
            int actualQuantity = (int) Math.ceil(x * 1.0D / outputQuantity);
            return inputs.cost().value(actualQuantity) + step2.cost() + step3.cost();
        };
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
