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

package baritone.bot.pathing.path;

import baritone.bot.Baritone;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.utils.Helper;
import baritone.bot.utils.Utils;
import baritone.bot.utils.pathing.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.List;

/**
 * @author leijurv
 */
public interface IPath extends Helper {

    /**
     * Ordered list of movements to carry out.
     * movements.get(i).getSrc() should equal positions.get(i)
     * movements.get(i).getDest() should equal positions.get(i+1)
     * movements.size() should equal positions.size()-1
     */
    List<Movement> movements();

    /**
     * All positions along the way.
     * Should begin with the same as getSrc and end with the same as getDest
     */
    List<BetterBlockPos> positions();

    /**
     * Number of positions in this path
     *
     * @return Number of positions in this path
     */
    default int length() {
        return positions().size();
    }

    /**
     * What's the next step
     *
     * @param currentPosition the current position
     * @return
     */
    default Movement subsequentMovement(BlockPos currentPosition) {
        List<BetterBlockPos> pos = positions();
        List<Movement> movements = movements();
        for (int i = 0; i < pos.size(); i++) {
            if (currentPosition.equals(pos.get(i))) {
                return movements.get(i);
            }
        }
        throw new UnsupportedOperationException(currentPosition + " not in path");
    }

    /**
     * Determines whether or not a position is within this path.
     *
     * @param pos The position to check
     * @return Whether or not the specified position is in this class
     */
    default boolean isInPath(BlockPos pos) {
        return positions().contains(pos);
    }

    default Tuple<Double, BlockPos> closestPathPos(double x, double y, double z) {
        double best = -1;
        BlockPos bestPos = null;
        for (BlockPos pos : positions()) {
            double dist = Utils.distanceToCenter(pos, x, y, z);
            if (dist < best || best == -1) {
                best = dist;
                bestPos = pos;
            }
        }
        return new Tuple<>(best, bestPos);
    }

    /**
     * Where does this path start
     */
    default BetterBlockPos getSrc() {
        return positions().get(0);
    }

    /**
     * Where does this path end
     */
    default BetterBlockPos getDest() {
        List<BetterBlockPos> pos = positions();
        return pos.get(pos.size() - 1);
    }

    default double ticksRemainingFrom(int pathPosition) {
        double sum = 0;
        //this is fast because we aren't requesting recalculation, it's just cached
        for (int i = pathPosition; i < movements().size(); i++) {
            sum += movements().get(i).getCost(null);
        }
        return sum;
    }

    int getNumNodesConsidered();

    default IPath cutoffAtLoadedChunks() {
        for (int i = 0; i < positions().size(); i++) {
            BlockPos pos = positions().get(i);
            if (Minecraft.getMinecraft().world.getChunk(pos) instanceof EmptyChunk) {
                displayChatMessageRaw("Cutting off path at edge of loaded chunks");
                displayChatMessageRaw("Length decreased by " + (positions().size() - i - 1));
                return new CutoffPath(this, i);
            }
        }
        displayChatMessageRaw("Path ends within loaded chunks");
        return this;
    }

    default IPath staticCutoff(Goal destination) {
        if (length() < Baritone.settings().pathCutoffMinimumLength.get()) {
            return this;
        }
        if (destination == null || destination.isInGoal(getDest())) {
            return this;
        }
        double factor = Baritone.settings().pathCutoffFactor.get();
        int newLength = (int) (length() * factor);
        displayChatMessageRaw("Static cutoff " + length() + " to " + newLength);
        return new CutoffPath(this, newLength);
    }
}
