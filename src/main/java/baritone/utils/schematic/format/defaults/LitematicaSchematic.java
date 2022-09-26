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
 * @author rycbar
 * @since 22.09.2022
 *
 */
public final class LitematicaSchematic extends StaticSchematic {
    int minX=0,minY=0,minZ=0;
    private static final String reg = "Regions";
    private static final String meta = "Metadata";
    private static final String schemSize = "EnclosingSize";
    private static final String blSt = "BlockStates";
    private static final String blStPl = "BlockStatePalette";
    private static final String pos = "Position";
    private static final String size = "Size";
    private static String subReg;
    private static String[] regNames;
    private static NBTTagCompound nbt;

    public LitematicaSchematic(NBTTagCompound nbtCompound) {
        nbt = nbtCompound;
        regNames = getRegions();
        getMinimumCorner();

        this.x = Math.abs(nbt.getCompoundTag(meta).getCompoundTag(schemSize).getInteger("x"));
        this.y = Math.abs(nbt.getCompoundTag(meta).getCompoundTag(schemSize).getInteger("y"));
        this.z = Math.abs(nbt.getCompoundTag(meta).getCompoundTag(schemSize).getInteger("z"));
        this.states = new IBlockState[this.x][this.z][this.y];

        for (String subRegion : regNames) {
            subReg = subRegion;
            NBTTagList usedBlockTypes = nbt.getCompoundTag(reg).getCompoundTag(subReg).getTagList(blStPl, 10);
            IBlockState[] blockList = getBlockList(usedBlockTypes);

            int bitsPerBlock = getBitsPerBlock(usedBlockTypes.tagCount());
            long regionVolume = getVolume();
            long[] blockStateArray = getBlockStates();

            LitematicaBitArray bitArray = new LitematicaBitArray(bitsPerBlock, regionVolume, blockStateArray);

            writeSubregionIntoSchematic(blockList, bitArray);
        }
    }

    /**
     * @return Array of subregion names.
     */
    private static String[] getRegions() {
        return nbt.getCompoundTag(reg).getKeySet().toArray(new String[0]);
    }

    /**
     * Calculates the minimum cords/origin of the schematic as litematica schematics
     * can have a non-minimum origin.
     */
    private void getMinimumCorner() {
        for (String subRegion : regNames) {
            subReg = subRegion;
            this.minX = Math.min(this.minX, getMinimumCord("x"));
            this.minY = Math.min(this.minY, getMinimumCord("y"));
            this.minZ = Math.min(this.minZ, getMinimumCord("z"));
        }
    }

    /**
     * @param s axis that should be read.
     * @return the lower cord of the requested axis.
     */
    private static int getMinimumCord(String s) {
        int a = nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(pos).getInteger(s);
        int b = nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger(s);
        if (b < 0) {
            b++;
        }
        return Math.min(a,a+b);

    }

    /**
     * @param blockStatePalette List of all different block types used in the schematic.
     * @return Array of BlockStates.
     */
    private static IBlockState[] getBlockList(NBTTagList blockStatePalette) {
        IBlockState[] blockList = new IBlockState[blockStatePalette.tagCount()];

        for (int i = 0; i< blockStatePalette.tagCount(); i++) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation((((NBTTagCompound) blockStatePalette.get(i)).getString("Name"))));
            NBTTagCompound properties = ((NBTTagCompound) blockStatePalette.get(i)).getCompoundTag("Properties");

            blockList[i] = getBlockState(block, properties);
        }
        return blockList;
    }

    /**
     * @param block block.
     * @param properties List of Properties the block has.
     * @return A blockState.
     */
    private static IBlockState getBlockState(Block block, NBTTagCompound properties) {
        IBlockState blockState = block.getDefaultState();

        for (Object key : properties.getKeySet().toArray()) {
            IProperty<?> property = block.getBlockState().getProperty((String) key);
            String propertyValue = properties.getString((String) key);
            if (property != null) {
                blockState = setPropertyValue(blockState, property, propertyValue);
            }
        }
        return blockState;
    }

    /**
     * i haven't written this and i wont try to decode it.
     * @param state .
     * @param property .
     * @param value .
     * @return .
     * @param <T> .
     */
    private static <T extends Comparable<T>> IBlockState setPropertyValue(IBlockState state, IProperty<T> property, String value) {
        Optional<T> parsed = property.parseValue(value).toJavaUtil();
        if (parsed.isPresent()) {
            return state.withProperty(property, parsed.get());
        } else {
            throw new IllegalArgumentException("Invalid value for property " + property);
        }
    }

    /**
     * @param amountOfBlockTypes amount of block types in the schematic.
     * @return amount of bits used to encode a block.
     */
    private static int getBitsPerBlock(int amountOfBlockTypes) {
        return  (int) Math.floor((Math.log(amountOfBlockTypes)) / Math.log(2))+1;
    }

    /**
     * @return the volume of the subregion.
     */
    private static long getVolume() {
        return Math.abs(
                nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger("x") *
                        nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger("y") *
                        nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger("z"));
    }

    /**
     * @return array of Long values.
     */
    private static long[] getBlockStates() {
        String rawBlockString = Objects.requireNonNull((nbt.getCompoundTag(reg).getCompoundTag(subReg).getTag(blSt))).toString();
        rawBlockString = rawBlockString.substring(3,rawBlockString.length()-1);
        String[] rawBlockArrayString = rawBlockString.split(",");
        long[] rawBlockData = new long[rawBlockArrayString.length];
        for (int i = 0; i < rawBlockArrayString.length; i++) {
            rawBlockData[i] = Long.parseLong(rawBlockArrayString[i].substring(0,rawBlockArrayString[i].length()-1));
        }
        return rawBlockData;
    }

    /**
     * @param blockList list with the different block types used in the schematic
     * @param bitArray bit array that holds the placement pattern
     */
    private void writeSubregionIntoSchematic(IBlockState[] blockList, LitematicaBitArray bitArray) {
        int posX = getMinimumCord("x");
        int posY = getMinimumCord("y");
        int posZ = getMinimumCord("z");
        int index = 0;
        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    if (inSubregion(x, y, z)) {
                        this.states[x-(minX- posX)][z-(minZ- posZ)][y-(minY- posY)] = blockList[bitArray.getAt(index)];
                        index++;
                    }
                }
            }
        }
    }

    /**
     * @param x cord of the schematic.
     * @param y cord of the schematic.
     * @param z cord of the schematic.
     * @return if the current block is inside the subregion.
     */
    private static boolean inSubregion(int x, int y, int z) {
        return
                x < Math.abs(nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger("x")) &&
                y < Math.abs(nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger("y")) &&
                z < Math.abs(nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger("z"));
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