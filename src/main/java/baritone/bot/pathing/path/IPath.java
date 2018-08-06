package baritone.bot.pathing.path;

import baritone.bot.pathing.movement.Movement;
import baritone.bot.utils.Utils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;

/**
 * @author leijurv
 */
public interface IPath {

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
    List<BlockPos> positions();

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
        List<BlockPos> pos = positions();
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
    default BlockPos getSrc() {
        return positions().get(0);
    }

    /**
     * Where does this path end
     */
    default BlockPos getDest() {
        List<BlockPos> pos = positions();
        return pos.get(pos.size() - 1);
    }

    /**
     * For rendering purposes, what blocks should be highlighted in red
     *
     * @return an unordered collection of positions
     */
    Collection<BlockPos> getBlocksToBreak();

    /**
     * For rendering purposes, what blocks should be highlighted in green
     *
     * @return an unordered collection of positions
     */
    Collection<BlockPos> getBlocksToPlace();

    int getNumNodesConsidered();
}
