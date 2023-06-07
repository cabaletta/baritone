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
import net.minecraft.util.math.Vec3d;

/**
 * @author Brady
 */
public class SphereSchematic extends MaskSchematic {

    private final double cx, cy, cz, rx, ry, rz;
    private final boolean filled;

    public SphereSchematic(ISchematic schematic, boolean filled) {
        super(schematic);
        this.cx = schematic.widthX() / 2.0;
        this.cy = schematic.heightY() / 2.0;
        this.cz = schematic.lengthZ() / 2.0;
        this.rx = this.cx * this.cx;
        this.ry = this.cy * this.cy;
        this.rz = this.cz * this.cz;
        this.filled = filled;
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        double dx = Math.abs((x + 0.5) - this.cx);
        double dy = Math.abs((y + 0.5) - this.cy);
        double dz = Math.abs((z + 0.5) - this.cz);
        return !this.outside(dx, dy, dz)
                && (this.filled || outside(dx + 1, dy, dz) || outside(dx, dy + 1, dz) || outside(dx, dy, dz + 1));
    }

    private boolean outside(double dx,double dy, double dz) {
        return dx * dx / this.rx + dy * dy / this.ry + dz * dz / this.rz > 1;
    }
}
