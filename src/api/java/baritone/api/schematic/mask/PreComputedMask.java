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

package baritone.api.schematic.mask;

/**
 * @author Brady
 */
final class PreComputedMask extends AbstractMask implements StaticMask {

    private final boolean[][][] mask;

    public PreComputedMask(StaticMask mask) {
        super(mask.widthX(), mask.heightY(), mask.lengthZ());

        this.mask = new boolean[this.heightY()][this.lengthZ()][this.widthX()];
        for (int y = 0; y < this.heightY(); y++) {
            for (int z = 0; z < this.lengthZ(); z++) {
                for (int x = 0; x < this.widthX(); x++) {
                    this.mask[y][z][x] = mask.partOfMask(x, y, z);
                }
            }
        }
    }

    @Override
    public boolean partOfMask(int x, int y, int z) {
        return this.mask[y][z][x];
    }
}
