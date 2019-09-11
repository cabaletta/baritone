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
import com.google.common.collect.ImmutableMap;
import com.mojang.bridge.game.GameSession;
import baritone.utils.schematic.litematica.PositionUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.data.BlockListReport;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.nbt.NBTUtil;
import baritone.utils.schematic.litematica.LitematicaBlockStateContainer;

import java.io.File;

public class Schematic implements ISchematic {
    public int widthX;
    public int heightY;
    public int lengthZ;
    protected BlockState[][][] states;

    public Schematic(CompoundNBT schematic) {
        int ver = schematic.getInt("Version");
        if (!(ver == 5)) {
            throw new IllegalStateException("bad schematic version: " + ver);
        }

        CompoundNBT regions = schematic.getCompound("Regions");
        CompoundNBT metadata = schematic.getCompound("Metadata");
        widthX = metadata.getCompound("EnclosingSize").getInt("x");
        heightY = metadata.getCompound("EnclosingSize").getInt("y");
        lengthZ = metadata.getCompound("EnclosingSize").getInt("z");
        states = new BlockState[widthX][lengthZ][heightY];

        for (String regionKey : regions.keySet()) {
            CompoundNBT region = regions.getCompound(regionKey);
            // BlockPos regionPos = NBTUtil.readBlockPos(region.getCompound("Position")); Not doing this because this method expects uppercase "X", "Y", "Z" tag keys in the compound. In litematic those are lowercase.
            int posX = region.getCompound("Position").getInt("x");
            int posY = region.getCompound("Position").getInt("y");
            int posZ = region.getCompound("Position").getInt("z");
            BlockPos regionPos = new BlockPos(posX, posY, posZ);
            // BlockPos regionSize = NBTUtil.readBlockPos(region.getCompound("Size")); Same goes for region size.
            int sizX = region.getCompound("Size").getInt("x");
            int sizY = region.getCompound("Size").getInt("y");
            int sizZ = region.getCompound("Size").getInt("z");
            BlockPos regionSize = new BlockPos(Math.abs(sizX), Math.abs(sizY), Math.abs(sizZ));
            ListNBT blockStatePalette = region.getList("BlockStatePalette", 10);
            long[] blockStateArr = region.getLongArray("BlockStates");


            BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(new BlockPos(sizX, sizY, sizZ)).add(regionPos);
            BlockPos posMin = PositionUtils.getMinCorner(regionPos, posEndRel);
            LitematicaBlockStateContainer container = LitematicaBlockStateContainer.createFrom(blockStatePalette, blockStateArr, regionSize);

            for (int y = 0; y < regionSize.getY(); y++) {
                for (int z = 0; z < regionSize.getZ(); z++) {
                    for (int x = 0; x < regionSize.getX(); x++) {
                        states[x + posMin.getX()][z + posMin.getZ()][y + posMin.getY()] = container.get(x, y, z);
                    }
                }
            }
        }
        // throw new UnsupportedOperationException("1.13 be like: numeric IDs btfo");
    }

    @Override
    public BlockState desiredState(int x, int y, int z) {
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
