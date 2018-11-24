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

import tenor.DependencyType;
import tenor.IQuantizedDependentCostCalculator;
import tenor.ISingleParentQuantizedPriorityAllocator;
import tenor.QuantizedTaskPriorityAllocationCache;

public class MineWithToolTask extends QuantizedTaskPriorityAllocationCache implements ISingleParentQuantizedPriorityAllocator, IQuantizedDependentCostCalculator {

    public final String tool;
    MineTask parent;

    AquireItemTask aquireTool;
    // TODO locate task?
    DoMine doMine;


    public MineWithToolTask(MineTask parent, String tool) {
        super(parent.bot, DependencyType.SERIAL);
        this.tool = tool;
        this.parent = parent;
        addParent(parent);
        this.aquireTool = registry().getItemBased(AquireItemTask.class, tool);
        aquireTool.addParent(this); // we aren't constructing this, so need to add us as a parent manually
        this.doMine = new DoMine(this);
    }
}
