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

/**
 * @author Brady
 */
public final class SphereSchematic extends CachedMaskSchematic {

    public SphereSchematic(ISchematic schematic, boolean filled) {
        super(schematic, new StaticMaskFunction() {

            private final double centerX = schematic.widthX() / 2.0;
            private final double centerY = schematic.heightY() / 2.0;
            private final double centerZ = schematic.lengthZ() / 2.0;
            private final double radiusSqX = this.centerX * this.centerX;
            private final double radiusSqY = this.centerY * this.centerY;
            private final double radiusSqZ = this.centerZ * this.centerZ;

            @Override
            public boolean partOfMask(int x, int y, int z) {
                double dx = Math.abs((x + 0.5) - this.centerX);
                double dy = Math.abs((y + 0.5) - this.centerY);
                double dz = Math.abs((z + 0.5) - this.centerZ);
                if (this.outside(dx, dy, dz)) {
                    return false;
                }
                return filled
                        || this.outside(dx + 1, dy, dz)
                        || this.outside(dx, dy + 1, dz)
                        || this.outside(dx, dy, dz + 1);
            }

            private boolean outside(double dx, double dy, double dz) {
                return dx * dx / this.radiusSqX + dy * dy / this.radiusSqY + dz * dz / this.radiusSqZ > 1;
            }
        });
    }
}
