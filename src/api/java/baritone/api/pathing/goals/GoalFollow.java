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

import net.minecraft.world.entity.Entity;

/**
 * A dynamic goal that continuously targets a living entity's current block
 * position. Unlike static goals, {@link #heuristic} and {@link #isInGoal}
 * re-evaluate against the entity's <em>live</em> coordinates every time they
 * are called, making this class correct when used with
 * {@link baritone.api.process.PathingCommandType#REVALIDATE_GOAL_AND_PATH}.
 *
 * <p>GoalRunAway and GoalComposite already exist in Baritone's API; this class
 * fills the remaining gap for player/entity following at the goal level.
 */
public class GoalFollow implements Goal {

    private final Entity target;
    private final int    range;

    /**
     * @param target the entity to track (must not be null)
     * @param range  acceptable proximity in blocks; 0 means stand on the same block
     */
    public GoalFollow(Entity target, int range) {
        if (target == null) {
            throw new IllegalArgumentException("target entity must not be null");
        }
        this.target = target;
        this.range  = Math.max(0, range);
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        double dx = x + 0.5 - target.getX();
        double dy = y       - target.getY();
        double dz = z + 0.5 - target.getZ();
        return dx * dx + dy * dy + dz * dz <= (double) range * range;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int tx = (int) Math.floor(target.getX());
        int ty = (int) Math.floor(target.getY());
        int tz = (int) Math.floor(target.getZ());
        return GoalBlock.calculate(x - tx, y - ty, z - tz);
    }

    public Entity getTarget() {
        return target;
    }

    public int getRange() {
        return range;
    }

    @Override
    public String toString() {
        return String.format("GoalFollow{target=%s, range=%d}",
                target.getName().getString(), range);
    }
}
