package baritone.utils.schematic.litematica;

import net.minecraft.block.state.IBlockState;

public interface ILitematicaBlockStatePaletteResizer {
    int onResize(int bits, IBlockState newState);
}
