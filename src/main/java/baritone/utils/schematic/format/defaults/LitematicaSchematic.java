/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils.schematic.format.defaults;

import baritone.utils.schematic.StaticSchematic;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.state.IBlockState;

import org.apache.commons.lang3.Validate;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Emerson
 * @since 12/27/2020
 */
public final class LitematicaSchematic extends StaticSchematic {

    public LitematicaSchematic(NBTTagCompound nbt) {
        String regionName = (String) nbt.getCompoundTag("Regions").getKeySet().toArray()[0];
        this.x = Math.abs(nbt.getCompoundTag("Regions").getCompoundTag(regionName).getCompoundTag("Size").getInteger("x"));
        this.y = Math.abs(nbt.getCompoundTag("Regions").getCompoundTag(regionName).getCompoundTag("Size").getInteger("y"));
        this.z = Math.abs(nbt.getCompoundTag("Regions").getCompoundTag(regionName).getCompoundTag("Size").getInteger("z"));
        this.states = new IBlockState[this.x][this.z][this.y];


        NBTTagList paletteTag = nbt.getCompoundTag("Regions").getCompoundTag(regionName).getTagList("BlockStatePalette",10);
        // ListNBT paletteTag = nbt.getCompound("Regions").getCompound(regionName).getList("BlockStatePalette",10);

        // Create the block states array
        IBlockState[] paletteBlockStates = new IBlockState[paletteTag.tagCount()];
        // For every part of the array
        for (int i = 0; i<paletteTag.tagCount(); i++) {
            // Set the default state by getting block name
            Block block = Block.REGISTRY.getObject(new ResourceLocation((((NBTTagCompound) paletteTag.get(i)).getString("Name"))));
            IBlockState blockState = block.getDefaultState();
            NBTTagCompound properties = ((NBTTagCompound) paletteTag.get(i)).getCompoundTag("Properties");
            Object[] keys = properties.getKeySet().toArray();
            Map<String, String> propertiesMap = new HashMap<>();
            // Create a map for each state
            for (int j = 0; j<keys.length; j++) {
                propertiesMap.put((String) keys[j], (properties.getString((String) keys[j])));
            }
            for (int j = 0; j<keys.length; j++) {
                IProperty<?> property = block.getBlockState().getProperty(keys[j].toString());
                if (property != null) {
                    blockState = setPropertyValue(blockState, property, propertiesMap.get(keys[j]));
                }
            }
            paletteBlockStates[i] = blockState;
        }


        // BlockData is stored as an NBT long[]
        int paletteSize = (int) Math.floor(log2(paletteTag.tagCount()))+1;
        long litematicSize = (long) this.x*this.y*this.z;

        // In 1.12, the long array isn't exposed by the libraries so parsing has to be done manually
        String rawBlockString = (nbt.getCompoundTag("Regions").getCompoundTag(regionName)).getTag("BlockStates").toString();
        rawBlockString = rawBlockString.substring(3,rawBlockString.length()-1);
        String[] rawBlockArrayString = rawBlockString.split(",");
        long[] rawBlockData = new long[rawBlockArrayString.length];
        for (int i = 0; i < rawBlockArrayString.length; i++) {
            rawBlockData[i] = Long.parseLong(rawBlockArrayString[i].substring(0,rawBlockArrayString[i].length()-1));
        }


        LitematicaBitArray bitArray = new LitematicaBitArray(paletteSize, litematicSize, rawBlockData);
        if (paletteSize > 32) {
            throw new IllegalStateException("Too many blocks in schematic to handle");
        }

        int[] serializedBlockStates = new int[(int) litematicSize];
        for (int i = 0; i<serializedBlockStates.length; i++) {
            serializedBlockStates[i] = bitArray.getAt(i);
        }

        int counter = 0;
        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    IBlockState state = paletteBlockStates[serializedBlockStates[counter]];
                    this.states[x][z][y] = state;
                    counter++;
                }
            }
        }
    }
    private static double log2(int N) {
        return (Math.log(N) / Math.log(2));
    }

    private static <T extends Comparable<T>> IBlockState setPropertyValue(IBlockState state, IProperty<T> property, String value) {
        Optional<T> parsed = property.parseValue(value).toJavaUtil();
        if (parsed.isPresent()) {
            return state.withProperty(property, parsed.get());
        } else {
            throw new IllegalArgumentException("Invalid value for property " + property);
        }
    }

    /** LitematicaBitArray class from litematica */
    private static class LitematicaBitArray
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
                this.longArray = new long[(int) (roundUp((long) arraySizeIn * (long) bitsPerEntryIn, 64L) / 64L)];
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


        public long size()
        {
            return this.arraySize;
        }

        public static long roundUp(long number, long interval)
        {
            if (interval == 0)
            {
                return 0;
            }
            else if (number == 0)
            {
                return interval;
            }
            else
            {
                if (number < 0)
                {
                    interval *= -1;
                }

                long i = number % interval;
                return i == 0 ? number : number + interval - i;
            }
        }
    }
}
