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
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
    private final CompoundTag nbt;

    /**
     * @param nbtTagCompound a decompressed file stream aka nbt data.
     * @param rotated        if the schematic is rotated by 90°.
     */
    public LitematicaSchematic(CompoundTag nbtTagCompound, boolean rotated) {
        this.nbt = nbtTagCompound;
        this.offsetMinCorner = new Vec3i(getMinOfSchematic("x"), getMinOfSchematic("y"), getMinOfSchematic("z"));
        this.y = Math.abs(nbt.getCompound("Metadata").getCompound("EnclosingSize").getInt("y"));

        if (rotated) {
            System.out.println("is rotated");
            this.x = Math.abs(nbt.getCompound("Metadata").getCompound("EnclosingSize").getInt("z"));
            this.z = Math.abs(nbt.getCompound("Metadata").getCompound("EnclosingSize").getInt("x"));
        } else {
            this.x = Math.abs(nbt.getCompound("Metadata").getCompound("EnclosingSize").getInt("x"));
            this.z = Math.abs(nbt.getCompound("Metadata").getCompound("EnclosingSize").getInt("z"));
        }
        this.states = new BlockState[this.x][this.z][this.y];
        fillInSchematic();
    }

    /**
     * @return Array of subregion names.
     */
    private static String[] getRegions(CompoundTag nbt) {
        return nbt.getCompound("Regions").getAllKeys().toArray(new String[0]);
    }

    /**
     * Gets both ends from a region box for a given axis and returns the lower one.
     *
     * @param s axis that should be read.
     * @return the lower coord of the requested axis.
     */
    private static int getMinOfSubregion(CompoundTag nbt, String subReg, String s) {
        int a = nbt.getCompound("Regions").getCompound(subReg).getCompound("Position").getInt(s);
        int b = nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt(s);
        if (b < 0) {
            b++;
        }
        return Math.min(a, a + b);

    }

    /**
     * @param blockStatePalette List of all different block types used in the schematic.
     * @return Array of BlockStates.
     */
    private static BlockState[] getBlockList(ListTag blockStatePalette) {
        BlockState[] blockList = new BlockState[blockStatePalette.size()];

        for (int i = 0; i < blockStatePalette.size(); i++) {
            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation((((CompoundTag) blockStatePalette.get(i)).getString("Name"))));
            CompoundTag properties = ((CompoundTag) blockStatePalette.get(i)).getCompound("Properties");

            blockList[i] = getBlockState(block, properties);
        }
        return blockList;
    }

    /**
     * @param block      block.
     * @param properties List of Properties the block has.
     * @return A blockState.
     */
    private static BlockState getBlockState(Block block, CompoundTag properties) {
        BlockState blockState = block.defaultBlockState();

        for (Object key : properties.getAllKeys().toArray()) {
            Property<?> property = block.getStateDefinition().getProperty((String) key);
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
    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        if (parsed.isPresent()) {
            return state.setValue(property, parsed.get());
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
    private static long getVolume(CompoundTag nbt, String subReg) {
        return Math.abs(
                nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt("x") *
                        nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt("y") *
                        nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt("z"));
    }

    /**
     * @return array of Long values.
     */
    private static long[] getBlockStates(CompoundTag nbt, String subReg) {
        return nbt.getCompound("Regions").getCompound(subReg).getLongArray("BlockStates");
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        for (String subReg : getRegions(nbt)) {
            if (inSubregion(nbt, subReg, x, y, z)) {
                System.out.println("found " + x + "," + y + "," + z + " in subregion: " + subReg + " state: " + currentState);
                return true;
            }
        }
        System.out.println("not found block: " + x + "," + y + "," + z + " state: " + currentState);
        return false;
    }

    /**
     * Subregion don't have to be the same size as the enclosing size of the schematic. If they are smaller we check here if the current block is part of the subregion.
     *
     * @param x coord of the block relative to the minimum corner.
     * @param y coord of the block relative to the minimum corner.
     * @param z coord of the block relative to the minimum corner.
     * @return if the current block is part of the subregion.
     */
    private static boolean inSubregion(CompoundTag nbt, String subReg, int x, int y, int z) {
        CompoundTag region = nbt.getCompound("Regions").getCompound(subReg);
        int sizeX = region.getCompound("Size").getInt("x");
        int sizeY = region.getCompound("Size").getInt("y");
        int sizeZ = region.getCompound("Size").getInt("z");
        int posX = region.getCompound("Position").getInt("x");
        int posY = region.getCompound("Position").getInt("y");
        int posZ = region.getCompound("Position").getInt("z");

        boolean weThink = x >= Math.min(posX, posX + sizeX) && x < Math.max(posX, posX + sizeX) &&
                y >= Math.min(posY, posY + sizeY) && y < Math.max(posY, posY + sizeY) &&
                z >= Math.min(posZ, posZ + sizeZ) && z < Math.max(posZ, posZ + sizeZ);

        System.out.println("weThink: " + weThink + " x: " + x + " y: " + y + " z: " + z + " subReg: " + subReg);

        return x >= 0 && y >= 0 && z >= 0 &&
                x < Math.abs(nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt("x")) &&
                y < Math.abs(nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt("y")) &&
                z < Math.abs(nbt.getCompound("Regions").getCompound(subReg).getCompound("Size").getInt("z"));
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
            System.out.println("fillInSchematic for subregion: " + subReg);
            ListTag usedBlockTypes = nbt.getCompound("Regions").getCompound(subReg).getList("BlockStatePalette", 10);
            BlockState[] blockList = getBlockList(usedBlockTypes);

            int bitsPerBlock = getBitsPerBlock(usedBlockTypes.size());
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
    private void writeSubregionIntoSchematic(CompoundTag nbt, String subReg, BlockState[] blockList, LitematicaBitArray bitArray) {
        Vec3i offsetSubregion = new Vec3i(getMinOfSubregion(nbt, subReg, "x"), getMinOfSubregion(nbt, subReg, "y"), getMinOfSubregion(nbt, subReg, "z"));
        int index = 0;
        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    System.out.println("checking x: " + x + " z: " + z + " y: " + y);
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
     * @return x size of the schematic.
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return y size of the schematic.
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return z size of the schematic.
     */
    public int getZ() {
        return this.z;
    }

    /**
     * @param x          position relative to the minimum corner of the schematic.
     * @param y          position relative to the minimum corner of the schematic.
     * @param z          position relative to the minimum corner of the schematic.
     * @param blockState new blockstate of the block at this position.
     */
    public void setDirect(int x, int y, int z, BlockState blockState) {
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
