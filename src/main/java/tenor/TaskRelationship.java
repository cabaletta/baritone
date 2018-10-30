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

public class TaskRelationship<P extends ITaskNodeBase, C extends ITask> implements ITaskRelationshipBase<P, C> {

    public final P parent;
    public final C child;
    public final DependencyType type;

    public TaskRelationship(P parent, C child, DependencyType type) {
        this.parent = parent;
        this.child = child;
        this.type = type;
        if (parent.type() != type) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public P parentTask() {
        return parent;
    }

    @Override
    public C childTask() {
        return child;
    }

    @Override
    public DependencyType type() {
        return type;
    }
}
