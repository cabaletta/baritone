package baritone.utils.schematic.litematica;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class LitematicaBlockStateContainer implements ILitematicaBlockStatePaletteResizer {

    static final IBlockState AIR_DEFAULT_STATE = Blocks.AIR.getDefaultState();
    private final Function<NBTTagCompound, IBlockState> deserializer;
    private final IBlockState defaultState;
    private final long[] longArray;
    private final int storageSize;
    private final BlockPos pos;
    private int bits;
    private BitArray storage;
    private ILitematicaBlockStatePalette palette;

    public LitematicaBlockStateContainer(BlockPos pos, NBTTagList paletteList, long[] longArray) {
        this.deserializer = in -> NBTUtil.readBlockState((NBTTagCompound) in);
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
            } else {
                this.bits = Math.max(this.bits, 2);
                this.palette = new LitematicaBlockStatePaletteLinear(this.bits, this, this.deserializer);
            }
            this.palette.idFor(AIR_DEFAULT_STATE);

            // Storage init
            if (this.longArray == null) {
                this.storage = new BitArray(this.bits, this.storageSize);
            } else {
                this.storage = new BitArray(this.bits, this.storageSize, longArray);
            }
        }
    }

    private void readChunkPalette(NBTTagList paletteNBT) {
        int i = Math.max(2, MathHelper.log2DeBruijn(paletteNBT.size()));
        if (i != this.bits) {
            this.setBits(i);
        }
        this.palette.readNBT(paletteNBT);
    }

    private int getIndex(int x, int y, int z) {
        return (y * pos.getX() * pos.getZ()) + z * pos.getX() + x;
    }

    public IBlockState get(int x, int y, int z) {
        int index = this.getIndex(x, y, z);
        IBlockState state = this.palette.get(this.storage.getAt(index));
        return (state == null ? this.defaultState : state);
    }

    public int onResize(int bits, IBlockState state) {
        BitArray bitarray = this.storage;
        ILitematicaBlockStatePalette iblockstatepalette = this.palette;
        this.setBits(bits);

        for (int i = 0; i < bitarray.size(); ++i) {
            IBlockState t = iblockstatepalette.get(bitarray.getAt(i));
            if (t != null) {
                this.set(i, t);
            }
        }
        return this.palette.idFor(state);
    }

    protected void set(int index, IBlockState state) {
        int i = this.palette.idFor(state);
        this.storage.setAt(index, i);
    }
}
