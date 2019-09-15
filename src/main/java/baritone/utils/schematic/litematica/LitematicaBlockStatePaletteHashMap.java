package baritone.utils.schematic.litematica;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IntIdentityHashBiMap;

import java.util.function.Function;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette {
    private final IntIdentityHashBiMap<IBlockState> statePaletteMap;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final Function<NBTTagCompound, IBlockState> deserializer;
    private final int bits;

    LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer, Function<NBTTagCompound, IBlockState> deserializerIn) {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.deserializer = deserializerIn;
        this.statePaletteMap = new IntIdentityHashBiMap<>(1 << bitsIn);
    }

    public int idFor(IBlockState state) {
        int i = this.statePaletteMap.getId(state);
        if (i == -1) {
            i = this.statePaletteMap.add(state);
            if (i >= 1 << this.bits) {
                i = this.paletteResizer.onResize(this.bits + 1, state);
            }
        }

        return i;
    }

    public IBlockState get(int indexKey) {
        return this.statePaletteMap.get(indexKey);
    }

    public void readNBT(NBTTagList nbt) {
        this.statePaletteMap.clear();

        for(int i = 0; i < nbt.size(); ++i) {
            IBlockState bst = this.deserializer.apply(nbt.getCompound(i));
            if (bst != LitematicaBlockStateContainer.AIR_DEFAULT_STATE) {
                this.statePaletteMap.add(bst);
            }
        }
    }
}
