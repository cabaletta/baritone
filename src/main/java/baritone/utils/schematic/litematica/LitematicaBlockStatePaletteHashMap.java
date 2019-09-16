package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.IntIdentityHashBiMap;

import java.util.function.Function;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette {
    private final IntIdentityHashBiMap<BlockState> statePaletteMap;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final Function<CompoundNBT, BlockState> deserializer;
    private final int bits;

    LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer, Function<CompoundNBT, BlockState> deserializerIn) {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.deserializer = deserializerIn;
        this.statePaletteMap = new IntIdentityHashBiMap<BlockState>(1 << bitsIn);
    }

    public int idFor(BlockState state) {
        int i = this.statePaletteMap.getId(state);
        if (i == -1) {
            i = this.statePaletteMap.add(state);
            if (i >= 1 << this.bits) {
                i = this.paletteResizer.onResize(this.bits + 1, state);
            }
        }

        return i;
    }

    public BlockState get(int indexKey) {
        return this.statePaletteMap.getByValue(indexKey);
    }

    public void readNBT(ListNBT nbt) {
        this.statePaletteMap.clear();

        for (int i = 0; i < nbt.size(); ++i) {
            BlockState bst = this.deserializer.apply(nbt.getCompound(i));
            if (bst != LitematicaBlockStateContainer.AIR_DEFAULT_STATE) {
                this.statePaletteMap.add(bst);
            }
        }
    }
}
