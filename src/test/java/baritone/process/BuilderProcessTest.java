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

package baritone.process;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BuilderProcessTest {

    @BeforeClass
    public static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void satisfiedUnreachableBreakGoalDoesNotPreventPathing() {
        BetterBlockPos feet = new BetterBlockPos(0, 0, 0);
        List<Goal> goals = new ArrayList<>();

        BuilderProcess.addGoalIfNotAlreadySatisfied(
                goals,
                new BuilderProcess.GoalBreak(new BlockPos(1, 0, 0)),
                feet
        );
        BuilderProcess.addGoalIfNotAlreadySatisfied(
                goals,
                new BuilderProcess.GoalBreak(new BlockPos(3, 0, 0)),
                feet
        );

        assertEquals(1, goals.size());
        assertFalse(new GoalComposite(goals.toArray(new Goal[0])).isInGoal(feet));
    }
}
