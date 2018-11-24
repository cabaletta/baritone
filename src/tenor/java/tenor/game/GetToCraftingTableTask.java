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

public class GetToCraftingTableTask extends SingularTaskLeaf {

    private final ComputationRequest craftingTable;

    public GetToCraftingTableTask(Bot bot) {
        super(bot);
        registry().registerSingleton(this);
        this.craftingTable = ComputationRequestManager.INSTANCE.getByGoal(this, "GoalGetToBlock crafting_table"); // idk
    }

    @Override
    public double cost() {
        Double d = craftingTable.cost();
        if (d == null) {
            // unknown, has not been calculated yet either way
            return 1000D; // estimate
        }
        return d;
    }

    @Override
    public double priority() {
        return parentTasks().stream().mapToDouble(ISingularChildTaskRelationship::allocatedPriority).sum();
    }
}
