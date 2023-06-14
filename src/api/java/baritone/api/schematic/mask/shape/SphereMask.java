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

package baritone.api.schematic.mask.shape;

import baritone.api.schematic.mask.AbstractMask;
import baritone.api.schematic.mask.StaticMask;

/**
 * @author Brady
 */
public final class SphereMask extends AbstractMask implements StaticMask {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radiusSqX;
    private final double radiusSqY;
    private final double radiusSqZ;
    private final boolean filled;

    public SphereMask(int widthX, int heightY, int lengthZ, boolean filled) {
        super(widthX, heightY, lengthZ);
        this.centerX = widthX / 2.0;
        this.centerY = heightY / 2.0;
        this.centerZ = lengthZ / 2.0;
        this.radiusSqX = this.centerX * this.centerX;
        this.radiusSqY = this.centerY * this.centerY;
        this.radiusSqZ = this.centerZ * this.centerZ;
        this.filled = filled;
    }

    @Override
    public boolean partOfMask(int x, int y, int z) {
        double dx = Math.abs((x + 0.5) - this.centerX);
        double dy = Math.abs((y + 0.5) - this.centerY);
        double dz = Math.abs((z + 0.5) - this.centerZ);
        if (this.outside(dx, dy, dz)) {
            return false;
        }
        return this.filled
                || this.outside(dx + 1, dy, dz)
                || this.outside(dx, dy + 1, dz)
                || this.outside(dx, dy, dz + 1);
    }

    private boolean outside(double dx, double dy, double dz) {
        return dx * dx / this.radiusSqX + dy * dy / this.radiusSqY + dz * dz / this.radiusSqZ > 1;
    }
}
