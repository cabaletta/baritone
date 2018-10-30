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

import java.util.List;

public abstract class TaskNode<T extends IChildTaskRelationship & ITaskRelationshipBase, S extends IParentTaskRelationship & ITaskRelationshipBase> extends Task<T> implements ITaskNodeBase<T, S> {

    List<S> childRelationships;
    public final DependencyType type;

    public TaskNode(DependencyType type) {
        this.type = type;
    }

    @Override
    public List<S> childTasks() {
        return childRelationships;
    }

    @Override
    public DependencyType type() {
        return type;
    }

    @Override
    public void addChild(S relationship) {
        if (relationship.parentTask() != this) {
            throw new IllegalArgumentException();
        }
        if (relationship.type() != type) {
            throw new IllegalArgumentException();
        }
        childRelationships.add(relationship);
    }
}
