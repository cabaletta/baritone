package baritone.utils.schematic.litematica;

import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import net.minecraft.util.math.MathHelper;

public class LitematicaBitArray
{
    /** The long array that is used to store the data for this BitArray. */
    private final long[] longArray;
    /** Number of bits a single entry takes up */
    private final int bitsPerEntry;
    /**
     * The maximum value for a single entry. This also works as a bitmask for a single entry.
     * For instance, if bitsPerEntry were 5, this value would be 31 (ie, {@code 0b00011111}).
     */
    private final long maxEntryValue;
    /** Number of entries in this array (<b>not</b> the length of the long array that internally backs this array) */
    private final int arraySize;

    public LitematicaBitArray(int bitsPerEntryIn, int arraySizeIn)
    {
        this(bitsPerEntryIn, arraySizeIn, null);
    }

    public LitematicaBitArray(int bitsPerEntryIn, int arraySizeIn, @Nullable long[] longArrayIn)
    {
        Validate.inclusiveBetween(1L, 32L, (long) bitsPerEntryIn);
        this.arraySize = arraySizeIn;
        this.bitsPerEntry = bitsPerEntryIn;
        this.maxEntryValue = (1L << bitsPerEntryIn) - 1L;

        if (longArrayIn != null)
        {
            this.longArray = longArrayIn;
        }
        else
        {
            this.longArray = new long[MathHelper.roundUp(arraySizeIn * bitsPerEntryIn, 64) / 64];
        }
    }

    public void setAt(int index, int value)
    {
        Validate.inclusiveBetween(0L, (long) (this.arraySize - 1), (long) index);
        Validate.inclusiveBetween(0L, this.maxEntryValue, (long) value);
        int startOffset = index * this.bitsPerEntry;
        int startArrIndex = startOffset >> 6; // startOffset / 64
        int endArrIndex = ((index + 1) * this.bitsPerEntry - 1) >> 6;
        int startBitOffset = startOffset & 0x3F; // startOffset % 64
        this.longArray[startArrIndex] = this.longArray[startArrIndex] & ~(this.maxEntryValue << startBitOffset) | ((long) value & this.maxEntryValue) << startBitOffset;

        if (startArrIndex != endArrIndex)
        {
            int endOffset = 64 - startBitOffset;
            int j1 = this.bitsPerEntry - endOffset;
            this.longArray[endArrIndex] = this.longArray[endArrIndex] >>> j1 << j1 | ((long) value & this.maxEntryValue) >> endOffset;
        }
    }

    public int getAt(int index)
    {
        Validate.inclusiveBetween(0L, (long) (this.arraySize - 1), (long) index);
        int startOffset = index * this.bitsPerEntry;
        int startArrIndex = startOffset >> 6; // startOffset / 64
        int endArrIndex = ((index + 1) * this.bitsPerEntry - 1) >> 6;
        int startBitOffset = startOffset & 0x3F; // startOffset % 64

        if (startArrIndex == endArrIndex)
        {
            return (int) (this.longArray[startArrIndex] >>> startBitOffset & this.maxEntryValue);
        }
        else
        {
            int endOffset = 64 - startBitOffset;
            return (int) ((this.longArray[startArrIndex] >>> startBitOffset | this.longArray[endArrIndex] << endOffset) & this.maxEntryValue);
        }
    }

    public long[] getBackingLongArray()
    {
        return this.longArray;
    }

    public int size()
    {
        return this.arraySize;
    }
}
