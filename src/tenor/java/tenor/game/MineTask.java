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

public class MineTask extends QuantizedTaskPriorityAllocationCache implements ISingleParentQuantizedPriorityAllocator, IQuantizedDependentCostCalculator {

    // TODO shared claims of block locations in the world across all mine tasks across all bots

    final AquireItemTask parent;
    final MineWithToolTask[] children;


    public MineTask(AquireItemTask parent) {
        super(parent.bot, DependencyType.ANY_ONE_OF);
        this.parent = parent;
        addParent(parent);
        String[] tools = getToolsCapableOfMining(parent.item);
        children = new MineWithToolTask[tools.length];
        for (int i = 0; i < children.length; i++) {
            children[i] = new MineWithToolTask(this, tools[i]);
        }
    }

    static String[] getToolsCapableOfMining(String block) {
        switch (block) {
            case "iron_ore":
                return new String[]{"stone_pickaxe"};
            case "stone":
            case "cobblestone":
                return new String[]{"wooden_pickaxe"};
            case "log":
                return new String[]{"hand"};
            default:
                throw new IllegalStateException(block);
        }
    }
}
