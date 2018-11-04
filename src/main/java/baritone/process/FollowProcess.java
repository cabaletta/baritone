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

import baritone.Baritone;
import baritone.api.process.IFollowProcess;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * Follow an entity
 *
 * @author leijurv
 */
public final class FollowProcess extends BaritoneProcessHelper implements IFollowProcess {

    private Entity following;

    public FollowProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public PathingCommand onTick() {
        // lol this is trashy but it works
        BlockPos pos;
        if (Baritone.settings().followOffsetDistance.get() == 0) {
            pos = following.getPosition();
        } else {
            GoalXZ g = GoalXZ.fromDirection(following.getPositionVector(), Baritone.settings().followOffsetDirection.get(), Baritone.settings().followOffsetDistance.get());
            pos = new BlockPos(g.getX(), following.posY, g.getZ());
        }
        return new PathingCommand(new GoalNear(pos, Baritone.settings().followRadius.get()), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
    }

    @Override
    public boolean isActive() {
        return following != null;
    }

    @Override
    public void onLostControl() {
        following = null;
    }

    @Override
    public void follow(Entity entity) {
        this.following = entity;
    }

    @Override
    public Entity following() {
        return this.following;
    }
}
