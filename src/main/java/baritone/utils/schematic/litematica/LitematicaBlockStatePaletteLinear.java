package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import java.util.function.Function;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette
{
    private final BlockState[] states;
    private final ILitematicaBlockStatePaletteResizer resizeHandler;
    private final Function<CompoundNBT, BlockState> deserializer;
    private final int bits;
    private int arraySize;

    LitematicaBlockStatePaletteLinear(int bits, ILitematicaBlockStatePaletteResizer paletteResizer, Function<CompoundNBT, BlockState> deserializer) {
        this.states = new BlockState[1 << bits];
        this.bits = bits;
        this.resizeHandler = paletteResizer;
        this.deserializer = deserializer;
    }

    public int idFor(BlockState state) {
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
        } else {
            return this.resizeHandler.onResize(this.bits + 1, state);
        }
    }

    public void readNBT(ListNBT nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            BlockState bst = this.deserializer.apply(nbt.getCompound(i));
            if (bst != LitematicaBlockStateContainer.AIR_DEFAULT_STATE) {
                this.states[i] = bst;
            }
        }

        this.arraySize = nbt.size();
    }
    public BlockState get(int indexKey) {
        return (indexKey >= 0 && indexKey < this.arraySize ? this.states[indexKey] : null);
    }
}
