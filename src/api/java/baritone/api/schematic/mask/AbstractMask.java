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
public abstract class AbstractMask implements Mask {

    private final int widthX;
    private final int heightY;
    private final int lengthZ;

    public AbstractMask(int widthX, int heightY, int lengthZ) {
        this.widthX = widthX;
        this.heightY = heightY;
        this.lengthZ = lengthZ;
    }

    @Override
    public int widthX() {
        return this.widthX;
    }

    @Override
    public int heightY() {
        return this.heightY;
    }

    @Override
    public int lengthZ() {
        return this.lengthZ;
    }
}
