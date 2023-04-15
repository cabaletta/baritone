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

/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/dynamic-connectivity/
 */

package baritone.builder.connectivity;

import baritone.builder.EulerTourForest;
import baritone.builder.utils.com.github.btrekkie.connectivity.ConnGraph;
import baritone.builder.utils.com.github.btrekkie.connectivity.ConnVertex;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/* Note that most of the ConnGraphTest test methods use the one-argument ConnVertex constructor, in order to make their
 * behavior more predictable. That way, there are consistent test results, and test failures are easier to debug.
 */
public class ConnGraphTest {
    private static long toLong(int x, int y) {
        return (long) x & 0xffffffffL | ((long) y & 0xffffffffL) << 32;
    }

    @Test
    public void testPerformanceOnRepeatedConnectionAndDisconnection() {
        if (true) {
            return; // slow
        }
        EulerTourForest.sanityCheck2();
        EulerTourForest.sanityCheck();
        for (int trial = 0; trial < 1; trial++) {
            try {
                Thread.sleep(2000);
                System.gc();
                Thread.sleep(2000);
            } catch (InterruptedException ex) {}
            long setup = System.currentTimeMillis();
            ConnGraph graph = new ConnGraph((a, b) -> (Integer) a + (Integer) b);
            int SZ = 1000;
            for (int x = 0; x < SZ; x++) {
                for (int y = 0; y < SZ; y++) {
                    graph.setVertexAugmentation(toLong(x, y), 1); // much faster to do this earlier idk
                }
            }
            for (int x = 0; x < SZ; x++) {
                if (x % 100 == 0) {
                    System.out.println("Indicating progress: connected row " + x);
                }
                for (int y = 0; y < SZ; y++) {
                    if (y != SZ - 1 && y != SZ / 2) { // leave graph disconnected in the center - two big areas with no connection
                        graph.addEdge(toLong(x, y), toLong(x, y + 1));
                    }
                    if (x != SZ - 1) {
                        graph.addEdge(toLong(x, y), toLong(x + 1, y));
                    }
                }
            }
            System.out.println("Setup took " + (System.currentTimeMillis() - setup));
            System.out.println("Part size " + graph.getComponentAugmentation(0));

        /*
        // previous test for cutting in half
        long a = System.currentTimeMillis();
        for (int x = 0; x < SZ; x++) {
            int y = SZ / 2;
            graph.removeEdge(vertices[x*SZ+y],vertices[x*SZ+y+1]);
            System.out.println("Sz " + graph.getComponentAugmentation(vertices[0]));
        }
        System.out.println("Time: " + (System.currentTimeMillis() - a));
        */

            // try connecting and disconnecting one edge

            for (int reconnectTrial = 0; reconnectTrial < 10; reconnectTrial++) { // then try connecting and disconnecting them
                long start = System.currentTimeMillis();
                int x = SZ / 2;
                int y = SZ / 2;
                graph.addEdge(toLong(x, y), toLong(x, y + 1));
                long afterAdd = System.currentTimeMillis();
                System.out.println("Connected size " + graph.getComponentAugmentation(0));
                graph.removeEdge(toLong(x, y), toLong(x, y + 1));
                System.out.println("Disconnected size " + graph.getComponentAugmentation(0));
                System.out.println("Took " + (System.currentTimeMillis() - afterAdd) + " to remove and " + (afterAdd - start) + " to add");
            }

            System.out.println("entire row");

            // now try connecting and disconnecting the entire row

            for (int reconnectTrial = 0; reconnectTrial < 10; reconnectTrial++) { // then try connecting and disconnecting them
                long start = System.currentTimeMillis();
                int y = SZ / 2;
                for (int x = 0; x < SZ; x++) {
                    graph.addEdge(toLong(x, y), toLong(x, y + 1));
                }
                long afterAdd = System.currentTimeMillis();
                System.out.println("Connected size " + graph.getComponentAugmentation(0));
                for (int x = 0; x < SZ; x++) {
                    graph.removeEdge(toLong(x, y), toLong(x, y + 1));
                }
                System.out.println("Disconnected size " + graph.getComponentAugmentation(0));
                System.out.println("Took " + (System.currentTimeMillis() - afterAdd) + " to remove and " + (afterAdd - start) + " to add");
            }

            // entire column
            System.out.println("Part size " + graph.getComponentAugmentation(0));
            {
                int y = SZ / 2;
                for (int x = 0; x < SZ; x++) {
                    graph.addEdge(toLong(x, y), toLong(x, y + 1));
                }
            }
            System.out.println("Part size " + graph.getComponentAugmentation(0));
            long col = System.currentTimeMillis();
            {
                int x = SZ / 2;
                for (int y = 0; y < SZ; y++) {
                    graph.removeEdge(toLong(x, y), toLong(x + 1, y));
                }
            }
            System.out.println("Part size " + graph.getComponentAugmentation(0));
            System.out.println("Column took " + (System.currentTimeMillis() - col));
        }
    }

