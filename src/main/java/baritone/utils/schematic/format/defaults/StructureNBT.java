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

import baritone.utils.accessor.ITemplate;
import baritone.utils.schematic.StaticSchematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.Template;

import java.util.List;

/**
 * @author Sam
 * @since 03/11/2022
 */
public final class StructureNBT extends StaticSchematic {

    public StructureNBT(NBTTagCompound schematic) {
        Template template = new Template();
        template.read(schematic);

        BlockPos size = template.getSize();
        this.x = size.getX();
        this.y = size.getY();
        this.z = size.getZ();

        List<Template.BlockInfo> blocks = ((ITemplate) template).getBlocks();
        this.states = new IBlockState[this.x][this.z][this.y];
        for (Template.BlockInfo block : blocks ) {
            this.states[block.pos.getX()][block.pos.getZ()][block.pos.getY()] = block.blockState;
        }
    }

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentState) {
        // Filtering out Structure Void
        if (x >= 0 && x < widthX() && y >= 0 && y < heightY() && z >= 0 && z < lengthZ()) {
            return this.states[x][z][y] != null;
        }
        return false;
    }
}
