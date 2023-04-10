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

package baritone.api.pathing.goals;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class GoalPatrol implements Goal, IGoalRenderPos {

    private List<Goal> waypoints;
    private Mode mode;
    private int index;

    public GoalPatrol(List<Goal> waypoints, Mode mode, int index) {
        this.waypoints = waypoints;
        this.mode = mode;
        this.index = index;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        boolean inGoal = waypoints.get(index).isInGoal(x, y, z);
        BlockPos playerPos = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().playerFeet();
        if (playerPos.getX() == x && playerPos.getY() == y && playerPos.getZ() == z) {
            if (inGoal) {
                return getNextOrReturnTrue();
            }
            return false;
        } else {
            return inGoal;
        }
    }

    @Override
    public double heuristic(int x, int y, int z) {
        return waypoints.get(index).heuristic(x, y, z);
    }

    private boolean getNextOrReturnTrue() {
        if(waypoints.size() == 1) {
            return true;
        } else {
            switch (mode) {
                case ONEWAY:
                    waypoints.remove(0);
                    break;
                case CIRCLE:
                    index++;
                    index = index >= waypoints.size() ? 0 : index;
                    break;
                case PENDULUMUP:
                    index++;
                    mode = index >= waypoints.size() - 1 ? Mode.PENDULUMDOWN : mode;
                    break;
                case PENDULUMDOWN:
                    index--;
                    mode = index <= 0 ? Mode.PENDULUMUP : mode;
                    break;
                case RANDOME:
                    index = (int) Math.floor(Math.random() * waypoints.size());
                    break;
            }
            for (IBaritone process : BaritoneAPI.getProvider().getAllBaritones()) {
                if (process instanceof ICustomGoalProcess) {
                    ((ICustomGoalProcess) process).setGoal(waypoints.get(index));
                }
            }
            return false;
        }
    }

    @Override
    public BlockPos getGoalPos() {
        return ((GoalBlock)waypoints.get(index)).getGoalPos();
    }

    public enum Mode {
        ONEWAY("oneway"),
        CIRCLE("circle"),
        PENDULUMUP("pendulum"),
        PENDULUMDOWN(),
        RANDOME("random");

        private final String[] names;

        Mode(String... names) {
            this.names = names;
        }

        public static Mode getByNameOrDefault(String name) {
            for (Mode mode : Mode.values()) {
                for (String alias : mode.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return mode;
                    }
                }
            }
            return Mode.ONEWAY;
        }


        public String getName() {
            if (names.length > 0) {
                return names[0];
            } else {
                return "";
            }
        }
    }
}
