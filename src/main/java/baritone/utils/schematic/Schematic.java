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

package baritone.utils.schematic;

import baritone.api.utils.ISchematic;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

public class Schematic implements ISchematic {
    public final int widthX;
    public final int heightY;
    public final int lengthZ;
    protected final IBlockState[][][] states;

    public Schematic(NBTTagCompound schematic) {
        String type = schematic.getString("Materials");
        if (!type.equals("Alpha")) {
            throw new IllegalStateException("bad schematic " + type);
        }
        widthX = schematic.getInteger("Width");
        heightY = schematic.getInteger("Height");
        lengthZ = schematic.getInteger("Length");
        byte[] blocks = schematic.getByteArray("Blocks");
        byte[] metadata = schematic.getByteArray("Data");

        byte[] additional = null;
        if (schematic.hasKey("AddBlocks")) {
            byte[] addBlocks = schematic.getByteArray("AddBlocks");
            additional = new byte[addBlocks.length * 2];
            for (int i = 0; i < addBlocks.length; i++) {
                additional[i * 2 + 0] = (byte) ((addBlocks[i] >> 4) & 0xF); // lower nibble
                additional[i * 2 + 1] = (byte) ((addBlocks[i] >> 0) & 0xF); // upper nibble
            }
        }
        states = new IBlockState[widthX][lengthZ][heightY];
        for (int y = 0; y < heightY; y++) {
            for (int z = 0; z < lengthZ; z++) {
                for (int x = 0; x < widthX; x++) {
                    int blockInd = (y * lengthZ + z) * widthX + x;

                    int blockID = blocks[blockInd] & 0xFF;
                    if (additional != null) {
                        // additional is 0 through 15 inclusive since it's & 0xF above
                        blockID |= additional[blockInd] << 8;
                    }
                    Block block = Block.REGISTRY.getObjectById(blockID);
                    int meta = metadata[blockInd] & 0xFF;
                    states[x][z][y] = block.getStateFromMeta(meta);
                }
            }
        }
    }

    @Override
    public IBlockState desiredState(int x, int y, int z) {
        return states[x][z][y];
    }

    @Override
    public int widthX() {
        return widthX;
    }

    @Override
    public int heightY() {
        return heightY;
    }

    @Override
    public int lengthZ() {
        return lengthZ;
    }
}
