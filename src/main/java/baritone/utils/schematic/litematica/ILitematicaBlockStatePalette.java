package baritone.utils.schematic.litematica;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagList;

public interface ILitematicaBlockStatePalette {
    int idFor(IBlockState state);

    IBlockState get(int key);

    void readNBT(NBTTagList paletteNBT);
}
