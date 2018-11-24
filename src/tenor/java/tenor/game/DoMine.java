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

import tenor.IQuantityRelationship;
import tenor.ISingleParentQuantizedPriorityAllocator;
import tenor.QuantizedTaskLeaf;

public class DoMine extends QuantizedTaskLeaf implements ISingleParentQuantizedPriorityAllocator {
    final MineWithToolTask parent;

    public DoMine(MineWithToolTask parent) {
        super(parent.bot);
        this.parent = parent;
        addParent(parent);
    }

    @Override
    public IQuantityRelationship cost() {
        return null;
    }
}
