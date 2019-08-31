package baritone.utils.accessor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BitArray;
import net.minecraft.world.chunk.IBlockStatePalette;

public interface IBlockStateContainer {
    IBlockStatePalette getPalette();

    BitArray getStorage();

    IBlockState getFast(int index);

    default IBlockState getFast(int x, int y, int z) {
        return getFast(y << 8 | z << 4 | x);
    };
}
