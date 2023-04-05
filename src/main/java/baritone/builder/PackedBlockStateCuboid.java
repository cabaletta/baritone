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

    public final Bounds bounds;
    private final BlockStateCachedData[] states;
    private final BlockStateCachedData[] statesWithScaffolding;

    private PackedBlockStateCuboid(int x, int y, int z) {
        this.bounds = new CuboidBounds(x, y, z);
        this.states = new BlockStateCachedData[bounds.volume()];
        this.statesWithScaffolding = new BlockStateCachedData[bounds.volume()];
    }

    public PackedBlockStateCuboid(int[][][] blockStates, BlockData data) {
        this(blockStates.length, blockStates[0].length, blockStates[0][0].length);
        bounds.forEach((x, y, z) -> states[bounds.toIndex(x, y, z)] = data.get(blockStates[x][y][z]));
        genScaffoldVariant();
    }

    private void genScaffoldVariant() {
        for (int i = 0; i < states.length; i++) {
            statesWithScaffolding[i] = states[i].isAir ? FakeStates.SCAFFOLDING : states[i];
        }
    }

    public BlockStateCachedData get(int index) {
        return states[index];
    }

    public BlockStateCachedData getScaffoldingVariant(int index) {
        return statesWithScaffolding[index];
    }
}
