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

import net.minecraft.util.EnumFacing;

/**
 * @author Brady
 */
public final class CylinderSchematic extends CachedMaskSchematic {

    public CylinderSchematic(ISchematic schematic, boolean filled, EnumFacing.Axis alignment) {
        super(schematic, new StaticMaskFunction() {

            private final double centerA = this.getA(schematic.widthX(), schematic.heightY()) / 2.0;
            private final double centerB = this.getB(schematic.heightY(), schematic.lengthZ()) / 2.0;
            private final double radiusSqA = this.centerA * this.centerA;
            private final double radiusSqB = this.centerB * this.centerB;

            @Override
            public boolean partOfMask(int x, int y, int z) {
                double da = Math.abs((this.getA(x, y) + 0.5) - this.centerA);
                double db = Math.abs((this.getB(y, z) + 0.5) - this.centerB);
                return !this.outside(da, db)
                        && (filled || outside(da + 1, db) || outside(da, db + 1));
            }

            private boolean outside(double da, double db) {
                return da * da / this.radiusSqA + db * db / this.radiusSqB > 1;
            }

            private int getA(int x, int y) {
                return alignment == EnumFacing.Axis.X ? y : x;
            }

            private int getB(int y, int z) {
                return alignment == EnumFacing.Axis.Z ? y : z;
            }
        });
    }
}
