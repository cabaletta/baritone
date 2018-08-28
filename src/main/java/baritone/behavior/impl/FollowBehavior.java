/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior.impl;

import baritone.api.event.events.TickEvent;
import baritone.behavior.Behavior;
import baritone.pathing.goals.GoalNear;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * Follow an entity
 *
 * @author leijurv
 */
public class FollowBehavior extends Behavior {
    public static final FollowBehavior INSTANCE = new FollowBehavior();

    private FollowBehavior() {
    }

    Entity following;

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (following == null) {
            return;
        }
        // lol this is trashy but it works
        PathingBehavior.INSTANCE.setGoal(new GoalNear(new BlockPos(following), 3));
        PathingBehavior.INSTANCE.path();
    }

    public void follow(Entity follow) {
        this.following = follow;
    }

    public void cancel() {
        PathingBehavior.INSTANCE.cancel();
        follow(null);
    }
}
