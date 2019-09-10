 package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;

public interface ILitematicaBlockStatePaletteResizer
{
    int onResize(int bits, BlockState state);
}
