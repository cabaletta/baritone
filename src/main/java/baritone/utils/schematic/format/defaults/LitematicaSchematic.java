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

    public LitematicaSchematic(NBTTagCompound nbt) {
        this.nbt = nbt;
        regNames = getRegions();
        minCord();

        this.x = Math.abs(nbt.getCompoundTag(meta).getCompoundTag(schemSize).getInteger("x"));
        this.y = Math.abs(nbt.getCompoundTag(meta).getCompoundTag(schemSize).getInteger("y"));
        this.z = Math.abs(nbt.getCompoundTag(meta).getCompoundTag(schemSize).getInteger("z"));
        this.states = new IBlockState[this.x][this.z][this.y];

        for (String subRegion : regNames) {
            subReg = subRegion;
            NBTTagList blockStatePalette = nbt.getCompoundTag(reg).getCompoundTag(subReg).getTagList(blStPl, 10);
            // ListTag blockStatePalette = nbt.getCompound(reg).getCompound(subReg).getList(blStPl,10);
            IBlockState[] paletteBlockStates = paletteBlockStates(blockStatePalette);
            // BlockState[] paletteBlockStates = paletteBlockStates(blockStatePalette);

            int bitsPerBlock = bitsPerBlock(blockStatePalette.tagCount());
            long regionVolume = getVolume();
            long[] blockStateArray = getBlockStates();

            LitematicaBitArray bitArray = new LitematicaBitArray(bitsPerBlock, regionVolume, blockStateArray);

            if (bitsPerBlock > 32) {
                throw new IllegalStateException("Too many blocks in schematic to handle");
            }

            createSchematicOfSubRegion(paletteBlockStates, bitArray);
        }
    }

    /**
     * @param paletteBlockStates list with the different block types used in the schematic
     * @param bitArray bit array that holds the placement pattern
     */
    private void createSchematicOfSubRegion(IBlockState[] paletteBlockStates, LitematicaBitArray bitArray) {
        int posX = getMin("x");
        int posY = getMin("y");
        int posZ = getMin("z");
        int index = 0;
        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    if (inSubregion(x, y, z)) {
                        this.states[x-(minX- posX)][z-(minZ- posZ)][y-(minY- posY)] = paletteBlockStates[bitArray.getAt(index)];
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

    /**
     * @param s axis that should be read.
     * @return the lower cord of the requested axis.
     */
    private static int getMin(String s) {
        int a = nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(pos).getInteger(s);
        int b = nbt.getCompoundTag(reg).getCompoundTag(subReg).getCompoundTag(size).getInteger(s);
        if (b < 0) {
            b++;
        }
        return Math.min(a,a+b);

    }

    /**
     * Calculates the minimum cords/origin of the schematic as litematica schematics
     * can have a non minimum origin.
     */
    private void minCord() {
        for (String subRegion : regNames) {
            subReg = subRegion;
            this.minX = Math.min(this.minX,getMin("x"));
            this.minY = Math.min(this.minY,getMin("y"));
            this.minZ = Math.min(this.minZ,getMin("z"));
        }
    }

    /**
     * @return Array of subregion names.
     */
    private static String[] getRegions() {
        return nbt.getCompoundTag(reg).getKeySet().toArray(new String[0]);
    }

    /**
     * @param blockStatePalette List of all different block types used in the schematic.
     * @return Array of BlockStates.
     */
    private static IBlockState[] paletteBlockStates(NBTTagList blockStatePalette) {
        // private static BlockState[] paletteBlockStates(TagList blockStatePalette) {
        IBlockState[] paletteBlockStates = new IBlockState[blockStatePalette.tagCount()];
        //BlockState[] paletteBlockState = new BlockState[blockStatePalette.tagCount()];

        for (int i = 0; i< blockStatePalette.tagCount(); i++) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation((((NBTTagCompound) blockStatePalette.get(i)).getString("Name"))));
            //Block block = Registry.BLOCK.get(new ResourceLocation((((CompoundTag) blockStatePalette.get(i)).getString("Name"))));
            NBTTagCompound properties = ((NBTTagCompound) blockStatePalette.get(i)).getCompoundTag("Properties");
            //CompoundTag properties = ((CompoundTag) blockStatePalette.get(i)).getCompound("Properties");

            paletteBlockStates[i] = getBlockState(block, properties);
        }
        return paletteBlockStates;
    }

    /**
     * @param block block.
     * @param properties List of Properties the block has.
     * @return A blockState.
     */
    private static IBlockState getBlockState(Block block, NBTTagCompound properties) {
        //private static BlockState getBlockState(Block block, CompoundTag properties) {
        IBlockState blockState = block.getDefaultState();
        //BlockState blockState = block.defaultBlockState();

        for (Object key : properties.getKeySet().toArray()) {
            //for (Object key : properties.getAllKeys().toArray()) {
            IProperty<?> property = block.getBlockState().getProperty(key.toString());
            //Property<?> property = block.getStateDefinition().getProperty(key.toString());
            if (property != null) {
                blockState = setPropertyValue(blockState, property, propertiesMap(properties).get(key));
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
        //private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.parseValue(value).toJavaUtil();
        //Optional<T> parsed = property.getValue(value);
        if (parsed.isPresent()) {
            return state.withProperty(property, parsed.get());
            //return state.setValue(property, parsed.get());
        } else {
            throw new IllegalArgumentException("Invalid value for property " + property);
        }
    }

    /**
     * @param properties properties a block has.
     * @return properties as map.
     */
    private static Map<String, String> propertiesMap(NBTTagCompound properties) {
        //private static Map<String, String> propertiesMap(CompoundTag properties) {
        Map<String, String> propertiesMap = new HashMap<>();

        for (Object key : properties.getKeySet().toArray()) {
            //for (Object key : properties.getAllKeys().toArray()) {
            propertiesMap.put((String) key, (properties.getString((String) key)));
        }
        return propertiesMap;
    }

    /**
     * @param amountOfBlockTypes amount of block types in the schematic.
     * @return amount of bits used to encode a block.
     */
    private static int bitsPerBlock(int amountOfBlockTypes) {
        //private static int bitsPerBlock(ListTag blockStatePalette) {
        return  (int) Math.floor((Math.log(amountOfBlockTypes)) / Math.log(2))+1;
        //return (int) Math.floor((Math.log(blockStatePalette.size())) / Math.log(2))+1;
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

    // will only work after 1.12. will replace the getBlockStates() above.
    /*
    /**
     * @return array of Long values.
     */
    /*
    private static long[] getBlockStates() {
        return nbt.getCompoundTag(reg).getCompoundTag(subReg).getTag(blSt).getLongArray();
    }*/

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