package baritone.utils.schematic.litematica;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class LitematicaBlockStateContainer implements ILitematicaBlockStatePaletteResizer
{
    public static final BlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    protected LitematicaBitArray storage;
    protected ILitematicaBlockStatePalette palette;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final int sizeLayer;
    protected int bits;

    public LitematicaBlockStateContainer(int sizeX, int sizeY, int sizeZ)
    {
        this(sizeX, sizeY, sizeZ, 2, null);
    }

    private LitematicaBlockStateContainer(int sizeX, int sizeY, int sizeZ, int bits, long[] backingLongArray)
    {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeLayer = sizeX * sizeZ;

        this.setBits(bits, backingLongArray);
    }

    public Vec3i getSize()
    {
        return new Vec3i(this.sizeX, this.sizeY, this.sizeZ);
    }

    public BlockState get(int x, int y, int z)
    {
        BlockState state = this.palette.getBlockState(this.storage.getAt(this.getIndex(x, y, z)));
        return state == null ? AIR_BLOCK_STATE : state;
    }

    public void set(int x, int y, int z, BlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(this.getIndex(x, y, z), id);
    }

    protected void set(int index, BlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(index, id);
    }

    protected int getIndex(int x, int y, int z)
    {
        return (y * this.sizeLayer) + z * this.sizeX + x;
    }

    protected void setBits(int bitsIn, long[] backingLongArray)
    {
        if (bitsIn != this.bits)
        {
            this.bits = bitsIn;

            if (this.bits <= 4)
            {
                this.bits = Math.max(2, this.bits);
                this.palette = new LitematicaBlockStatePaletteLinear(this.bits, this);
            }
            else
            {
                this.palette = new LitematicaBlockStatePaletteHashMap(this.bits, this);
            }

            this.palette.idFor(AIR_BLOCK_STATE);

            if (backingLongArray != null)
            {
                this.storage = new LitematicaBitArray(this.bits, this.sizeX * this.sizeY * this.sizeZ, backingLongArray);
            }
            else
            {
                this.storage = new LitematicaBitArray(this.bits, this.sizeX * this.sizeY * this.sizeZ);
            }
        }
    }

    @Override
    public int onResize(int bits, BlockState state)
    {
        LitematicaBitArray bitArray = this.storage;
        ILitematicaBlockStatePalette statePaletteOld = this.palette;
        this.setBits(bits, null);

        for (int id = 0; id < bitArray.size(); ++id)
        {
            BlockState stateTmp = statePaletteOld.getBlockState(bitArray.getAt(id));

            if (stateTmp != null)
            {
                this.set(id, stateTmp);
            }
        }

        return this.palette.idFor(state);
    }

    public long[] getBackingLongArray()
    {
        return this.storage.getBackingLongArray();
    }

    public ILitematicaBlockStatePalette getPalette()
    {
        return this.palette;
    }

    public static LitematicaBlockStateContainer createFrom(ListNBT palette, long[] blockStates, BlockPos size)
    {
        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ(), bits, blockStates);
        container.palette.readFromNBT(palette);
        return container;
    }
}
