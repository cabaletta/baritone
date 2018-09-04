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
import baritone.chunk.ChunkPacker;
import baritone.chunk.WorldProvider;
import baritone.pathing.goals.Goal;
import baritone.pathing.goals.GoalComposite;
import baritone.pathing.goals.GoalTwoBlocks;
import baritone.utils.BlockStateInterface;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mine blocks of a certain type
 *
 * @author leijurv
 */
public class MineBehavior extends Behavior {
    public static final MineBehavior INSTANCE = new MineBehavior();

    private MineBehavior() {
    }

    List<String> mining;

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (mining == null) {
            return;
        }
        List<BlockPos> locs = new ArrayList<>();
        for (String m : mining) {
            locs.addAll(WorldProvider.INSTANCE.getCurrentWorld().cache.getLocationsOf(m, 1, 1));
        }
        BlockPos playerFeet = playerFeet();
        locs.sort(Comparator.comparingDouble(playerFeet::distanceSq));

        // remove any that are within loaded chunks that aren't actually what we want
        locs.removeAll(locs.stream()
                .filter(pos -> !(world().getChunk(pos) instanceof EmptyChunk))
                .filter(pos -> !mining.contains(ChunkPacker.blockToString(BlockStateInterface.get(pos).getBlock()).toLowerCase()))
                .collect(Collectors.toList()));
        if (locs.size() > 30) {
            locs = locs.subList(0, 30);
        }
        if (locs.isEmpty()) {
            displayChatMessageRaw("No locations for " + mining + " known, cancelling");
            cancel();
            return;
        }
        PathingBehavior.INSTANCE.setGoal(new GoalComposite(locs.stream().map(GoalTwoBlocks::new).toArray(Goal[]::new)));
        PathingBehavior.INSTANCE.path();
    }

    public void mine(String... mining) {
        this.mining = new ArrayList<>(Arrays.asList(mining));
    }

    public void cancel() {
        PathingBehavior.INSTANCE.cancel();
        mine(null);
    }
}
