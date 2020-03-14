package baritone.utils.accessor;

import net.minecraft.block.state.IBlockState;

public interface IBlockStateContainer {

    IBlockState getAtPalette(int index);

    int[] storageArray();
}
