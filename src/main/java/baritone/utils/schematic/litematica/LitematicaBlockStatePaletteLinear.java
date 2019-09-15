package baritone.utils.schematic.litematica;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.function.Function;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette {
    private final IBlockState[] states;
    private final ILitematicaBlockStatePaletteResizer resizeHandler;
    private final Function<NBTTagCompound, IBlockState> deserializer;
    private final int bits;
    private int arraySize;

    LitematicaBlockStatePaletteLinear(int bits, ILitematicaBlockStatePaletteResizer paletteResizer, Function<NBTTagCompound, IBlockState> deserializer) {
        this.states = new IBlockState[1 << bits];
        this.bits = bits;
        this.resizeHandler = paletteResizer;
        this.deserializer = deserializer;
    }

    public int idFor(IBlockState state) {
        for(int i = 0; i < this.arraySize; ++i) {
            if (this.states[i] == state) {
                return i;
            }
        }

        int j = this.arraySize;
        if (j < this.states.length) {
            this.states[j] = state;
            ++this.arraySize;
            return j;
        }
        else {
            return this.resizeHandler.onResize(this.bits + 1, state);
        }
    }

    public void readNBT(NBTTagList nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            IBlockState bst = this.deserializer.apply(nbt.getCompound(i));
            if (bst != LitematicaBlockStateContainer.AIR_DEFAULT_STATE) {
                this.states[i] = bst;
            }
        }

        this.arraySize = nbt.size();
    }

    public IBlockState get(int indexKey) {
        return (indexKey >= 0 && indexKey < this.arraySize ? this.states[indexKey] : null);
    }
}
