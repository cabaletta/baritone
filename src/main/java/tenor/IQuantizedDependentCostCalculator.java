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

public interface IQuantizedDependentCostCalculator extends IQuantizedTaskNode {
    default IQuantityRelationship cost() {
        switch (type()) {
            case SERIAL:
            case PARALLEL_ALL:
                return q -> {
                    double sum = 0;
                    for (IQuantizedParentTaskRelationship relationship : childTasks()) {
                        sum += relationship.cost().value(q);
                    }
                    return sum;
                };
            case ANY_ONE_OF: // TODO this could be smarter about allocating
                return q -> {
                    double min = -1;
                    for (IQuantizedParentTaskRelationship relationship : childTasks()) {
                        double cost = relationship.cost().value(q);
                        if (min == -1 || cost < min) {
                            min = cost;
                        }
                    }
                    return min;
                };
            default:
                throw new UnsupportedOperationException();

        }
    }
}
