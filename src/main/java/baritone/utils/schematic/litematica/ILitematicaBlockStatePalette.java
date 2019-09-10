package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.ListNBT;

import javax.annotation.Nullable;

public interface ILitematicaBlockStatePalette
{
    /**
     * Gets the palette id for the given block state and adds
     * the state to the palette if it doesn't exist there yet.
     */
    int idFor(BlockState state);

    /**
     * Gets the block state by the palette id.
     */
    @Nullable
    BlockState getBlockState(int indexKey);

    int getPaletteSize();

    void readFromNBT(ListNBT tagList);

    ListNBT writeToNBT();
}
