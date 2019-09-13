package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.ListNBT;

public interface ILitematicaBlockStatePalette
{
    int idFor(BlockState state);

    BlockState get(int key);

    void readNBT(ListNBT paletteNBT);

}
