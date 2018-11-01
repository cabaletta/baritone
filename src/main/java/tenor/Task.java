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

import java.util.ArrayList;
import java.util.List;

public abstract class Task<T extends IChildTaskRelationship & ITaskRelationshipBase> implements ITask<T> {

    public final Bot bot;

    private final List<T> parentRelationships = new ArrayList<>();

    public Task(Bot bot) {
        this.bot = bot;
        registry().register(this);
    }

    @Override
    public Bot bot() {
        return bot;
    }

    @Override
    public List<T> parentTasks() {
        return parentRelationships;
    }

    @Override
    public void addParent(T relationship) {
        if (relationship.childTask() != this) {
            throw new IllegalArgumentException();
        }
        relationship.parentTask().addChild((IParentTaskRelationship) relationship);
        parentRelationships.add(relationship);
    }
}
