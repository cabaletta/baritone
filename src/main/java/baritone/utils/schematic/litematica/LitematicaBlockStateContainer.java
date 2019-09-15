package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class LitematicaBlockStateContainer implements ILitematicaBlockStatePaletteResizer {

    static final BlockState AIR_DEFAULT_STATE = Blocks.AIR.getDefaultState();
    private final Function<CompoundNBT, BlockState> deserializer;
    private final BlockState defaultState;
    private final long[] longArray;
    private final int storageSize;
    private final BlockPos pos;
    private int bits;
    private BitArray storage;
    private ILitematicaBlockStatePalette palette;

    public LitematicaBlockStateContainer(BlockPos pos, ListNBT paletteList, long[] longArray) {
        this.deserializer = in -> NBTUtil.readBlockState((CompoundNBT) in);
        this.defaultState = AIR_DEFAULT_STATE;
        this.longArray = longArray;
        this.pos = pos;
        this.storageSize = pos.getX() * pos.getY() * pos.getZ();
        this.readChunkPalette(paletteList);
    }

    private void setBits(int newBits) {
        if (newBits != this.bits) {
            this.bits = newBits;

            // Palette init
            if (this.bits > 4) {
                this.palette = new LitematicaBlockStatePaletteHashMap(this.bits, this, this.deserializer);
            }
            else {
                this.bits = Math.max(this.bits, 2);
                this.palette = new LitematicaBlockStatePaletteLinear(this.bits, this, this.deserializer);
            }
            this.palette.idFor(AIR_DEFAULT_STATE);

            // Storage init
            if (this.longArray == null) {
                this.storage = new BitArray(this.bits, this.storageSize);
            }
            else {
                this.storage = new BitArray(this.bits, this.storageSize, longArray);
            }
        }
    }

    private void readChunkPalette(ListNBT paletteNBT) {
        int i = Math.max(2, MathHelper.log2DeBruijn(paletteNBT.size()));
        if (i != this.bits) {
            this.setBits(i);
        }
        this.palette.readNBT(paletteNBT);
    }

    private int getIndex(int x, int y, int z) {
        return (y * pos.getX() * pos.getZ()) + z * pos.getX() + x;
    }

    public BlockState get(int x, int y, int z) {
        int index = this.getIndex(x, y, z);
        BlockState state = this.palette.get(this.storage.getAt(index));
        return (state == null ? this.defaultState : state);
    }

    public int onResize(int bits, BlockState state) {
        BitArray bitarray = this.storage;
        ILitematicaBlockStatePalette iblockstatepalette = this.palette;
        this.setBits(bits);

        for(int i = 0; i < bitarray.size(); ++i) {
            BlockState t = iblockstatepalette.get(bitarray.getAt(i));
            if (t != null) {
                this.set(i, t);
            }
        }
        return this.palette.idFor(state);
    }

    protected void set(int index, BlockState state) {
        int i = this.palette.idFor(state);
        this.storage.setAt(index, i);
    }
}
