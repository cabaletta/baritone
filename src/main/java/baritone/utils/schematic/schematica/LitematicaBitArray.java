package baritone.utils.schematic.schematica;

import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import baritone.utils.schematic.schematica.MathUtils;

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
    private final long arraySize;

    public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn)
    {
        this(bitsPerEntryIn, arraySizeIn, null);
    }

    public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn, @Nullable long[] longArrayIn)
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
            this.longArray = new long[(int) (MathUtils.roundUp((long) arraySizeIn * (long) bitsPerEntryIn, 64L) / 64L)];
        }
    }

    public void setAt(long index, int value)
    {
        Validate.inclusiveBetween(0L, this.arraySize - 1L, (long) index);
        Validate.inclusiveBetween(0L, this.maxEntryValue, (long) value);
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64
        this.longArray[startArrIndex] = this.longArray[startArrIndex] & ~(this.maxEntryValue << startBitOffset) | ((long) value & this.maxEntryValue) << startBitOffset;

        if (startArrIndex != endArrIndex)
        {
            int endOffset = 64 - startBitOffset;
            int j1 = this.bitsPerEntry - endOffset;
            this.longArray[endArrIndex] = this.longArray[endArrIndex] >>> j1 << j1 | ((long) value & this.maxEntryValue) >> endOffset;
        }
    }

    public int getAt(long index)
    {
        Validate.inclusiveBetween(0L, this.arraySize - 1L, (long) index);
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

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

    public long[] getValueCounts()
    {
        long[] counts = new long[(int) this.maxEntryValue + 1];
        final long size = this.arraySize;

        for (long i = 0; i < size; ++i)
        {
            ++counts[this.getAt(i)];
        }

        return counts;
    }

    public long[] getBackingLongArray()
    {
        return this.longArray;
    }

    public long size()
    {
        return this.arraySize;
    }
}