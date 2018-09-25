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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.IFollowBehavior;
import baritone.api.event.events.TickEvent;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.utils.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * Follow an entity
 *
 * @author leijurv
 */
public final class FollowBehavior extends Behavior implements IFollowBehavior, Helper {

    public static final FollowBehavior INSTANCE = new FollowBehavior();

    private Entity following;

    private FollowBehavior() {}

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            following = null;
            return;
        }
        if (following == null) {
            return;
        }
        // lol this is trashy but it works
        BlockPos pos;
        if (Baritone.settings().followOffsetDistance.get() == 0) {
            pos = following.getPosition();
        } else {
            GoalXZ g = GoalXZ.fromDirection(following.getPositionVector(), Baritone.settings().followOffsetDirection.get(), Baritone.settings().followOffsetDistance.get());
            pos = new BlockPos(g.getX(), following.posY, g.getZ());
        }
        PathingBehavior.INSTANCE.setGoal(new GoalNear(pos, Baritone.settings().followRadius.get()));
        PathingBehavior.INSTANCE.revalidateGoal();
        PathingBehavior.INSTANCE.path();
    }

    @Override
    public void follow(Entity entity) {
        this.following = entity;
    }

    @Override
    public Entity following() {
        return this.following;
    }

    @Override
    public void cancel() {
        PathingBehavior.INSTANCE.cancel();
        follow(null);
    }
}
