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
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.IFollowProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Follow an entity
 *
 * @author leijurv
 */
public final class FollowProcess extends BaritoneProcessHelper implements IFollowProcess {

    private Predicate<Entity> filter;
    private List<Entity> cache;
    private List<String> itemFilter = new ArrayList<>();

    public FollowProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        scanWorld();
        Goal goal = new GoalComposite(cache.stream().map(this::towards).toArray(Goal[]::new));
        return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    private Goal towards(Entity following) {
        BlockPos pos;
        if (Baritone.settings().followOffsetDistance.value == 0) {
            pos = new BlockPos(following);
        } else {
            GoalXZ g = GoalXZ.fromDirection(following.getPositionVector(), Baritone.settings().followOffsetDirection.value, Baritone.settings().followOffsetDistance.value);
            pos = new BlockPos(g.getX(), following.posY, g.getZ());
        }
        return new GoalNear(pos, Baritone.settings().followRadius.value);
    }


    private boolean followable(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.isDead) {
            return false;
        }
        if (entity.equals(ctx.player())) {
            return false;
        }
        return ctx.world().loadedEntityList.contains(entity);
    }

    private void scanWorld() {
        cache = Stream.of(ctx.world().loadedEntityList, ctx.world().playerEntities)
                .flatMap(List::stream)
                .filter(this::followable)
                .filter(this.filter)
                .distinct()
                .collect(Collectors.toList());

        //TODO i really hate this but idk how i could make this into a stream/lambda solution
        //TODO the booleans used here are a war crime
        boolean andCondition = itemFilter != null;
        for (Entity entity: cache) {
            andCondition = andCondition && entity instanceof EntityItem;
        }
        if (andCondition) {
            Iterator<Entity> iterator = cache.iterator();
            while (iterator.hasNext()) {
                EntityItem item = (EntityItem) iterator.next();
                boolean orCondition = false;
                for (String filter : itemFilter) {
                    ResourceLocation resLoc = Item.REGISTRY.getNameForObject(item.getItem().getItem());
                    orCondition = orCondition || resLoc.getPath().equalsIgnoreCase(filter) || resLoc.toString().equalsIgnoreCase(filter);
                }
                if (!orCondition) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public boolean isActive() {
        if (filter == null) {
            return false;
        }
        scanWorld();
        return !cache.isEmpty();
    }

    @Override
    public void onLostControl() {
        filter = null;
        cache = null;
        itemFilter = null;
    }

    @Override
    public String displayName0() {
        return "Following " + cache;
    }

    @Override
    public void follow(Predicate<Entity> filter) {
        this.filter = filter;
    }

    @Override
    public void setItemFilter(List<String> itemFilter) {
        this.itemFilter = itemFilter;
    }

    @Override
    public List<String> getItemFilter() {
        return itemFilter;
    }

    @Override
    public List<Entity> following() {
        return cache;
    }

    @Override
    public Predicate<Entity> currentFilter() {
        return filter;
    }
}
