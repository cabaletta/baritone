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

import baritone.builder.DependencyGraphScaffoldingOverlay.CollapsedDependencyGraph;
import baritone.builder.DependencyGraphScaffoldingOverlay.CollapsedDependencyGraph.CollapsedDependencyGraphComponent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Given a DependencyGraphScaffoldingOverlay, put in scaffolding blocks until the entire graph is navigable from the root.
 * <p>
 * In other words, add scaffolding blocks to the schematic until the entire thing can theoretically be built from one
 * starting point, just by placing blocks against blocks. So like, anything floating in the air will get a connector down to the
 * ground (or over to something that's eventually connected to the ground). After this is done, nothing will be left floating in
 * midair with no connection to the rest of the build.
 */
public class Scaffolder {

    private final DependencyGraphScaffoldingOverlay overlayGraph;
    // NOTE: these next three fields are updated in-place as the overlayGraph is updated :)
    private final CollapsedDependencyGraph collapsedGraph;
    private final Int2ObjectMap<CollapsedDependencyGraphComponent> components;
    private final Long2ObjectMap<CollapsedDependencyGraphComponent> componentLocations;

    private final List<CollapsedDependencyGraphComponent> rootComponents;

    private Scaffolder(DependencyGraphScaffoldingOverlay overlayGraph) {
        this.overlayGraph = overlayGraph;
        this.collapsedGraph = overlayGraph.getCollapsedGraph();
        this.components = collapsedGraph.getComponents();
        this.componentLocations = collapsedGraph.getComponentLocations();

        // since the components form a DAG (because all strongly connected components, and therefore all cycles, have been collapsed)
        // we can locate all root components by simply finding the ones with no incoming edges
        this.rootComponents = components
                .values()
                .stream()
                .filter(component -> component.getIncoming().isEmpty())
                .collect(Collectors.toCollection(ArrayList::new)); // ensure arraylist since we will be mutating the list
    }

    private void loop() {
        CollapsedDependencyGraphComponent root = rootComponents.remove(rootComponents.size() - 1);
        if (!root.getIncoming().isEmpty()) {
            throw new IllegalStateException();
        }
        ScaffoldingSearchNode end = dijkstra(root);
        List<ScaffoldingSearchNode> path = new ArrayList<>();
        while (end != null) {
            path.add(end);
            end = end.prev;
        }
        if (!root.getPositions().contains(path.get(path.size() - 1).pos)) {
            throw new IllegalStateException();
        }
        if (!componentLocations.containsKey(path.get(0).pos)) {
            throw new IllegalStateException();
        }
        for (int i = 1; i < path.size() - 1; i++) {
            if (componentLocations.containsKey(path.get(i).pos)) {
                throw new IllegalStateException();
            }
        }

    }

    private void walkAllDescendents(CollapsedDependencyGraphComponent root, Set<CollapsedDependencyGraphComponent> set) { // TODO this allocs due to the unmodifiable, also in practice this is like O(n^2) as the scaffolding proceeds downwards...
        set.add(root);
        for (CollapsedDependencyGraphComponent component : root.getOutgoing()) {
            walkAllDescendents(component, set);
        }
    }

    private ScaffoldingSearchNode dijkstra(CollapsedDependencyGraphComponent root) {
        Set<CollapsedDependencyGraphComponent> descendents = new ObjectOpenHashSet<>();
        walkAllDescendents(root, descendents);
        PriorityQueue<ScaffoldingSearchNode> openSet = new PriorityQueue<>(Comparator.comparingInt(node -> node.costSoFar));
        Long2ObjectOpenHashMap<ScaffoldingSearchNode> nodeMap = new Long2ObjectOpenHashMap<>();
        LongIterator it = root.getPositions().iterator();
        while (it.hasNext()) {
            long l = it.nextLong();
            nodeMap.put(l, new ScaffoldingSearchNode(l));
        }
        openSet.addAll(nodeMap.values());
        while (!openSet.isEmpty()) {
            ScaffoldingSearchNode node = openSet.poll();
            CollapsedDependencyGraphComponent tentativeComponent = componentLocations.get(node.pos);
            if (tentativeComponent != null) {
                if (descendents.contains(tentativeComponent)) {
                    // have gone back onto a descendent of this node
                    // sadly this can happen even at the same Y level even in Y_STRICT mode due to orientable blocks forming a loop
                    continue; // TODO does this need to be here? can I expand THROUGH an unrelated component? probably requires testing, this is quite a mind bending possibility
                } else {
                    return node; // all done! found a path to a component unrelated to this one, meaning we have successfully connected with scaffolding this part of the build back to the rest of it
                }
            }
            for (Face face : Face.VALUES) {
                if (overlayGraph.hypotheticalScaffoldingIncomingEdge(node.pos, face)) { // we don't have to worry about an incoming edge going into the frontier set because the root component is strongly connected and has no incoming edges from other SCCs, therefore any and all incoming edges will come from hypothetical scaffolding air locations
                    long neighborPos = face.offset(node.pos);
                    int newCost = node.costSoFar + edgeCost(face); // TODO future edge cost should include an added modifier for if neighborPos is in a favorable or unfavorable position e.g. above / under a diagonal depending on if map art or not
                    ScaffoldingSearchNode existingNode = nodeMap.get(neighborPos);
                    if (existingNode != null) {
                        // it's okay if neighbor isn't marked as "air" in the overlay - that's what we want to find - a path to another component
                        // however, we can't consider neighbors within the same component as a solution, clearly
                        // we can accomplish this and kill two birds with one stone by skipping all nodes already in the node map
                        // any position in the initial frontier is clearly in the node map, but also any node that has already been considered
                        // this prevents useless cycling of equivalent paths
                        // this is okay because our search has no heuristic so this is a uniform cost search
                        if (existingNode.costSoFar < newCost) {
                            throw new IllegalStateException();
                        }
                        continue; // nothing to do - we already have an equal-or-better path to this location
                    }
                    ScaffoldingSearchNode newNode = new ScaffoldingSearchNode(neighborPos);
                    newNode.costSoFar = newCost;
                    newNode.prev = node;
                    nodeMap.put(newNode.pos, newNode);
                    openSet.add(newNode);
                }
            }
        }
        return null;
    }

    private int edgeCost(Face face) {
        if (Main.STRICT_Y && face == Face.UP) {
            throw new IllegalStateException();
        }
        // gut feeling: give slight bias to moving horizontally
        // that will influence it to create horizontal bridges more often than vertical pillars
        // horizontal bridges are easier to maneuver around and over
        if (face.y == 0) {
            return 1;
        }
        return 2;
    }

    private class ScaffoldingSearchNode {

        private final long pos;
        private int costSoFar;
        private ScaffoldingSearchNode prev;

        private ScaffoldingSearchNode(long pos) {
            this.pos = pos;
        }
    }

    private void sanityCheck() {
        // we will trust DependencyGraphScaffoldingOverlay that there are no cycles of any kind in the components - they form a DAG
        //
    }

    private void enableBlock(long pos) {
        // first, before everything gets destroyed by updating the overlay graph, let's chill out our subgraphs
        // i really would rather not write a whole new thing for incrementally recomputing an overlay graph!

    }

    public static void run(DependencyGraphScaffoldingOverlay overlay) {


        CollapsedDependencyGraph collapsed = overlay.getCollapsedGraph();
        Map<Integer, CollapsedDependencyGraphComponent> components = collapsed.getComponents();

    }
}
