package baritone.bot.pathing.goals;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Don't get into the block, but get directly adjacent to it.
 * Useful for chests.
 * @author avecowa
 */
public class GoalGetToBlock extends GoalComposite {

    public GoalGetToBlock(BlockPos pos) {
        super(adjacentBlocks(pos));
    }

    private static BlockPos[] adjacentBlocks(BlockPos pos) {
        BlockPos[] sides = new BlockPos[6];
        for (int i = 0; i < 6; i++) {
            sides[i] = pos.offset(EnumFacing.values()[i]);
        }
        return sides;
    }
}
