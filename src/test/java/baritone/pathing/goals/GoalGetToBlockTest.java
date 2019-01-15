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

package baritone.pathing.goals;

import baritone.api.pathing.goals.GoalGetToBlock;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class GoalGetToBlockTest {

    @Test
    public void isInGoal() {
        List<String> acceptableOffsets = new ArrayList<>(Arrays.asList("0,0,0", "0,0,1", "0,0,-1", "1,0,0", "-1,0,0", "0,-1,1", "0,-1,-1", "1,-1,0", "-1,-1,0", "0,1,0", "0,-1,0", "0,-2,0"));
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    boolean inGoal = new GoalGetToBlock(new BlockPos(0, 0, 0)).isInGoal(new BlockPos(x, y, z));
                    String repr = x + "," + y + "," + z;
                    System.out.println(repr + " " + inGoal);
                    if (inGoal) {
                        assertTrue(repr, acceptableOffsets.contains(repr));
                        acceptableOffsets.remove(repr);
                    }
                }
            }
        }
        assertTrue(acceptableOffsets.toString(), acceptableOffsets.isEmpty());
    }
}