    /**
     * Tests ConnectivityGraph on a small forest and a binary tree-like subgraph.
     */
    @Test
    public void testForestAndBinaryTree() {
        ConnGraph graph = new ConnGraph();
        Random random = new Random(6170);
        ConnVertex vertex1 = new ConnVertex(random);
        ConnVertex vertex2 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex1, vertex2));
        ConnVertex vertex3 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex3, vertex1));
        ConnVertex vertex4 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex1, vertex4));
        ConnVertex vertex5 = new ConnVertex(random);
        ConnVertex vertex6 = new ConnVertex(random);
        ConnVertex vertex7 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex6, vertex7));
        assertTrue(graph.addEdge(vertex6, vertex5));
        assertTrue(graph.addEdge(vertex4, vertex5));
        assertFalse(graph.addEdge(vertex1, vertex3));
        ConnVertex vertex8 = new ConnVertex(random);
        ConnVertex vertex9 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex8, vertex9));
        ConnVertex vertex10 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex8, vertex10));
        assertFalse(graph.removeEdge(vertex7, vertex1));
        assertTrue(graph.connected(vertex1, vertex4));
        assertTrue(graph.connected(vertex1, vertex1));
        assertTrue(graph.connected(vertex1, vertex2));
        assertTrue(graph.connected(vertex3, vertex6));
        assertTrue(graph.connected(vertex7, vertex4));
        assertTrue(graph.connected(vertex8, vertex9));
        assertTrue(graph.connected(vertex5, vertex2));
        assertTrue(graph.connected(vertex8, vertex10));
        assertTrue(graph.connected(vertex9, vertex10));
        assertFalse(graph.connected(vertex1, vertex8));
        assertFalse(graph.connected(vertex2, vertex10));
        assertTrue(graph.removeEdge(vertex4, vertex5));
        assertTrue(graph.connected(vertex1, vertex3));
        assertTrue(graph.connected(vertex2, vertex4));
        assertTrue(graph.connected(vertex5, vertex6));
        assertTrue(graph.connected(vertex5, vertex7));
        assertTrue(graph.connected(vertex8, vertex9));
        assertTrue(graph.connected(vertex3, vertex3));
        assertFalse(graph.connected(vertex1, vertex5));
        assertFalse(graph.connected(vertex4, vertex7));
        assertFalse(graph.connected(vertex1, vertex8));
        assertFalse(graph.connected(vertex6, vertex9));

        /*Set<ConnVertex> expectedAdjVertices = new HashSet<ConnVertex>();
        expectedAdjVertices.add(vertex2);
        expectedAdjVertices.add(vertex3);
        expectedAdjVertices.add(vertex4);
        assertEquals(expectedAdjVertices, new HashSet<ConnVertex>(graph.adjacentVertices(vertex1)));
        expectedAdjVertices.clear();
        expectedAdjVertices.add(vertex5);
        expectedAdjVertices.add(vertex7);
        assertEquals(expectedAdjVertices, new HashSet<ConnVertex>(graph.adjacentVertices(vertex6)));
        assertEquals(Collections.singleton(vertex8), new HashSet<ConnVertex>(graph.adjacentVertices(vertex9)));
        assertEquals(Collections.emptySet(), new HashSet<ConnVertex>(graph.adjacentVertices(new ConnVertex(random))));*/
        graph.optimize();

        List<ConnVertex> vertices = new ArrayList<ConnVertex>(1000);
        for (int i = 0; i < 1000; i++) {
            vertices.add(new ConnVertex(random));
        }
        for (int i = 0; i < 1000; i++) {
            if (i > 0 && Integer.bitCount(i) <= 3) {
                graph.addEdge(vertices.get(i), vertices.get((i - 1) / 2));
            }
        }
        for (int i = 0; i < 1000; i++) {
            if (Integer.bitCount(i) > 3) {
                graph.addEdge(vertices.get((i - 1) / 2), vertices.get(i));
            }
        }
        for (int i = 15; i < 31; i++) {
            graph.removeEdge(vertices.get(i), vertices.get((i - 1) / 2));
        }
        assertTrue(graph.connected(vertices.get(0), vertices.get(0)));
        assertTrue(graph.connected(vertices.get(11), vertices.get(2)));
        assertTrue(graph.connected(vertices.get(7), vertices.get(14)));
        assertTrue(graph.connected(vertices.get(0), vertices.get(10)));
        assertFalse(graph.connected(vertices.get(0), vertices.get(15)));
        assertFalse(graph.connected(vertices.get(15), vertices.get(16)));
        assertFalse(graph.connected(vertices.get(14), vertices.get(15)));
        assertFalse(graph.connected(vertices.get(7), vertices.get(605)));
        assertFalse(graph.connected(vertices.get(5), vertices.get(87)));
        assertTrue(graph.connected(vertices.get(22), vertices.get(22)));
        assertTrue(graph.connected(vertices.get(16), vertices.get(70)));
        assertTrue(graph.connected(vertices.get(113), vertices.get(229)));
        assertTrue(graph.connected(vertices.get(21), vertices.get(715)));
        assertTrue(graph.connected(vertices.get(175), vertices.get(715)));
        assertTrue(graph.connected(vertices.get(30), vertices.get(999)));
        assertTrue(graph.connected(vertices.get(991), vertices.get(999)));
    }

    /**
     * Tests ConnectivityGraph on a small graph that has cycles.
     */
    @Test
    public void testSmallCycles() {
        ConnGraph graph = new ConnGraph();
        Random random = new Random(6170);
        ConnVertex vertex1 = new ConnVertex(random);
        ConnVertex vertex2 = new ConnVertex(random);
        ConnVertex vertex3 = new ConnVertex(random);
        ConnVertex vertex4 = new ConnVertex(random);
        ConnVertex vertex5 = new ConnVertex(random);
        assertTrue(graph.addEdge(vertex1, vertex2));
        assertTrue(graph.addEdge(vertex2, vertex3));
        assertTrue(graph.addEdge(vertex1, vertex3));
        assertTrue(graph.addEdge(vertex2, vertex4));
        assertTrue(graph.addEdge(vertex3, vertex4));
        assertTrue(graph.addEdge(vertex4, vertex5));
        assertTrue(graph.connected(vertex5, vertex1));
        assertTrue(graph.connected(vertex1, vertex4));
        assertTrue(graph.removeEdge(vertex4, vertex5));
        assertFalse(graph.connected(vertex4, vertex5));
        assertFalse(graph.connected(vertex5, vertex1));
        assertTrue(graph.connected(vertex1, vertex4));
        assertTrue(graph.removeEdge(vertex1, vertex2));
        assertTrue(graph.removeEdge(vertex3, vertex4));
        assertTrue(graph.connected(vertex1, vertex4));
        assertTrue(graph.removeEdge(vertex2, vertex3));
        assertTrue(graph.connected(vertex1, vertex3));
        assertTrue(graph.connected(vertex2, vertex4));
        assertFalse(graph.connected(vertex1, vertex4));
    }

    /**
     * Tests ConnectivityGraph on a grid-based graph.
     */
    @Test
    public void testGrid() {
        ConnGraph graph = new ConnGraph();
        Random random = new Random(6170);
        ConnVertex vertex = new ConnVertex(random);
        assertTrue(graph.connected(vertex, vertex));

        graph = new ConnGraph(SumAndMax.AUGMENTATION);
        List<List<ConnVertex>> vertices = new ArrayList<List<ConnVertex>>(20);
        for (int y = 0; y < 20; y++) {
            List<ConnVertex> row = new ArrayList<ConnVertex>(20);
            for (int x = 0; x < 20; x++) {
                row.add(new ConnVertex(random));
            }
            vertices.add(row);
        }
        for (int y = 0; y < 19; y++) {
            for (int x = 0; x < 19; x++) {
                assertTrue(graph.addEdge(vertices.get(y).get(x), vertices.get(y).get(x + 1)));
                assertTrue(graph.addEdge(vertices.get(y).get(x), vertices.get(y + 1).get(x)));
            }
        }
        graph.optimize();

        assertTrue(graph.connected(vertices.get(0).get(0), vertices.get(15).get(12)));
        assertTrue(graph.connected(vertices.get(0).get(0), vertices.get(18).get(19)));
        assertFalse(graph.connected(vertices.get(0).get(0), vertices.get(19).get(19)));
        assertFalse(graph.removeEdge(vertices.get(18).get(19), vertices.get(19).get(19)));
        assertFalse(graph.removeEdge(vertices.get(0).get(0), vertices.get(2).get(2)));

        assertTrue(graph.removeEdge(vertices.get(12).get(8), vertices.get(11).get(8)));
        assertTrue(graph.removeEdge(vertices.get(12).get(9), vertices.get(11).get(9)));
        assertTrue(graph.removeEdge(vertices.get(12).get(8), vertices.get(12).get(7)));
        assertTrue(graph.removeEdge(vertices.get(13).get(8), vertices.get(13).get(7)));
        assertTrue(graph.removeEdge(vertices.get(13).get(8), vertices.get(14).get(8)));
        assertTrue(graph.removeEdge(vertices.get(12).get(9), vertices.get(12).get(10)));
        assertTrue(graph.removeEdge(vertices.get(13).get(9), vertices.get(13).get(10)));
        assertTrue(graph.connected(vertices.get(2).get(1), vertices.get(12).get(8)));
        assertTrue(graph.connected(vertices.get(12).get(8), vertices.get(13).get(9)));
        assertTrue(graph.removeEdge(vertices.get(13).get(9), vertices.get(14).get(9)));
        assertFalse(graph.connected(vertices.get(2).get(1), vertices.get(12).get(8)));
        assertTrue(graph.connected(vertices.get(12).get(8), vertices.get(13).get(9)));
        assertFalse(graph.connected(vertices.get(11).get(8), vertices.get(12).get(8)));
        assertTrue(graph.connected(vertices.get(16).get(18), vertices.get(6).get(15)));
        assertTrue(graph.removeEdge(vertices.get(12).get(9), vertices.get(12).get(8)));
        assertTrue(graph.removeEdge(vertices.get(12).get(8), vertices.get(13).get(8)));
        assertFalse(graph.connected(vertices.get(2).get(1), vertices.get(12).get(8)));
        assertFalse(graph.connected(vertices.get(12).get(8), vertices.get(13).get(9)));
        assertFalse(graph.connected(vertices.get(11).get(8), vertices.get(12).get(8)));
        assertTrue(graph.connected(vertices.get(13).get(8), vertices.get(12).get(9)));

        assertTrue(graph.removeEdge(vertices.get(6).get(15), vertices.get(5).get(15)));
        assertTrue(graph.removeEdge(vertices.get(6).get(15), vertices.get(7).get(15)));
        assertTrue(graph.removeEdge(vertices.get(6).get(15), vertices.get(6).get(14)));
        assertTrue(graph.removeEdge(vertices.get(6).get(15), vertices.get(6).get(16)));
        assertFalse(graph.removeEdge(vertices.get(6).get(15), vertices.get(5).get(15)));
        assertFalse(graph.connected(vertices.get(16).get(18), vertices.get(6).get(15)));
        assertFalse(graph.connected(vertices.get(7).get(15), vertices.get(6).get(15)));
        graph.addEdge(vertices.get(6).get(15), vertices.get(7).get(15));
        assertTrue(graph.connected(vertices.get(16).get(18), vertices.get(6).get(15)));

        for (int y = 1; y < 19; y++) {
            for (int x = 1; x < 19; x++) {
                graph.removeEdge(vertices.get(y).get(x), vertices.get(y).get(x + 1));
                graph.removeEdge(vertices.get(y).get(x), vertices.get(y + 1).get(x));
            }
        }

        assertTrue(graph.addEdge(vertices.get(5).get(6), vertices.get(0).get(7)));
        assertTrue(graph.addEdge(vertices.get(12).get(8), vertices.get(5).get(6)));
        assertTrue(graph.connected(vertices.get(5).get(6), vertices.get(14).get(0)));
        assertTrue(graph.connected(vertices.get(12).get(8), vertices.get(0).get(17)));
        assertFalse(graph.connected(vertices.get(3).get(5), vertices.get(0).get(9)));
        assertFalse(graph.connected(vertices.get(14).get(2), vertices.get(11).get(18)));

        assertNull(graph.getVertexAugmentation(vertices.get(13).get(8)));
        assertNull(graph.getVertexAugmentation(vertices.get(6).get(4)));
        assertNull(graph.getComponentAugmentation(vertices.get(13).get(8)));
        assertNull(graph.getComponentAugmentation(vertices.get(6).get(4)));
        assertFalse(graph.vertexHasAugmentation(vertices.get(13).get(8)));
        assertFalse(graph.vertexHasAugmentation(vertices.get(6).get(4)));
        assertFalse(graph.componentHasAugmentation(vertices.get(13).get(8)));
        assertFalse(graph.componentHasAugmentation(vertices.get(6).get(4)));
    }

    /**
     * Tests the specified ConnGraph with a hub-and-spokes subgraph and a clique subgraph. The graph must be empty and
     * be augmented with SumAndMax objects.
     */
    private void checkWheelAndClique(ConnGraph graph) {
        Random random = new Random(6170);
        ConnVertex hub = new ConnVertex(random);
        List<ConnVertex> spokes1 = new ArrayList<ConnVertex>(10);
        List<ConnVertex> spokes2 = new ArrayList<ConnVertex>(10);
        for (int i = 0; i < 10; i++) {
            ConnVertex spoke1 = new ConnVertex(random);
            ConnVertex spoke2 = new ConnVertex(random);
            assertTrue(graph.addEdge(spoke1, spoke2));
            assertNull(graph.setVertexAugmentation(spoke1, new SumAndMax(i, i)));
            assertNull(graph.setVertexAugmentation(spoke2, new SumAndMax(i, i + 10)));
            spokes1.add(spoke1);
            spokes2.add(spoke2);
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(graph.addEdge(spokes1.get(i), hub));
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(graph.addEdge(hub, spokes2.get(i)));
        }

        List<ConnVertex> clique = new ArrayList<ConnVertex>(10);
        for (int i = 0; i < 10; i++) {
            ConnVertex vertex = new ConnVertex(random);
            assertNull(graph.setVertexAugmentation(vertex, new SumAndMax(i, i + 20)));
            clique.add(vertex);
        }
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                assertTrue(graph.addEdge(clique.get(i), clique.get(j)));
            }
        }
        assertTrue(graph.addEdge(hub, clique.get(0)));

        assertTrue(graph.connected(spokes1.get(5), clique.get(3)));
        assertTrue(graph.connected(spokes1.get(3), spokes2.get(8)));
        assertTrue(graph.connected(spokes1.get(4), spokes2.get(4)));
        assertTrue(graph.connected(clique.get(5), hub));
        SumAndMax expectedAugmentation = new SumAndMax(135, 29);
        assertEquals(expectedAugmentation, graph.getComponentAugmentation(spokes2.get(8)));
        assertTrue(graph.componentHasAugmentation(spokes2.get(8)));
        assertEquals(expectedAugmentation, graph.getComponentAugmentation(hub));
        assertEquals(expectedAugmentation, graph.getComponentAugmentation(clique.get(9)));
        assertEquals(new SumAndMax(4, 4), graph.getVertexAugmentation(spokes1.get(4)));
        assertTrue(graph.vertexHasAugmentation(spokes1.get(4)));
        assertNull(graph.getVertexAugmentation(hub));
        assertFalse(graph.vertexHasAugmentation(hub));

        assertTrue(graph.removeEdge(spokes1.get(5), hub));
        assertTrue(graph.connected(spokes1.get(5), clique.get(2)));
        assertTrue(graph.connected(spokes1.get(5), spokes1.get(8)));
        assertTrue(graph.connected(spokes1.get(5), spokes2.get(5)));
        assertEquals(new SumAndMax(135, 29), graph.getComponentAugmentation(hub));
        assertTrue(graph.removeEdge(spokes2.get(5), hub));
        assertFalse(graph.connected(spokes1.get(5), clique.get(2)));
        assertFalse(graph.connected(spokes1.get(5), spokes1.get(8)));
        assertTrue(graph.connected(spokes1.get(5), spokes2.get(5)));
        assertEquals(new SumAndMax(125, 29), graph.getComponentAugmentation(hub));
        assertTrue(graph.addEdge(spokes1.get(5), hub));
        assertTrue(graph.connected(spokes1.get(5), clique.get(2)));
        assertTrue(graph.connected(spokes1.get(5), spokes1.get(8)));
        assertTrue(graph.connected(spokes1.get(5), spokes2.get(5)));
        assertEquals(new SumAndMax(135, 29), graph.getComponentAugmentation(hub));

        assertTrue(graph.removeEdge(hub, clique.get(0)));
        assertFalse(graph.connected(spokes1.get(3), clique.get(4)));
        assertTrue(graph.connected(spokes2.get(7), hub));
        assertFalse(graph.connected(hub, clique.get(0)));
        assertTrue(graph.connected(spokes2.get(9), spokes1.get(5)));
        assertEquals(new SumAndMax(90, 19), graph.getComponentAugmentation(hub));
        assertEquals(new SumAndMax(90, 19), graph.getComponentAugmentation(spokes2.get(4)));
        assertEquals(new SumAndMax(45, 29), graph.getComponentAugmentation(clique.get(1)));

        assertEquals(new SumAndMax(9, 29), graph.setVertexAugmentation(clique.get(9), new SumAndMax(-20, 4)));
        for (int i = 0; i < 10; i++) {
            assertEquals(
                    new SumAndMax(i, i + 10), graph.setVertexAugmentation(spokes2.get(i), new SumAndMax(i - 1, i)));
        }
        assertNull(graph.removeVertexAugmentation(hub));
        assertEquals(new SumAndMax(4, 4), graph.removeVertexAugmentation(spokes1.get(4)));
        assertEquals(new SumAndMax(6, 7), graph.removeVertexAugmentation(spokes2.get(7)));

        assertEquals(new SumAndMax(70, 9), graph.getComponentAugmentation(hub));
        assertTrue(graph.componentHasAugmentation(hub));
        assertEquals(new SumAndMax(70, 9), graph.getComponentAugmentation(spokes1.get(6)));
        assertEquals(new SumAndMax(16, 28), graph.getComponentAugmentation(clique.get(4)));

        assertTrue(graph.addEdge(hub, clique.get(1)));
        expectedAugmentation = new SumAndMax(86, 28);
        assertEquals(expectedAugmentation, graph.getComponentAugmentation(hub));
        assertTrue(graph.componentHasAugmentation(hub));
        assertEquals(expectedAugmentation, graph.getComponentAugmentation(spokes2.get(7)));
        assertEquals(expectedAugmentation, graph.getComponentAugmentation(clique.get(3)));

        for (int i = 0; i < 10; i++) {
            assertTrue(graph.removeEdge(hub, spokes1.get(i)));
            if (i != 5) {
                assertTrue(graph.removeEdge(hub, spokes2.get(i)));
            }
        }
        assertFalse(graph.connected(hub, spokes1.get(8)));
        assertFalse(graph.connected(hub, spokes2.get(4)));
        assertTrue(graph.connected(hub, clique.get(5)));

        graph.clear();
        assertTrue(graph.addEdge(hub, spokes1.get(0)));
        assertTrue(graph.addEdge(hub, spokes2.get(0)));
        assertTrue(graph.addEdge(spokes1.get(0), spokes2.get(0)));
        assertTrue(graph.connected(hub, spokes1.get(0)));
        assertFalse(graph.connected(hub, spokes2.get(4)));
        assertTrue(graph.connected(clique.get(5), clique.get(5)));
        assertNull(graph.getComponentAugmentation(hub));
        assertNull(graph.getVertexAugmentation(spokes2.get(8)));
    }

    /**
     * Tests a graph with a hub-and-spokes subgraph and a clique subgraph.
     */
    @Test
    public void testWheelAndClique() {
        checkWheelAndClique(new ConnGraph(SumAndMax.AUGMENTATION));

        ConnGraph graph1 = new ConnGraph(SumAndMax.MUTATING_AUGMENTATION);
        checkWheelAndClique(graph1);
        graph1.clear();
        checkWheelAndClique(graph1);

        SumAndMaxPoolAndCache pool = new SumAndMaxPoolAndCache();
        ConnGraph graph2 = new ConnGraph(pool, pool);
        checkWheelAndClique(graph2);
        graph2.clear();
        checkWheelAndClique(graph2);
    }

    /**
     * Sets the matching between vertices.get(columnIndex) and vertices.get(columnIndex + 1) to the permutation
     * suggested by newPermutation. See the comments for the implementation of testPermutations().
     *
     * @param graph          The graph.
     * @param vertices       The vertices.
     * @param columnIndex    The index of the column.
     * @param oldPermutation The permutation for the current matching between vertices.get(columnIndex) and
     *                       vertices.get(columnIndex + 1). setPermutation removes the edges in this matching. If there are currently no
     *                       edges between those columns, then oldPermutation is null.
     * @param newPermutation The permutation for the new matching.
     * @return newPermutation.
     */
    private int[] setPermutation(
            ConnGraph graph, List<List<ConnVertex>> vertices, int columnIndex,
            int[] oldPermutation, int[] newPermutation) {
        List<ConnVertex> column1 = vertices.get(columnIndex);
        List<ConnVertex> column2 = vertices.get(columnIndex + 1);
        if (oldPermutation != null) {
            for (int i = 0; i < oldPermutation.length; i++) {
                assertTrue(graph.removeEdge(column1.get(i), column2.get(oldPermutation[i])));
            }
        }
        for (int i = 0; i < newPermutation.length; i++) {
            assertTrue(graph.addEdge(column1.get(i), column2.get(newPermutation[i])));
        }
        return newPermutation;
    }

    /**
     * Asserts that the specified permutation is the correct composite permutation for the specified column, i.e. that
     * for all i, vertices.get(0).get(i) is in the same connected component as
     * vertices.get(columnIndex + 1).get(expectedPermutation[i]). See the comments for the implementation of
     * testPermutations().
     */
    private void checkPermutation(
            ConnGraph graph, List<List<ConnVertex>> vertices, int columnIndex, int[] expectedPermutation) {
        List<ConnVertex> firstColumn = vertices.get(0);
        List<ConnVertex> column = vertices.get(columnIndex + 1);
        for (int i = 0; i < expectedPermutation.length; i++) {
            assertTrue(graph.connected(firstColumn.get(i), column.get(expectedPermutation[i])));
        }
    }

    /**
     * Asserts that the specified permutation differs from the correct composite permutation for the specified column in
     * every position, i.e. that for all i, vertices.get(0).get(i) is in a different connected component from
     * vertices.get(columnIndex + 1).get(wrongPermutation[i]). See the comments for the implementation of
     * testPermutations().
     */
    private void checkWrongPermutation(
            ConnGraph graph, List<List<ConnVertex>> vertices, int columnIndex, int[] wrongPermutation) {
        List<ConnVertex> firstColumn = vertices.get(0);
        List<ConnVertex> column = vertices.get(columnIndex + 1);
        for (int i = 0; i < wrongPermutation.length; i++) {
            assertFalse(graph.connected(firstColumn.get(i), column.get(wrongPermutation[i])));
        }
    }

    /**
     * Tests a graph in the style used to prove lower bounds on the performance of dynamic connectivity, as presented in
     * https://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-851-advanced-data-structures-spring-2012/lecture-videos/session-21-dynamic-connectivity-lower-bound/ .
     */
    @Test
    public void testPermutations() {
        // The graph used in testPermutations() uses an 8 x 9 grid of vertices, such that vertices.get(i).get(j) is the
        // vertex at row j, column i. There is a perfect matching between each pair of columns i and i + 1 - that is,
        // there are eight non-adjacent edges from vertices in column i to vertices in column i + 1. These form a
        // permutation, so that the element j of the permutation is the row number of the vertex in column i + 1 that is
        // adjacent to the vertex at row j, column i.
        ConnGraph graph = new ConnGraph();
        Random random = new Random(6170);
        List<List<ConnVertex>> vertices = new ArrayList<List<ConnVertex>>(9);
        for (int i = 0; i < 9; i++) {
            List<ConnVertex> column = new ArrayList<ConnVertex>(8);
            for (int j = 0; j < 8; j++) {
                column.add(new ConnVertex(random));
            }
            vertices.add(column);
        }

        int[] permutation0 = setPermutation(graph, vertices, 0, null, new int[]{2, 5, 0, 4, 7, 1, 3, 6});
        int[] permutation1 = setPermutation(graph, vertices, 1, null, new int[]{6, 5, 0, 7, 1, 2, 4, 3});
        int[] permutation2 = setPermutation(graph, vertices, 2, null, new int[]{2, 1, 7, 5, 6, 0, 4, 3});
        int[] permutation3 = setPermutation(graph, vertices, 3, null, new int[]{5, 2, 4, 6, 3, 0, 7, 1});
        int[] permutation4 = setPermutation(graph, vertices, 4, null, new int[]{5, 0, 2, 7, 4, 3, 1, 6});
        int[] permutation5 = setPermutation(graph, vertices, 5, null, new int[]{4, 7, 0, 1, 3, 6, 2, 5});
        int[] permutation6 = setPermutation(graph, vertices, 6, null, new int[]{4, 5, 3, 1, 7, 6, 2, 0});
        int[] permutation7 = setPermutation(graph, vertices, 7, null, new int[]{6, 7, 3, 0, 5, 1, 2, 4});

        permutation0 = setPermutation(graph, vertices, 0, permutation0, new int[]{7, 5, 3, 0, 4, 2, 1, 6});
        checkWrongPermutation(graph, vertices, 0, new int[]{5, 3, 0, 4, 2, 1, 6, 7});
        checkPermutation(graph, vertices, 0, new int[]{7, 5, 3, 0, 4, 2, 1, 6});
        permutation4 = setPermutation(graph, vertices, 4, permutation4, new int[]{2, 7, 0, 6, 5, 4, 1, 3});
        checkWrongPermutation(graph, vertices, 4, new int[]{7, 1, 6, 0, 5, 4, 3, 2});
        checkPermutation(graph, vertices, 4, new int[]{2, 7, 1, 6, 0, 5, 4, 3});
        permutation2 = setPermutation(graph, vertices, 2, permutation2, new int[]{3, 5, 6, 1, 4, 2, 7, 0});
        checkWrongPermutation(graph, vertices, 2, new int[]{6, 0, 7, 5, 3, 2, 4, 1});
        checkPermutation(graph, vertices, 2, new int[]{1, 6, 0, 7, 5, 3, 2, 4});
        permutation6 = setPermutation(graph, vertices, 6, permutation6, new int[]{4, 7, 1, 3, 6, 0, 5, 2});
        checkWrongPermutation(graph, vertices, 6, new int[]{7, 3, 0, 4, 2, 5, 1, 6});
        checkPermutation(graph, vertices, 6, new int[]{6, 7, 3, 0, 4, 2, 5, 1});
        permutation1 = setPermutation(graph, vertices, 1, permutation1, new int[]{2, 4, 0, 5, 6, 3, 7, 1});
        checkWrongPermutation(graph, vertices, 1, new int[]{3, 5, 2, 6, 0, 4, 7, 1});
        checkPermutation(graph, vertices, 1, new int[]{1, 3, 5, 2, 6, 0, 4, 7});
        permutation5 = setPermutation(graph, vertices, 5, permutation5, new int[]{5, 3, 2, 0, 7, 1, 6, 4});
        checkWrongPermutation(graph, vertices, 5, new int[]{5, 1, 0, 4, 3, 6, 7, 2});
        checkPermutation(graph, vertices, 5, new int[]{2, 5, 1, 0, 4, 3, 6, 7});
        permutation3 = setPermutation(graph, vertices, 3, permutation3, new int[]{1, 7, 3, 0, 4, 5, 6, 2});
        checkWrongPermutation(graph, vertices, 3, new int[]{7, 3, 6, 2, 0, 4, 1, 5});
        checkPermutation(graph, vertices, 3, new int[]{5, 7, 3, 6, 2, 0, 4, 1});
        permutation7 = setPermutation(graph, vertices, 7, permutation7, new int[]{4, 7, 5, 6, 2, 0, 1, 3});
        checkWrongPermutation(graph, vertices, 7, new int[]{2, 0, 6, 4, 7, 3, 1, 5});
        checkPermutation(graph, vertices, 7, new int[]{5, 2, 0, 6, 4, 7, 3, 1});
    }

    /**
     * Tests the specified ConnGraph with a graph based on the United States. The graph must be empty and be augmented
     * with SumAndMax objects.
     */
    private void checkUnitedStates(ConnGraph graph) {
        Random random = new Random(6170);
        ConnVertex alabama = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(alabama, new SumAndMax(7, 1819)));
        ConnVertex alaska = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(alaska, new SumAndMax(1, 1959)));
        ConnVertex arizona = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(arizona, new SumAndMax(9, 1912)));
        ConnVertex arkansas = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(arkansas, new SumAndMax(4, 1836)));
        ConnVertex california = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(california, new SumAndMax(53, 1850)));
        ConnVertex colorado = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(colorado, new SumAndMax(7, 1876)));
        ConnVertex connecticut = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(connecticut, new SumAndMax(5, 1788)));
        ConnVertex delaware = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(delaware, new SumAndMax(1, 1787)));
        ConnVertex florida = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(florida, new SumAndMax(27, 1845)));
        ConnVertex georgia = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(georgia, new SumAndMax(14, 1788)));
        ConnVertex hawaii = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(hawaii, new SumAndMax(2, 1959)));
        ConnVertex idaho = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(idaho, new SumAndMax(2, 1890)));
        ConnVertex illinois = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(illinois, new SumAndMax(18, 1818)));
        ConnVertex indiana = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(indiana, new SumAndMax(9, 1816)));
        ConnVertex iowa = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(iowa, new SumAndMax(4, 1846)));
        ConnVertex kansas = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(kansas, new SumAndMax(4, 1861)));
        ConnVertex kentucky = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(kentucky, new SumAndMax(6, 1792)));
        ConnVertex louisiana = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(louisiana, new SumAndMax(6, 1812)));
        ConnVertex maine = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(maine, new SumAndMax(2, 1820)));
        ConnVertex maryland = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(maryland, new SumAndMax(8, 1788)));
        ConnVertex massachusetts = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(massachusetts, new SumAndMax(9, 1788)));
        ConnVertex michigan = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(michigan, new SumAndMax(14, 1837)));
        ConnVertex minnesota = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(minnesota, new SumAndMax(8, 1858)));
        ConnVertex mississippi = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(mississippi, new SumAndMax(4, 1817)));
        ConnVertex missouri = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(missouri, new SumAndMax(8, 1821)));
        ConnVertex montana = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(montana, new SumAndMax(1, 1889)));
        ConnVertex nebraska = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(nebraska, new SumAndMax(3, 1867)));
        ConnVertex nevada = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(nevada, new SumAndMax(4, 1864)));
        ConnVertex newHampshire = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(newHampshire, new SumAndMax(2, 1788)));
        ConnVertex newJersey = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(newJersey, new SumAndMax(12, 1787)));
        ConnVertex newMexico = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(newMexico, new SumAndMax(3, 1912)));
        ConnVertex newYork = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(newYork, new SumAndMax(27, 1788)));
        ConnVertex northCarolina = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(northCarolina, new SumAndMax(13, 1789)));
        ConnVertex northDakota = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(northDakota, new SumAndMax(1, 1889)));
        ConnVertex ohio = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(ohio, new SumAndMax(16, 1803)));
        ConnVertex oklahoma = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(oklahoma, new SumAndMax(5, 1907)));
        ConnVertex oregon = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(oregon, new SumAndMax(5, 1859)));
        ConnVertex pennsylvania = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(pennsylvania, new SumAndMax(18, 1787)));
        ConnVertex rhodeIsland = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(rhodeIsland, new SumAndMax(2, 1790)));
        ConnVertex southCarolina = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(southCarolina, new SumAndMax(7, 1788)));
        ConnVertex southDakota = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(southDakota, new SumAndMax(1, 1889)));
        ConnVertex tennessee = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(tennessee, new SumAndMax(9, 1796)));
        ConnVertex texas = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(texas, new SumAndMax(36, 1845)));
        ConnVertex utah = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(utah, new SumAndMax(4, 1896)));
        ConnVertex vermont = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(vermont, new SumAndMax(1, 1791)));
        ConnVertex virginia = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(virginia, new SumAndMax(11, 1788)));
        ConnVertex washington = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(washington, new SumAndMax(10, 1889)));
        ConnVertex westVirginia = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(westVirginia, new SumAndMax(3, 1863)));
        ConnVertex wisconsin = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(wisconsin, new SumAndMax(8, 1848)));
        ConnVertex wyoming = new ConnVertex(random);
        assertNull(graph.setVertexAugmentation(wyoming, new SumAndMax(1, 1890)));

        assertTrue(graph.addEdge(alabama, florida));
        assertTrue(graph.addEdge(alabama, georgia));
        assertTrue(graph.addEdge(alabama, mississippi));
        assertTrue(graph.addEdge(alabama, tennessee));
        assertTrue(graph.addEdge(arizona, california));
        assertTrue(graph.addEdge(arizona, colorado));
        assertTrue(graph.addEdge(arizona, nevada));
        assertTrue(graph.addEdge(arizona, newMexico));
        assertTrue(graph.addEdge(arizona, utah));
        assertTrue(graph.addEdge(arkansas, louisiana));
        assertTrue(graph.addEdge(arkansas, mississippi));
        assertTrue(graph.addEdge(arkansas, missouri));
        assertTrue(graph.addEdge(arkansas, oklahoma));
        assertTrue(graph.addEdge(arkansas, tennessee));
        assertTrue(graph.addEdge(arkansas, texas));
        assertTrue(graph.addEdge(california, nevada));
        assertTrue(graph.addEdge(california, oregon));
        assertTrue(graph.addEdge(colorado, kansas));
        assertTrue(graph.addEdge(colorado, nebraska));
        assertTrue(graph.addEdge(colorado, newMexico));
        assertTrue(graph.addEdge(colorado, oklahoma));
        assertTrue(graph.addEdge(colorado, utah));
        assertTrue(graph.addEdge(colorado, wyoming));
        assertTrue(graph.addEdge(connecticut, massachusetts));
        assertTrue(graph.addEdge(connecticut, newYork));
        assertTrue(graph.addEdge(connecticut, rhodeIsland));
        assertTrue(graph.addEdge(delaware, maryland));
        assertTrue(graph.addEdge(delaware, newJersey));
        assertTrue(graph.addEdge(delaware, pennsylvania));
        assertTrue(graph.addEdge(florida, georgia));
        assertTrue(graph.addEdge(georgia, northCarolina));
        assertTrue(graph.addEdge(georgia, southCarolina));
        assertTrue(graph.addEdge(georgia, tennessee));
        assertTrue(graph.addEdge(idaho, montana));
        assertTrue(graph.addEdge(idaho, nevada));
        assertTrue(graph.addEdge(idaho, oregon));
        assertTrue(graph.addEdge(idaho, utah));
        assertTrue(graph.addEdge(idaho, washington));
        assertTrue(graph.addEdge(idaho, wyoming));
        assertTrue(graph.addEdge(illinois, indiana));
        assertTrue(graph.addEdge(illinois, iowa));
        assertTrue(graph.addEdge(illinois, kentucky));
        assertTrue(graph.addEdge(illinois, missouri));
        assertTrue(graph.addEdge(illinois, wisconsin));
        assertTrue(graph.addEdge(indiana, kentucky));
        assertTrue(graph.addEdge(indiana, michigan));
        assertTrue(graph.addEdge(indiana, ohio));
        assertTrue(graph.addEdge(iowa, minnesota));
        assertTrue(graph.addEdge(iowa, missouri));
        assertTrue(graph.addEdge(iowa, nebraska));
        assertTrue(graph.addEdge(iowa, southDakota));
        assertTrue(graph.addEdge(iowa, wisconsin));
        assertTrue(graph.addEdge(kansas, missouri));
        assertTrue(graph.addEdge(kansas, nebraska));
        assertTrue(graph.addEdge(kansas, oklahoma));
        assertTrue(graph.addEdge(kentucky, missouri));
        assertTrue(graph.addEdge(kentucky, ohio));
        assertTrue(graph.addEdge(kentucky, tennessee));
        assertTrue(graph.addEdge(kentucky, virginia));
        assertTrue(graph.addEdge(kentucky, westVirginia));
        assertTrue(graph.addEdge(louisiana, mississippi));
        assertTrue(graph.addEdge(louisiana, texas));
        assertTrue(graph.addEdge(maine, newHampshire));
        assertTrue(graph.addEdge(maryland, pennsylvania));
        assertTrue(graph.addEdge(maryland, virginia));
        assertTrue(graph.addEdge(maryland, westVirginia));
        assertTrue(graph.addEdge(massachusetts, newHampshire));
        assertTrue(graph.addEdge(massachusetts, newYork));
        assertTrue(graph.addEdge(massachusetts, rhodeIsland));
        assertTrue(graph.addEdge(massachusetts, vermont));
        assertTrue(graph.addEdge(michigan, ohio));
        assertTrue(graph.addEdge(michigan, wisconsin));
        assertTrue(graph.addEdge(minnesota, northDakota));
        assertTrue(graph.addEdge(minnesota, southDakota));
        assertTrue(graph.addEdge(minnesota, wisconsin));
        assertTrue(graph.addEdge(mississippi, tennessee));
        assertTrue(graph.addEdge(missouri, nebraska));
        assertTrue(graph.addEdge(missouri, oklahoma));
        assertTrue(graph.addEdge(missouri, tennessee));
        assertTrue(graph.addEdge(montana, northDakota));
        assertTrue(graph.addEdge(montana, southDakota));
        assertTrue(graph.addEdge(montana, wyoming));
        assertTrue(graph.addEdge(nebraska, southDakota));
        assertTrue(graph.addEdge(nebraska, wyoming));
        assertTrue(graph.addEdge(nevada, oregon));
        assertTrue(graph.addEdge(nevada, utah));
        assertTrue(graph.addEdge(newHampshire, vermont));
        assertTrue(graph.addEdge(newJersey, newYork));
        assertTrue(graph.addEdge(newJersey, pennsylvania));
        assertTrue(graph.addEdge(newMexico, oklahoma));
        assertTrue(graph.addEdge(newMexico, texas));
        assertTrue(graph.addEdge(newMexico, utah));
        assertTrue(graph.addEdge(newYork, pennsylvania));
        assertTrue(graph.addEdge(newYork, vermont));
        assertTrue(graph.addEdge(northCarolina, southCarolina));
        assertTrue(graph.addEdge(northCarolina, tennessee));
        assertTrue(graph.addEdge(northCarolina, virginia));
        assertTrue(graph.addEdge(northDakota, southDakota));
        assertTrue(graph.addEdge(ohio, pennsylvania));
        assertTrue(graph.addEdge(ohio, westVirginia));
        assertTrue(graph.addEdge(oklahoma, texas));
        assertTrue(graph.addEdge(oregon, washington));
        assertTrue(graph.addEdge(pennsylvania, westVirginia));
        assertTrue(graph.addEdge(southDakota, wyoming));
        assertTrue(graph.addEdge(tennessee, virginia));
        assertTrue(graph.addEdge(utah, wyoming));
        assertTrue(graph.addEdge(virginia, westVirginia));

        assertTrue(graph.connected(florida, washington));
        assertTrue(graph.connected(rhodeIsland, michigan));
        assertTrue(graph.connected(delaware, texas));
        assertFalse(graph.connected(alaska, newYork));
        assertFalse(graph.connected(hawaii, idaho));
        assertEquals(new SumAndMax(432, 1912), graph.getComponentAugmentation(newJersey));
        assertEquals(new SumAndMax(2, 1959), graph.getComponentAugmentation(hawaii));

        // 2186: Aliens attack, split nation in two using lasers
        assertTrue(graph.removeEdge(northDakota, minnesota));
        assertTrue(graph.removeEdge(southDakota, minnesota));
        assertTrue(graph.removeEdge(southDakota, iowa));
        assertTrue(graph.removeEdge(nebraska, iowa));
        assertTrue(graph.removeEdge(nebraska, missouri));
        assertTrue(graph.removeEdge(kansas, missouri));
        assertTrue(graph.removeEdge(oklahoma, missouri));
        assertTrue(graph.removeEdge(oklahoma, arkansas));
        assertTrue(graph.removeEdge(texas, arkansas));
        assertTrue(graph.connected(california, massachusetts));
        assertTrue(graph.connected(montana, virginia));
        assertTrue(graph.connected(idaho, southDakota));
        assertTrue(graph.connected(maine, tennessee));
        assertEquals(new SumAndMax(432, 1912), graph.getComponentAugmentation(vermont));
        assertTrue(graph.removeEdge(texas, louisiana));
        assertFalse(graph.connected(california, massachusetts));
        assertFalse(graph.connected(montana, virginia));
        assertTrue(graph.connected(idaho, southDakota));
        assertTrue(graph.connected(maine, tennessee));
        assertEquals(new SumAndMax(149, 1912), graph.getComponentAugmentation(wyoming));
        assertEquals(new SumAndMax(283, 1863), graph.getComponentAugmentation(vermont));

        // 2254: California breaks off into ocean, secedes
        assertTrue(graph.removeEdge(california, oregon));
        assertTrue(graph.removeEdge(california, nevada));
        assertTrue(graph.removeEdge(california, arizona));
        assertEquals(new SumAndMax(53, 1850), graph.removeVertexAugmentation(california));
        assertFalse(graph.connected(california, utah));
        assertFalse(graph.connected(california, oregon));
        assertNull(graph.getComponentAugmentation(california));
        assertEquals(new SumAndMax(96, 1912), graph.getComponentAugmentation(washington));
        assertEquals(new SumAndMax(283, 1863), graph.getComponentAugmentation(vermont));

        // 2367: Nuclear armageddon
        assertEquals(new SumAndMax(7, 1819), graph.removeVertexAugmentation(alabama));
        assertTrue(graph.removeEdge(alabama, florida));
        assertTrue(graph.removeEdge(alabama, georgia));
        assertTrue(graph.removeEdge(alabama, mississippi));
        assertTrue(graph.removeEdge(alabama, tennessee));
        assertEquals(new SumAndMax(1, 1959), graph.removeVertexAugmentation(alaska));
        assertEquals(new SumAndMax(9, 1912), graph.removeVertexAugmentation(arizona));
        assertTrue(graph.removeEdge(arizona, colorado));
        assertTrue(graph.removeEdge(arizona, nevada));
        assertTrue(graph.removeEdge(arizona, newMexico));
        assertTrue(graph.removeEdge(arizona, utah));
        assertEquals(new SumAndMax(4, 1836), graph.removeVertexAugmentation(arkansas));
        assertTrue(graph.removeEdge(arkansas, louisiana));
        assertTrue(graph.removeEdge(arkansas, mississippi));
        assertTrue(graph.removeEdge(arkansas, missouri));
        assertTrue(graph.removeEdge(arkansas, tennessee));
        assertEquals(new SumAndMax(7, 1876), graph.removeVertexAugmentation(colorado));
        assertTrue(graph.removeEdge(colorado, kansas));
        assertTrue(graph.removeEdge(colorado, nebraska));
        assertTrue(graph.removeEdge(colorado, newMexico));
        assertTrue(graph.removeEdge(colorado, oklahoma));
        assertTrue(graph.removeEdge(colorado, utah));
        assertTrue(graph.removeEdge(colorado, wyoming));
        assertEquals(new SumAndMax(5, 1788), graph.removeVertexAugmentation(connecticut));
        assertTrue(graph.removeEdge(connecticut, massachusetts));
        assertTrue(graph.removeEdge(connecticut, newYork));
        assertTrue(graph.removeEdge(connecticut, rhodeIsland));
        assertEquals(new SumAndMax(1, 1787), graph.removeVertexAugmentation(delaware));
        assertTrue(graph.removeEdge(delaware, maryland));
        assertTrue(graph.removeEdge(delaware, newJersey));
        assertTrue(graph.removeEdge(delaware, pennsylvania));
        assertEquals(new SumAndMax(27, 1845), graph.removeVertexAugmentation(florida));
        assertTrue(graph.removeEdge(florida, georgia));
        assertEquals(new SumAndMax(14, 1788), graph.removeVertexAugmentation(georgia));
        assertTrue(graph.removeEdge(georgia, northCarolina));
        assertTrue(graph.removeEdge(georgia, southCarolina));
        assertTrue(graph.removeEdge(georgia, tennessee));
        assertEquals(new SumAndMax(2, 1959), graph.removeVertexAugmentation(hawaii));
        assertEquals(new SumAndMax(2, 1890), graph.removeVertexAugmentation(idaho));
        assertTrue(graph.removeEdge(idaho, montana));
        assertTrue(graph.removeEdge(idaho, nevada));
        assertTrue(graph.removeEdge(idaho, oregon));
        assertTrue(graph.removeEdge(idaho, utah));
        assertTrue(graph.removeEdge(idaho, washington));
        assertTrue(graph.removeEdge(idaho, wyoming));
        assertEquals(new SumAndMax(18, 1818), graph.removeVertexAugmentation(illinois));
        assertTrue(graph.removeEdge(illinois, indiana));
        assertTrue(graph.removeEdge(illinois, iowa));
        assertTrue(graph.removeEdge(illinois, kentucky));
        assertTrue(graph.removeEdge(illinois, missouri));
        assertTrue(graph.removeEdge(illinois, wisconsin));
        assertEquals(new SumAndMax(9, 1816), graph.removeVertexAugmentation(indiana));
        assertTrue(graph.removeEdge(indiana, kentucky));
        assertTrue(graph.removeEdge(indiana, michigan));
        assertTrue(graph.removeEdge(indiana, ohio));
        assertEquals(new SumAndMax(4, 1846), graph.removeVertexAugmentation(iowa));
        assertTrue(graph.removeEdge(iowa, minnesota));
        assertTrue(graph.removeEdge(iowa, missouri));
        assertTrue(graph.removeEdge(iowa, wisconsin));
        assertEquals(new SumAndMax(4, 1861), graph.removeVertexAugmentation(kansas));
        assertTrue(graph.removeEdge(kansas, nebraska));
        assertTrue(graph.removeEdge(kansas, oklahoma));
        assertEquals(new SumAndMax(6, 1792), graph.removeVertexAugmentation(kentucky));
        assertTrue(graph.removeEdge(kentucky, missouri));
        assertTrue(graph.removeEdge(kentucky, ohio));
        assertTrue(graph.removeEdge(kentucky, tennessee));
        assertTrue(graph.removeEdge(kentucky, virginia));
        assertTrue(graph.removeEdge(kentucky, westVirginia));
        assertEquals(new SumAndMax(6, 1812), graph.removeVertexAugmentation(louisiana));
        assertTrue(graph.removeEdge(louisiana, mississippi));
        assertEquals(new SumAndMax(2, 1820), graph.removeVertexAugmentation(maine));
        assertTrue(graph.removeEdge(maine, newHampshire));
        assertEquals(new SumAndMax(8, 1788), graph.removeVertexAugmentation(maryland));
        assertTrue(graph.removeEdge(maryland, pennsylvania));
        assertTrue(graph.removeEdge(maryland, virginia));
        assertTrue(graph.removeEdge(maryland, westVirginia));
        assertEquals(new SumAndMax(9, 1788), graph.removeVertexAugmentation(massachusetts));
        assertTrue(graph.removeEdge(massachusetts, newHampshire));
        assertTrue(graph.removeEdge(massachusetts, newYork));
        assertTrue(graph.removeEdge(massachusetts, rhodeIsland));
        assertTrue(graph.removeEdge(massachusetts, vermont));
        assertEquals(new SumAndMax(14, 1837), graph.removeVertexAugmentation(michigan));
        assertTrue(graph.removeEdge(michigan, ohio));
        assertTrue(graph.removeEdge(michigan, wisconsin));
        assertEquals(new SumAndMax(8, 1858), graph.removeVertexAugmentation(minnesota));
        assertTrue(graph.removeEdge(minnesota, wisconsin));
        assertEquals(new SumAndMax(4, 1817), graph.removeVertexAugmentation(mississippi));
        assertTrue(graph.removeEdge(mississippi, tennessee));
        assertEquals(new SumAndMax(8, 1821), graph.removeVertexAugmentation(missouri));
        assertTrue(graph.removeEdge(missouri, tennessee));
        assertEquals(new SumAndMax(1, 1889), graph.removeVertexAugmentation(montana));
        assertTrue(graph.removeEdge(montana, northDakota));
        assertTrue(graph.removeEdge(montana, southDakota));
        assertTrue(graph.removeEdge(montana, wyoming));
        assertEquals(new SumAndMax(3, 1867), graph.removeVertexAugmentation(nebraska));
        assertTrue(graph.removeEdge(nebraska, southDakota));
        assertTrue(graph.removeEdge(nebraska, wyoming));
        assertEquals(new SumAndMax(4, 1864), graph.removeVertexAugmentation(nevada));
        assertTrue(graph.removeEdge(nevada, oregon));
        assertTrue(graph.removeEdge(nevada, utah));
        assertEquals(new SumAndMax(2, 1788), graph.removeVertexAugmentation(newHampshire));
        assertTrue(graph.removeEdge(newHampshire, vermont));
        assertEquals(new SumAndMax(12, 1787), graph.removeVertexAugmentation(newJersey));
        assertTrue(graph.removeEdge(newJersey, newYork));
        assertTrue(graph.removeEdge(newJersey, pennsylvania));
        assertEquals(new SumAndMax(3, 1912), graph.removeVertexAugmentation(newMexico));
        assertTrue(graph.removeEdge(newMexico, oklahoma));
        assertTrue(graph.removeEdge(newMexico, texas));
        assertTrue(graph.removeEdge(newMexico, utah));
        assertEquals(new SumAndMax(27, 1788), graph.removeVertexAugmentation(newYork));
        assertTrue(graph.removeEdge(newYork, pennsylvania));
        assertTrue(graph.removeEdge(newYork, vermont));
        assertEquals(new SumAndMax(13, 1789), graph.removeVertexAugmentation(northCarolina));
        assertTrue(graph.removeEdge(northCarolina, southCarolina));
        assertTrue(graph.removeEdge(northCarolina, tennessee));
        assertTrue(graph.removeEdge(northCarolina, virginia));
        assertEquals(new SumAndMax(1, 1889), graph.removeVertexAugmentation(northDakota));
        assertTrue(graph.removeEdge(northDakota, southDakota));
        assertEquals(new SumAndMax(16, 1803), graph.removeVertexAugmentation(ohio));
        assertTrue(graph.removeEdge(ohio, pennsylvania));
        assertTrue(graph.removeEdge(ohio, westVirginia));
        assertEquals(new SumAndMax(5, 1907), graph.removeVertexAugmentation(oklahoma));
        assertTrue(graph.removeEdge(oklahoma, texas));
        assertEquals(new SumAndMax(5, 1859), graph.removeVertexAugmentation(oregon));
        assertTrue(graph.removeEdge(oregon, washington));
        assertEquals(new SumAndMax(18, 1787), graph.removeVertexAugmentation(pennsylvania));
        assertTrue(graph.removeEdge(pennsylvania, westVirginia));
        assertEquals(new SumAndMax(2, 1790), graph.removeVertexAugmentation(rhodeIsland));
        assertEquals(new SumAndMax(7, 1788), graph.removeVertexAugmentation(southCarolina));
        assertEquals(new SumAndMax(1, 1889), graph.removeVertexAugmentation(southDakota));
        assertTrue(graph.removeEdge(southDakota, wyoming));
        assertEquals(new SumAndMax(9, 1796), graph.removeVertexAugmentation(tennessee));
        assertTrue(graph.removeEdge(tennessee, virginia));
        assertEquals(new SumAndMax(36, 1845), graph.removeVertexAugmentation(texas));
        assertEquals(new SumAndMax(4, 1896), graph.removeVertexAugmentation(utah));
        assertTrue(graph.removeEdge(utah, wyoming));
        assertEquals(new SumAndMax(1, 1791), graph.removeVertexAugmentation(vermont));
        assertEquals(new SumAndMax(11, 1788), graph.removeVertexAugmentation(virginia));
        assertTrue(graph.removeEdge(virginia, westVirginia));
        assertEquals(new SumAndMax(10, 1889), graph.removeVertexAugmentation(washington));
        assertEquals(new SumAndMax(3, 1863), graph.removeVertexAugmentation(westVirginia));
        assertEquals(new SumAndMax(8, 1848), graph.removeVertexAugmentation(wisconsin));
        assertEquals(new SumAndMax(1, 1890), graph.removeVertexAugmentation(wyoming));

        assertFalse(graph.connected(georgia, newMexico));
        assertFalse(graph.connected(wisconsin, michigan));
        assertFalse(graph.connected(ohio, kentucky));
        assertFalse(graph.connected(alaska, connecticut));
        assertNull(graph.getComponentAugmentation(southDakota));
        assertNull(graph.getComponentAugmentation(arkansas));
    }

    /**
     * Tests a graph based on the United States.
     */
    @Test
    public void testUnitedStates() {
        checkUnitedStates(new ConnGraph(SumAndMax.AUGMENTATION));
        checkUnitedStates(new ConnGraph(SumAndMax.MUTATING_AUGMENTATION));
    }

    /**
     * Tests ConnectivityGraph on the graph for a dodecahedron.
     */
    @Test
    public void testDodecahedron() {
        ConnGraph graph = new ConnGraph();
        Random random = new Random(6170);
        ConnVertex vertex1 = new ConnVertex(random);
        ConnVertex vertex2 = new ConnVertex(random);
        ConnVertex vertex3 = new ConnVertex(random);
        ConnVertex vertex4 = new ConnVertex(random);
        ConnVertex vertex5 = new ConnVertex(random);
        ConnVertex vertex6 = new ConnVertex(random);
        ConnVertex vertex7 = new ConnVertex(random);
        ConnVertex vertex8 = new ConnVertex(random);
        ConnVertex vertex9 = new ConnVertex(random);
        ConnVertex vertex10 = new ConnVertex(random);
        ConnVertex vertex11 = new ConnVertex(random);
        ConnVertex vertex12 = new ConnVertex(random);
        ConnVertex vertex13 = new ConnVertex(random);
        ConnVertex vertex14 = new ConnVertex(random);
        ConnVertex vertex15 = new ConnVertex(random);
        ConnVertex vertex16 = new ConnVertex(random);
        ConnVertex vertex17 = new ConnVertex(random);
        ConnVertex vertex18 = new ConnVertex(random);
        ConnVertex vertex19 = new ConnVertex(random);
        ConnVertex vertex20 = new ConnVertex(random);

        assertTrue(graph.addEdge(vertex1, vertex2));
        assertTrue(graph.addEdge(vertex1, vertex5));
        assertTrue(graph.addEdge(vertex1, vertex6));
        assertTrue(graph.addEdge(vertex2, vertex3));
        assertTrue(graph.addEdge(vertex2, vertex8));
        assertTrue(graph.addEdge(vertex3, vertex4));
        assertTrue(graph.addEdge(vertex3, vertex10));
        assertTrue(graph.addEdge(vertex4, vertex5));
        assertTrue(graph.addEdge(vertex4, vertex12));
        assertTrue(graph.addEdge(vertex5, vertex14));
        assertTrue(graph.addEdge(vertex6, vertex7));
        assertTrue(graph.addEdge(vertex6, vertex15));
        assertTrue(graph.addEdge(vertex7, vertex8));
        assertTrue(graph.addEdge(vertex7, vertex16));
        assertTrue(graph.addEdge(vertex8, vertex9));
        assertTrue(graph.addEdge(vertex9, vertex10));
        assertTrue(graph.addEdge(vertex9, vertex17));
        assertTrue(graph.addEdge(vertex10, vertex11));
        assertTrue(graph.addEdge(vertex11, vertex12));
        assertTrue(graph.addEdge(vertex11, vertex18));
        assertTrue(graph.addEdge(vertex12, vertex13));
        assertTrue(graph.addEdge(vertex13, vertex14));
        assertTrue(graph.addEdge(vertex13, vertex19));
        assertTrue(graph.addEdge(vertex14, vertex15));
        assertTrue(graph.addEdge(vertex15, vertex20));
        assertTrue(graph.addEdge(vertex16, vertex17));
        assertTrue(graph.addEdge(vertex16, vertex20));
        assertTrue(graph.addEdge(vertex17, vertex18));
        assertTrue(graph.addEdge(vertex18, vertex19));
        assertTrue(graph.addEdge(vertex19, vertex20));
        graph.optimize();

        assertTrue(graph.connected(vertex1, vertex17));
        assertTrue(graph.connected(vertex7, vertex15));

        assertTrue(graph.removeEdge(vertex5, vertex14));
        assertTrue(graph.removeEdge(vertex6, vertex15));
        assertTrue(graph.removeEdge(vertex7, vertex16));
        assertTrue(graph.removeEdge(vertex12, vertex13));
        assertTrue(graph.removeEdge(vertex16, vertex17));
        assertTrue(graph.connected(vertex1, vertex14));
        assertTrue(graph.connected(vertex4, vertex20));
        assertTrue(graph.connected(vertex14, vertex16));

        assertTrue(graph.removeEdge(vertex18, vertex19));
        assertFalse(graph.connected(vertex1, vertex14));
        assertFalse(graph.connected(vertex4, vertex20));
        assertTrue(graph.connected(vertex14, vertex16));

        graph.clear();
        graph.optimize();
        assertTrue(graph.connected(vertex7, vertex7));
        assertFalse(graph.connected(vertex1, vertex2));
    }

    /**
     * Tests the zero-argument ConnVertex constructor.
     */
    @Test
    public void testDefaultConnVertexConstructor() {
        ConnGraph graph = new ConnGraph();
        ConnVertex vertex1 = new ConnVertex();
        ConnVertex vertex2 = new ConnVertex();
        ConnVertex vertex3 = new ConnVertex();
        ConnVertex vertex4 = new ConnVertex();
        ConnVertex vertex5 = new ConnVertex();
        ConnVertex vertex6 = new ConnVertex();
        assertTrue(graph.addEdge(vertex1, vertex2));
        assertTrue(graph.addEdge(vertex2, vertex3));
        assertTrue(graph.addEdge(vertex1, vertex3));
        assertTrue(graph.addEdge(vertex4, vertex5));
        assertTrue(graph.connected(vertex1, vertex3));
        assertTrue(graph.connected(vertex4, vertex5));
        assertFalse(graph.connected(vertex1, vertex4));

        graph.optimize();
        assertTrue(graph.removeEdge(vertex1, vertex3));
        assertTrue(graph.connected(vertex1, vertex3));
        assertTrue(graph.connected(vertex4, vertex5));
        assertFalse(graph.connected(vertex1, vertex4));
        assertTrue(graph.removeEdge(vertex1, vertex2));
        assertFalse(graph.connected(vertex1, vertex3));
        assertTrue(graph.connected(vertex4, vertex5));
        assertFalse(graph.connected(vertex1, vertex4));

        //assertEquals(Collections.singleton(vertex3), new HashSet<ConnVertex>(graph.adjacentVertices(vertex2)));
        //assertTrue(graph.adjacentVertices(vertex1).isEmpty());
        //assertTrue(graph.adjacentVertices(vertex6).isEmpty());
    }
}
