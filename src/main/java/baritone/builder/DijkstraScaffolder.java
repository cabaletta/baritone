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

import baritone.builder.DependencyGraphScaffoldingOverlay.CollapsedDependencyGraph.CollapsedDependencyGraphComponent;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

public enum DijkstraScaffolder implements IScaffolderStrategy {
    INSTANCE;

    @Override
    public LongList scaffoldTo(CollapsedDependencyGraphComponent root, DependencyGraphScaffoldingOverlay overlayGraph) {
        // TODO what if this root is unreachable, e.g. it's lower in STRICT_Y mode?
        Set<CollapsedDependencyGraphComponent> exclusiveDescendents = new ObjectOpenHashSet<>();
        walkAllDescendents(root, exclusiveDescendents);
        exclusiveDescendents.remove(root);
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
            CollapsedDependencyGraphComponent tentativeComponent = overlayGraph.getCollapsedGraph().getComponentLocations().get(node.pos);
            if (tentativeComponent != null) {
                if (exclusiveDescendents.contains(tentativeComponent)) { // TODO is exclusiveDescendants even valid? returning a route into one of the descendants, if it's on the top of the heap, is valid because it closes a loop and the next dijkstra can start from there? perhaps there's no need to treat descendant interactions differently from any other non-root component?
                    // have gone back onto a descendent of this node
                    // sadly this can happen even at the same Y level even in Y_STRICT mode due to orientable blocks forming a loop
                    continue; // TODO does this need to be here? can I expand THROUGH an unrelated component? probably requires testing, this is quite a mind bending possibility
                } else {
                    // found a path to a component that isn't a descendent of the root
                    if (tentativeComponent != root) { // but if it IS the root, then we're just on our first loop iteration, we are far from done
                        return reconstructPathTo(node); // all done! found a path to a component unrelated to this one, meaning we have successfully connected this part of the build with scaffolding back to the rest of it
                        // TODO scaffolder strategy should be reworked into a coroutine-like format to decomposes a persistent dijkstra that retains the openset and nodemap between scaffolder component connections. each scaffoldersearchnode would need a persistent progeny (source component) and new combined components would need to be introduced as they're created. then the search can be simultaneous. this would solve the problem of potential incorrect selection of root node, as all possible root nodes are expanded at once
                    }
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
                        // this is okay because all paths are equivalent, so there is no possible way to find a better path (because currently it's a fixed value for horizontal / vertical movements)
                        if (existingNode.costSoFar != newCost && !root.getPositions().contains(neighborPos)) { // initialization nodes will have costSoFar = 0 as a base case
                            throw new IllegalStateException();
                        }
                        // TODO if root spans more than 1 y level, then this assumption is not correct because edgeCost is different for a horizontal vs vertical face, meaning that a neighbor can have different cost routes if both sideways and up are part of the root component
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

    private static void walkAllDescendents(CollapsedDependencyGraphComponent root, Set<CollapsedDependencyGraphComponent> set) {
        set.add(root);
        for (CollapsedDependencyGraphComponent component : root.getOutgoing()) {
            walkAllDescendents(component, set);
        }
    }

    private static LongList reconstructPathTo(ScaffoldingSearchNode end) {
        LongList path = new LongArrayList();
        while (end != null) {
            path.add(end.pos);
            end = end.prev;
        }
        return path;
    }

    private static int edgeCost(Face face) {
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

    private static class ScaffoldingSearchNode {

        private final long pos;
        private int costSoFar;
        private ScaffoldingSearchNode prev;

        private ScaffoldingSearchNode(long pos) {
            this.pos = pos;
        }
    }
}
