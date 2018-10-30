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

    @Override
    default IQuantityRelationship cost() {
        switch (type()) {
            case SERIAL:
            case PARALLEL_ALL:
                return q -> childTasks().stream()
                        .map(IQuantizedParentTaskRelationship::cost)
                        .mapToDouble(relationship -> relationship.value(q))
                        .sum();
            case ANY_ONE_OF: // TODO this could be smarter about allocating
                return q -> childTasks().stream()
                        .map(IQuantizedParentTaskRelationship::cost)
                        .mapToDouble(relationship -> relationship.value(q))
                        .min().orElse(-1);
            default:
                throw new UnsupportedOperationException();

        }
    }
}
