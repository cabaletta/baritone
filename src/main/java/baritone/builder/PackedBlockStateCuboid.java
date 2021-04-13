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

package baritone.builder;

public class PackedBlockStateCuboid {

    public final CuboidBounds bounds;
    private final int[] states;

    public PackedBlockStateCuboid(int[][][] blockStates) {
        this.bounds = new CuboidBounds(blockStates.length, blockStates[0].length, blockStates[0][0].length);
        this.states = new int[bounds.size];
        bounds.forEach((x, y, z) -> states[bounds.toIndex(x, y, z)] = blockStates[x][y][z]);
    }

    public int get(int index) {
        return states[index];
    }
}
