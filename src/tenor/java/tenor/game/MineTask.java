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

public class MineTask extends QuantizedTaskNode implements ISingleParentQuantizedPriorityAllocator {

    // TODO shared claims of block locations in the world across all mine tasks across all bots

    final AquireItemTask parent;

    final DoMine doMine;


    public MineTask(AquireItemTask parent) {
        super(parent.bot, DependencyType.ANY_ONE_OF);
        this.parent = parent;
        addParent(parent);
        this.doMine = new DoMine(this);
    }

    @Override
    public IQuantityRelationship cost() {
        return x -> x * 324232;
    }

    @Override
    public double priorityAllocatedTo(IQuantizedParentTaskRelationship child, int quantity) {
        return 0;
    }
}
