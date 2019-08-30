package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nonnull;
import java.util.List;

public interface IBlockFilter {
    /**
     * @param blockstate The blockstate of the block to test.
     * @return If that blockstate passes this filter.
     */
    boolean selected(@Nonnull IBlockState blockstate);

    /**
     * @return A possibly incomplete list of blocks this filter selects. Not all states of each block may be selected,
     * and this may not contain all selected blocks, but every block on this list is guaranteed to have a selected
     * state.
     */
    List<Block> blocks();
}
