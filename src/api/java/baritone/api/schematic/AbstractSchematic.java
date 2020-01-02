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

public abstract class AbstractSchematic implements ISchematic {

    protected int x;
    protected int y;
    protected int z;

    public AbstractSchematic() {
        this(0, 0, 0);
    }

    public AbstractSchematic(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int widthX() {
        return x;
    }

    @Override
    public int heightY() {
        return y;
    }

    @Override
    public int lengthZ() {
        return z;
    }
}
