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
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.Validate;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Emerson
 * @since 12/27/2020
 * @author rycbar
 * @since 22.09.2022
 *
 * Original version for 1.12 by Emerson. Edit for 1.18 by rycbar.
 *
 */
public final class LitematicaSchematic extends StaticSchematic {

    public LitematicaSchematic(CompoundTag nbt) {
        String regionName = (String) nbt.getCompound("Regions").getAllKeys().toArray()[0];
        this.x = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("x"));
        this.y = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("y"));
        this.z = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("z"));
        this.states = new BlockState[this.x][this.z][this.y];
        ListTag blockStatePalette = nbt.getCompound("Regions").getCompound(regionName).getList("BlockStatePalette",10);
        BlockState[] paletteBlockStates = paletteBlockStates(blockStatePalette);

        int bitsPerBlock = bitsPerBlock(blockStatePalette);
        long schematicVolume = schematicVolume();
        long[] rawBlockData = rawBlockData(rawBlockArrayString(nbt, regionName));

        LitematicaBitArray bitArray = new LitematicaBitArray(bitsPerBlock, schematicVolume, rawBlockData);

        if (bitsPerBlock > 32) {
            throw new IllegalStateException("Too many blocks in schematic to handle");
        }

        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    this.states[x][y][z] = paletteBlockStates[bitArray.getAt((y * this.z + z) * this.x + x)];
                }
            }
        }
    }

    /**
     * @param blockStatePalette List of all different block types used in the schematic.
     * @return Array of BlockStates.
     */
    private BlockState[] paletteBlockStates(ListTag blockStatePalette) {
        BlockState[] paletteBlockStates = new BlockState[blockStatePalette.size()];

        for (int i = 0; i< blockStatePalette.size(); i++) {
            Block block = Registry.BLOCK.get(new ResourceLocation((((CompoundTag) blockStatePalette.get(i)).getString("Name"))));
            CompoundTag properties = ((CompoundTag) blockStatePalette.get(i)).getCompound("Properties");

            paletteBlockStates[i] = getBlockState(block, properties);
        }
        return paletteBlockStates;
    }

    /**
     * @param block block.
     * @param properties List of Properties the block has.
     * @return A blockState.
     */
    private BlockState getBlockState(Block block, CompoundTag properties) {
        BlockState blockState = block.defaultBlockState();

        for (Object key : properties.getAllKeys().toArray()) {
            Property<?> property = block.getStateDefinition().getProperty(key.toString());
            if (property != null) {
                blockState = setPropertyValue(blockState, property, propertiesMap(properties).get(key));
            }
        }
        return blockState;
    }

    /**
     * i haven't written this and i wont try to decode it.
     * @param state
     * @param property
     * @param value
     * @return
     * @param <T>
     */
    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        if (parsed.isPresent()) {
            return state.setValue(property, parsed.get());
        } else {
            throw new IllegalArgumentException("Invalid value for property " + property);
        }
    }

    /**
     * @param properties properties a block has.
     * @return properties as map.
     */
    private static Map<String, String> propertiesMap(CompoundTag properties) {
        Map<String, String> propertiesMap = new HashMap<>();

        for (Object key : properties.getAllKeys().toArray()) {
            propertiesMap.put((String) key, (properties.getString((String) key)));
        }
        return propertiesMap;
    }

    /**
     * @param blockStatePalette List of all different block types used in the schematic.
     * @return amount of bits used to encode a block.
     */
    private static int bitsPerBlock(ListTag blockStatePalette) {
        return (int) Math.floor((Math.log(blockStatePalette.size())) / Math.log(2))+1;
    }

    /**
     * @return the amount of blocks in the schematic, including air blocks.
     */
    private long schematicVolume() {
        return (long) this.x*this.y*this.z;
    }

    /**
     * @param rawBlockArrayString String Array holding Long values as text.
     * @return array of Long values.
     */
    private static long[] rawBlockData(String[] rawBlockArrayString) {
        long[] rawBlockData = new long[rawBlockArrayString.length];
        for (int i = 0; i < rawBlockArrayString.length; i++) {
            rawBlockData[i] = Long.parseLong(rawBlockArrayString[i].substring(0,rawBlockArrayString[i].length()-1));
        }
        return rawBlockData;
    }

    /**
     * @param nbt schematic file.
     * @param regionName Name of the region the schematic is in.
     * @return String Array holding Long values as text.
     */
    private static String[] rawBlockArrayString(CompoundTag nbt, String regionName) {
        String rawBlockString = Objects.requireNonNull((nbt.getCompound("Regions").getCompound(regionName).get("BlockStates"))).toString();
        rawBlockString = rawBlockString.substring(3,rawBlockString.length()-1);
        return rawBlockString.split(",");
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