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

import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.IStaticSchematic;
import net.minecraft.block.state.IBlockState;

import java.util.List;

/**
 * Default implementation of {@link IStaticSchematic}
 *
 * @author Brady
 * @since 12/23/2019
 */
public class StaticSchematic extends AbstractSchematic implements IStaticSchematic {

    protected IBlockState[][][] states;

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current, List<IBlockState> approxPlaceable) {
        return this.states[x][z][y];
    }

    @Override
    public IBlockState getDirect(int x, int y, int z) {
        return this.states[x][z][y];
    }

    @Override
    public IBlockState[] getColumn(int x, int z) {
        return this.states[x][z];
    }
}
