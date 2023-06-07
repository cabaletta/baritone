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

package baritone.api.schematic;

import net.minecraft.block.state.IBlockState;

/**
 * @author Brady
 */
public class CylinderSchematic extends MaskSchematic {

    private final double cx, cz, rx, rz;
    private final boolean filled;

    public CylinderSchematic(ISchematic schematic, boolean filled) {
        super(schematic);
        this.cx = schematic.widthX() / 2.0;
        this.cz = schematic.lengthZ() / 2.0;
        this.rx = this.cx * this.cx;
        this.rz = this.cz * this.cz;
        this.filled = filled;
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        double dx = Math.abs((x + 0.5) - this.cx);
        double dz = Math.abs((z + 0.5) - this.cz);
        return !this.outside(dx, dz)
                && (this.filled || outside(dx + 1, dz) || outside(dx, dz + 1));
    }

    private boolean outside(double dx, double dz) {
        return dx * dx / this.rx + dz * dz / this.rz > 1;
    }
}
