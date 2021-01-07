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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.state.Property;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Emerson
 * @since 12/27/2020
 */
public final class LitematicaSchematic extends StaticSchematic {

    public LitematicaSchematic(CompoundNBT nbt) {
        String regionName = (String) nbt.getCompound("Regions").keySet().toArray()[0];
        this.x = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("x"));
        this.y = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("y"));
        this.z = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("z"));
        this.states = new BlockState[this.x][this.z][this.y];


        ListNBT paletteTag = nbt.getCompound("Regions").getCompound(regionName).getList("BlockStatePalette",10);
        if (paletteTag == null) {
            System.out.println("Failed to get palette");
        }

        BlockState[] paletteBlockStates = new BlockState[paletteTag.size()];
        for (int i = 0; i<paletteTag.size(); i++) {
            Block block = Registry.BLOCK.getOrDefault(new ResourceLocation((((CompoundNBT) paletteTag.get(i)).getString("Name"))));
            BlockState blockState = block.getDefaultState();
            CompoundNBT properties = ((CompoundNBT) paletteTag.get(i)).getCompound("Properties");
            Object[] keys = properties.keySet().toArray();
            Map<String, String> propertiesMap = new HashMap<>();
            for (int j = 0; j<keys.length; j++) {
                propertiesMap.put((String) keys[j], (properties.get((String) keys[j])).toString());
            }
            for (int j = 0; j<keys.length; j++) {
                Property<?> property = block.getStateContainer().getProperty(keys[j].toString());
                if (property != null) {
                    //Thank you litematica for putting quotes around the key values
                    blockState = setPropertyValue(blockState, property, propertiesMap.get(keys[j]).substring(1,(propertiesMap.get(keys[j])).length()-1));
                }
            }
            paletteBlockStates[i] = blockState;
        }


        // BlockData is stored as an NBT long[]
        int paletteSize = (int) Math.floor(log2(paletteTag.size()))+1;
        long litematicSize = (long) this.x*this.y*this.z;

        long[] rawBlockData = nbt.getCompound("Regions").getCompound(regionName).getLongArray("BlockStates");

        LitematicaBitArray bitArray = new LitematicaBitArray(paletteSize,litematicSize,rawBlockData);
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
                    BlockState state = paletteBlockStates[serializedBlockStates[counter]];
                    this.states[x][z][y] = state;
                    counter++;
                }
            }
        }
    }
    private static double log2(int N) {
        return (Math.log(N) / Math.log(2));
    }

    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.parseValue(value);
        if (parsed.isPresent()) {
            return state.with(property, parsed.get());
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
