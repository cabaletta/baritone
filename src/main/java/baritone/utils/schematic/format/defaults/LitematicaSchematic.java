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

import baritone.utils.accessor.INBTTagLongArray;
import baritone.utils.schematic.StaticSchematic;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Based on EmersonDove's work
 * <a href="https://github.com/cabaletta/baritone/pull/2544">...</a>
 *
 * @author rycbar
 * @since 22.09.2022
 */
public final class LitematicaSchematic extends StaticSchematic {
    private final Vec3i offsetMinCorner;
    private final NBTTagCompound nbt;

    /**
     * @param nbtTagCompound a decompressed file stream aka nbt data.
     * @param rotated        if the schematic is rotated by 90°.
     */
    public LitematicaSchematic(NBTTagCompound nbtTagCompound, boolean rotated) {
        this.nbt = nbtTagCompound;
        this.offsetMinCorner = new Vec3i(getMinOfSchematic("x"), getMinOfSchematic("y"), getMinOfSchematic("z"));
        this.y = Math.abs(nbt.getCompoundTag("Metadata").getCompoundTag("EnclosingSize").getInteger("y"));

        if (rotated) {
            this.x = Math.abs(nbt.getCompoundTag("Metadata").getCompoundTag("EnclosingSize").getInteger("z"));
            this.z = Math.abs(nbt.getCompoundTag("Metadata").getCompoundTag("EnclosingSize").getInteger("x"));
        } else {
            this.x = Math.abs(nbt.getCompoundTag("Metadata").getCompoundTag("EnclosingSize").getInteger("x"));
            this.z = Math.abs(nbt.getCompoundTag("Metadata").getCompoundTag("EnclosingSize").getInteger("z"));
        }
        this.states = new IBlockState[this.x][this.z][this.y];
        fillInSchematic();
    }

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentBlockState) {
        return x >= 0 && x < widthX() && y >= 0 && y < heightY() && z >= 0 && z < lengthZ() && states[x][y][z] != null;
    }

    /**
     * @return Array of subregion names.
     */
    private static String[] getRegions(NBTTagCompound nbt) {
        return nbt.getCompoundTag("Regions").getKeySet().toArray(new String[0]);
    }

    /**
     * Gets both ends from a region box for a given axis and returns the lower one.
     *
     * @param s axis that should be read.
     * @return the lower coord of the requested axis.
     */
    private static int getMinOfSubregion(NBTTagCompound nbt, String subReg, String s) {
        int a = nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Position").getInteger(s);
        int b = nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger(s);
        if (b < 0) {
            b++;
        }
        return Math.min(a, a + b);

    }

    /**
     * @param blockStatePalette List of all different block types used in the schematic.
     * @return Array of BlockStates.
     */
    private static IBlockState[] getBlockList(NBTTagList blockStatePalette) {
        IBlockState[] blockList = new IBlockState[blockStatePalette.tagCount()];

        for (int i = 0; i < blockStatePalette.tagCount(); i++) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation((((NBTTagCompound) blockStatePalette.get(i)).getString("Name"))));
            NBTTagCompound properties = ((NBTTagCompound) blockStatePalette.get(i)).getCompoundTag("Properties");

            blockList[i] = getBlockState(block, properties);
        }
        return blockList;
    }

    /**
     * @param block      block.
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
     * @author Emerson
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
        return (int) Math.max(2, Math.ceil(Math.log(amountOfBlockTypes) / Math.log(2)));
    }

    /**
     * Calculates the volume of the subregion. As size can be a negative value we take the absolute value of the
     * multiplication as the volume still holds a positive amount of blocks.
     *
     * @return the volume of the subregion.
     */
    private static long getVolume(NBTTagCompound nbt, String subReg) {
        return Math.abs(
                nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger("x") *
                        nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger("y") *
                        nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger("z"));
    }

    /**
     * @return array of Long values.
     */
    private static long[] getBlockStates(NBTTagCompound nbt, String subReg) {
        return ((INBTTagLongArray) nbt.getCompoundTag("Regions").getCompoundTag(subReg).getTag("BlockStates")).getLongArray();
    }

    /**
     * Subregion don't have to be the same size as the enclosing size of the schematic. If they are smaller we check here if the current block is part of the subregion.
     *
     * @param x coord of the block relative to the minimum corner.
     * @param y coord of the block relative to the minimum corner.
     * @param z coord of the block relative to the minimum corner.
     * @return if the current block is part of the subregion.
     */
    private static boolean inSubregion(NBTTagCompound nbt, String subReg, int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 &&
                x < Math.abs(nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger("x")) &&
                y < Math.abs(nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger("y")) &&
                z < Math.abs(nbt.getCompoundTag("Regions").getCompoundTag(subReg).getCompoundTag("Size").getInteger("z"));
    }

    /**
     * @param s axis.
     * @return the lowest coordinate of that axis of the schematic.
     */
    private int getMinOfSchematic(String s) {
        int n = Integer.MAX_VALUE;
        for (String subReg : getRegions(nbt)) {
            n = Math.min(n, getMinOfSubregion(nbt, subReg, s));
        }
        return n;
    }

    /**
     * reads the file data.
     */
    private void fillInSchematic() {
        for (String subReg : getRegions(nbt)) {
            NBTTagList usedBlockTypes = nbt.getCompoundTag("Regions").getCompoundTag(subReg).getTagList("BlockStatePalette", 10);
            IBlockState[] blockList = getBlockList(usedBlockTypes);

            int bitsPerBlock = getBitsPerBlock(usedBlockTypes.tagCount());
            long regionVolume = getVolume(nbt, subReg);
            long[] blockStateArray = getBlockStates(nbt, subReg);

            LitematicaBitArray bitArray = new LitematicaBitArray(bitsPerBlock, regionVolume, blockStateArray);

            writeSubregionIntoSchematic(nbt, subReg, blockList, bitArray);
        }
    }

    /**
     * Writes the file data in to the IBlockstate array.
     *
     * @param blockList list with the different block types used in the schematic.
     * @param bitArray  bit array that holds the placement pattern.
     */
    private void writeSubregionIntoSchematic(NBTTagCompound nbt, String subReg, IBlockState[] blockList, LitematicaBitArray bitArray) {
        Vec3i offsetSubregion = new Vec3i(getMinOfSubregion(nbt, subReg, "x"), getMinOfSubregion(nbt, subReg, "y"), getMinOfSubregion(nbt, subReg, "z"));
        int index = 0;
        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    if (inSubregion(nbt, subReg, x, y, z)) {
                        this.states[x - (offsetMinCorner.getX() - offsetSubregion.getX())][z - (offsetMinCorner.getZ() - offsetSubregion.getZ())][y - (offsetMinCorner.getY() - offsetSubregion.getY())] = blockList[bitArray.getAt(index)];
                        index++;
                    }
                }
            }
        }
    }

    /**
     * @return offset from the schematic origin to the minimum Corner as a Vec3i.
     */
    public Vec3i getOffsetMinCorner() {
        return offsetMinCorner;
    }

    /**
     * @param x          position relative to the minimum corner of the schematic.
     * @param y          position relative to the minimum corner of the schematic.
     * @param z          position relative to the minimum corner of the schematic.
     * @param blockState new blockstate of the block at this position.
     */
    public void setDirect(int x, int y, int z, IBlockState blockState) {
        this.states[x][z][y] = blockState;
    }

    /**
     * @param rotated if the schematic is rotated by 90°.
     * @return a copy of the schematic.
     */
    public LitematicaSchematic getCopy(boolean rotated) {
        return new LitematicaSchematic(nbt, rotated);
    }

    /**
     * @author maruohon
     * Class from the Litematica mod by maruohon
     * Usage under LGPLv3 with the permission of the author.
     * <a href="https://github.com/maruohon/litematica">...</a>
     */
    private static class LitematicaBitArray {
        /**
         * The long array that is used to store the data for this BitArray.
         */
        private final long[] longArray;
        /**
         * Number of bits a single entry takes up
         */
        private final int bitsPerEntry;
        /**
         * The maximum value for a single entry. This also works as a bitmask for a single entry.
         * For instance, if bitsPerEntry were 5, this value would be 31 (ie, {@code 0b00011111}).
         */
        private final long maxEntryValue;
        /**
         * Number of entries in this array (<b>not</b> the length of the long array that internally backs this array)
         */
        private final long arraySize;

        public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn, @Nullable long[] longArrayIn) {
            Validate.inclusiveBetween(1L, 32L, bitsPerEntryIn);
            this.arraySize = arraySizeIn;
            this.bitsPerEntry = bitsPerEntryIn;
            this.maxEntryValue = (1L << bitsPerEntryIn) - 1L;

            if (longArrayIn != null) {
                this.longArray = longArrayIn;
            } else {
                this.longArray = new long[(int) (roundUp(arraySizeIn * (long) bitsPerEntryIn, 64L) / 64L)];
            }
        }

        public static long roundUp(long number, long interval) {
            int sign = 1;
            if (interval == 0) {
                return 0;
            } else if (number == 0) {
                return interval;
            } else {
                if (number < 0) {
                    sign = -1;
                }

                long i = number % (interval * sign);
                return i == 0 ? number : number + (interval * sign) - i;
            }
        }

        public int getAt(long index) {
            Validate.inclusiveBetween(0L, this.arraySize - 1L, index);
            long startOffset = index * (long) this.bitsPerEntry;
            int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
            int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
            int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

            if (startArrIndex == endArrIndex) {
                return (int) (this.longArray[startArrIndex] >>> startBitOffset & this.maxEntryValue);
            } else {
                int endOffset = 64 - startBitOffset;
                return (int) ((this.longArray[startArrIndex] >>> startBitOffset | this.longArray[endArrIndex] << endOffset) & this.maxEntryValue);
            }
        }

        public long size() {
            return this.arraySize;
        }
    }
}