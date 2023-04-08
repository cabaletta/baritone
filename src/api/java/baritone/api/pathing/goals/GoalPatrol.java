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
import baritone.api.utils.Helper;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GoalPatrol implements Goal{

    private List<Goal> waypoints;
    private Mode mode;
    private int index;

    public GoalPatrol(List<Goal> waypoints, Mode mode, int index) {
        this.waypoints = waypoints;
        this.mode = mode;
        this.index = index;
    }
    public GoalPatrol(List<Goal> waypoints, Mode mode) {
        new GoalPatrol(waypoints, mode, 0);
    }

    public GoalPatrol(List<Goal> waypoints) {
        new GoalPatrol(waypoints, Mode.ONEWAY, 0);
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
        if(mode == Mode.ONEWAY && index == waypoints.size()-1 || waypoints.size() == 1) { //if we move oneway and are at the last waypoint or if we only have 1 waypoint we stop
            return true;
        } else {
            switch (mode) {
                case ONEWAY:
                    index++;
                    break;
                case CIRCLE:
                    /*if (index >= waypoints.size() - 1) {
                        index = 0;
                    } else {
                        index++;
                    }/**/
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
    public void setIndex(int Index) {
        this.index = index;
    }
    public void setMode(String name) {
        mode = Mode.getByNameOrDefault(name);
    }

    public Mode getMode() {
        return mode;
    }

    public void setWaypoints(List<Goal> waypoints) {
        this.waypoints = waypoints;
    }

    /*@Override
    public boolean isInGoal(BlockPos pos) {
        return Goal.super.isInGoal(pos);
    }

    @Override
    public double heuristic(BlockPos pos) {
        return Goal.super.heuristic(pos);
    }

    @Override
    public double heuristic() {
        return Goal.super.heuristic();
    }/**/

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

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();
            for (Mode mode : Mode.values()) {
                names.addAll(Arrays.asList(mode.names));
            }
            String[] adfs = names.toArray(new String[0]);
            return adfs;
        }
    }
}
