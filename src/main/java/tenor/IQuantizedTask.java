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

public interface IQuantizedTask extends ITask {

    /*default QuantityRelationship priority() {
        return q -> {
            double sum = 0;
            for (IQuantizedChildTaskRelationship parent : parents()) {
                sum += parent.allocatedPriority(q);
            }
            return sum;
        };
    }*/

    IQuantityRelationship priority();

    IQuantityRelationship cost();

    default IQuantizedChildTaskRelationship<? extends ITaskNodeBase> createRelationshipToParent(ITaskNodeBase parent) {
        if (parent instanceof IQuantizedTask) {
            return new QuantizedToQuantizedTaskRelationship((QuantizedTaskNode) parent, this, parent.type());
        } else {
            throw new UnsupportedOperationException("SingularToQuantized must be constructed manually since it needs a quantity");
        }
    }

    default QuantizedToQuantizedTaskRelationship createRelationshipToParent(QuantizedTaskNode parent) {
        return new QuantizedToQuantizedTaskRelationship(parent, this, parent.type());
    }
}
