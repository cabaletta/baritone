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

import baritone.api.utils.BetterBlockPos;
import org.junit.Test;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ScaffolderTest {
    @Test
    public void test() {
        BlockStateCachedData[][][] test = new BlockStateCachedData[8][8][8];
        PackedBlockStateCuboid.fillWithAir(test);
        for (int x = 0; x < test.length; x++) {
            for (int z = 0; z < test[0][0].length; z++) {
                test[x][0][z] = FakeStates.SOLID;
                test[x][1][z] = FakeStates.SOLID;
            }
        }
        test[5][5][5] = FakeStates.SOLID;
        test[5][5][6] = FakeStates.SOLID;
        test[0][5][5] = FakeStates.SOLID;
        Consumer<DependencyGraphScaffoldingOverlay> debug = dgso -> {
            for (int y = 0; y < test[0].length; y++) {
                System.out.println("Layer " + y);
                for (int x = 0; x < test.length; x++) {
                    for (int z = 0; z < test[0][0].length; z++) {
                        long pos = BetterBlockPos.toLong(x, y, z);
                        if (dgso.real(pos)) {
                            System.out.print("A" + dgso.getCollapsedGraph().getComponentLocations().get(pos).deletedIntoRecursive());
                        } else {
                            System.out.print(" ");
                        }
                    }
                    System.out.println();
                }
            }
        };
        PackedBlockStateCuboid states = new PackedBlockStateCuboid(test);
        PlaceOrderDependencyGraph graph = new PlaceOrderDependencyGraph(states);
        System.out.println("N " + Face.NORTH.z);
        System.out.println("S " + Face.SOUTH.z);
        for (int z = 0; z < test[0][0].length; z++) {
            //System.out.println(states.get(states.bounds.toIndex(0, 0, z)));
            System.out.println(z + " " + graph.outgoingEdge(BetterBlockPos.toLong(0, 0, z), Face.NORTH) + " " + graph.outgoingEdge(BetterBlockPos.toLong(0, 0, z), Face.SOUTH));
        }
        DependencyGraphAnalyzer.prevalidate(graph);
        DependencyGraphAnalyzer.prevalidateExternalToInteriorSearch(graph);
        DependencyGraphScaffoldingOverlay scaffolding = new DependencyGraphScaffoldingOverlay(graph);
        System.out.println("Hewwo");
        scaffolding.getCollapsedGraph().getComponents().forEach((key, value) -> {
            System.out.println(key);
            System.out.println(value.getPositions().stream().map(BetterBlockPos::fromLong).collect(Collectors.toList()));
        });
        System.out.println();
        debug.accept(scaffolding);
        Scaffolder.Output out = Scaffolder.run(graph, DijkstraScaffolder.INSTANCE);
        debug.accept(out.secretInternalForTesting());
    }
}
