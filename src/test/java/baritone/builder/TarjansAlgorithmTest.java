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

import org.junit.Test;

import java.util.Random;

public class TarjansAlgorithmTest {
    @Test
    public void test() {
        // the correctness test is already in there just gotta ask for it
        Random RAND = new Random(5021);
        for (int i = 0; i < 100; i++) {
            BlockStateCachedData[][][] test = new BlockStateCachedData[20][20][20];
            int density = i <= 10 ? i : i % 4; // high density is slow so only do them once
            for (int x = 0; x < test.length; x++) {
                for (int y = 0; y < test[0].length; y++) {
                    for (int z = 0; z < test[0][0].length; z++) {
                        if (RAND.nextInt(10) < density) {
                            test[x][y][z] = FakeStates.probablyCanBePlaced(RAND);
                        } else {
                            test[x][y][z] = FakeStates.AIR;
                        }
                    }

                }
            }
            PackedBlockStateCuboid states = new PackedBlockStateCuboid(test);
            PlaceOrderDependencyGraph graph = new PlaceOrderDependencyGraph(states);
            DependencyGraphScaffoldingOverlay overlay = new DependencyGraphScaffoldingOverlay(graph); // this runs tarjan's twice, but the alternative ruins the abstraction layers too much :)
            TarjansAlgorithm.TarjansResult result = TarjansAlgorithm.run(overlay);
            TarjansAlgorithm.sanityCheckResult(overlay, result);
        }
    }
}
