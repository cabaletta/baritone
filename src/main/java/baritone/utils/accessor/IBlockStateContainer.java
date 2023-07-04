package baritone.utils.accessor;

import net.minecraft.util.BitArray;
import net.minecraft.world.chunk.IBlockStatePalette;

public interface IBlockStateContainer<T> {

    IBlockStatePalette<T> getPalette();

    BitArray getStorage();

    T getAtPalette(int index);

    int[] storageArray();
}
