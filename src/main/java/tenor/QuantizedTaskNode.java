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

public abstract class QuantizedTaskNode extends TaskNode<IQuantizedChildTaskRelationship> implements IQuantizedTask {

    public QuantizedTaskNode(DependencyType type) {
        super(type);
    }

    // if the child task were able to provide this amount, how much priority would that be?
    public abstract double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity);

    public int quantityExecutingInto(QuantizedToSingularTaskRelationship child) {
        if (type() != DependencyType.SERIAL) {
            throw new UnsupportedOperationException(this + " " + child);
        }
        // need to calculate from scratch
        int ind = childTasks().indexOf(child);
        if (ind <= 0) {
            throw new IllegalStateException(childTasks() + "");
        }
        int minQuantity = -1;
        for (int i = 0; i < childTasks().indexOf(child); i++) {
            QuantizedToQuantizedTaskRelationship relationship = (QuantizedToQuantizedTaskRelationship) childTasks().get(i);
            IClaimProvider claim = (IClaimProvider) relationship.childTask();
            int amt = claim.quantityCompletedForParent(relationship);
            if (minQuantity == -1 || amt < minQuantity) {
                minQuantity = amt;
            }
        }
        return minQuantity;
    }
}
