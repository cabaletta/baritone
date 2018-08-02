package baritone.bot.pathing;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;

public interface IPath {

    /**
     * All positions along the way.
     * Should begin with the same as getSrc and end with the same as getDest
     */
    List<BlockPos> positions();

    /**
     * Where does this path start
     */
    BlockPos getSrc();

    /**
     * Where does this path end
     */
    BlockPos getDest();

    /**
     * For rendering purposes, what blocks should be highlighted in red
     * @return an unordered collection of positions
     */
    Collection<BlockPos> getBlocksToMine();

    /**
     * For rendering purposes, what blocks should be highlighted in green
     * @return an unordered collection of positions
     */
    Collection<BlockPos> getBlocksToPlace();
}
