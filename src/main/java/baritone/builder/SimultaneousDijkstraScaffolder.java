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
import baritone.builder.DependencyGraphScaffoldingOverlay.CollapsedDependencyGraph.CollapsedDependencyGraphComponent;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class SimultaneousDijkstraScaffolder implements IScaffolderStrategy {
    @Override
    public LongList scaffoldTo(CollapsedDependencyGraphComponent $ignored$, DependencyGraphScaffoldingOverlay overlayGraph) {
        // the use-case that i'm keeping mind from a performance pov is staircased mapart
        // O(n^2) across all 128x128=16384 blocks would be cringe
        // so let's have a combined priority queue from all cdg provenances without a reset on every scaffolding placement
        List<CollapsedDependencyGraphComponent> roots = overlayGraph.getCollapsedGraph().getComponents().values().stream().filter(component -> component.getIncoming().isEmpty()).collect(Collectors.toList());
        ObjectOpenHashSet<CollapsedDependencyGraphComponent> remainingRoots = new ObjectOpenHashSet<>();
        remainingRoots.addAll(roots);
        Object2IntOpenHashMap<CollapsedDependencyGraphComponent> componentToId = new Object2IntOpenHashMap<>();
        for (int i = 0; i < roots.size(); i++) {
            componentToId.put(roots.get(i), i);
        }
        PriorityQueue<ScaffoldingSearchNode> openSet = new PriorityQueue<>(Comparator.<ScaffoldingSearchNode>comparingInt(node -> node.costSoFar).thenComparingInt(node -> node.key.cdgid));
        Object2ObjectOpenHashMap<ScaffoldingSearchKey, ScaffoldingSearchNode> nodeMap = new Object2ObjectOpenHashMap<>();
        for (CollapsedDependencyGraphComponent component : roots) {
            int cdgid = componentToId.getInt(component);
            LongIterator it = component.getPositions().iterator();
            while (it.hasNext()) {
                long l = it.nextLong();
                ScaffoldingSearchKey key = new ScaffoldingSearchKey(l, cdgid);
                nodeMap.put(key, new ScaffoldingSearchNode(key));
            }
        }
        openSet.addAll(nodeMap.values());
        while (!openSet.isEmpty()) {
            ScaffoldingSearchNode node = openSet.poll();
            CollapsedDependencyGraphComponent provenance = roots.get(node.key.cdgid).deletedIntoRecursive();
            // is the deletedIntoRecursive valid? i think so because the costSoFar is a shared key in the priority queue. sure you could get a suboptimal path from an old subsection of a new cdg, but, it would only be considered after the optimal path. so, no issue? i think?
            if (!provenance.getIncoming().isEmpty()) {
                continue; // node originated from a cdg that has been scaffolded making it no longer a root
            }
            CollapsedDependencyGraphComponent tentativeComponent = overlayGraph.getCollapsedGraph().getComponentLocations().get(node.key.pos);
            if (tentativeComponent != provenance) {
                // TODO eventually figure out the situation with exclusiveDescendents like in the sequential dijkstra scaffolder
                LongList toActivate = reconstructPathTo(node);
                // have to do this bs because the scaffolding route can touch a third component even if only one scaffolding block is added
                long[] allNearby = toActivate.stream().flatMapToLong(pos -> LongStream.of(OFFS_INCL_ZERO).map(off -> (off + pos) & BetterBlockPos.POST_ADDITION_MASK)).toArray();
                // have to check before applying scaffolding because getComponentLocations will return the new component and not the deleted root if we did it after
                Set<CollapsedDependencyGraphComponent> toCheck = LongStream.of(allNearby).mapToObj(overlayGraph.getCollapsedGraph().getComponentLocations()::get).collect(Collectors.toCollection(ObjectOpenHashSet::new));
                Scaffolder.applyScaffoldingConnection(overlayGraph, toActivate);
                // have to check this again because new scaffolding can make its own collapsed node
                // for example if there are two individual blocks separated by a knight's move in strict_y mode, meaning there are two new scaffolding blocks added at a certain y, connecting to one block at that y and another at a higher y, then the two new scaffolding can form a larger collapsed node, causing the previous block to be merged into it
                // in short, it's possible for a new root to be created
                toCheck.addAll(LongStream.of(allNearby).mapToObj(overlayGraph.getCollapsedGraph().getComponentLocations()::get).collect(Collectors.toSet()));
                int sz = remainingRoots.size();
                for (CollapsedDependencyGraphComponent component : toCheck) {
                    if (component.deleted()) {
                        remainingRoots.remove(component);
                    }
                }
                if (remainingRoots.size() >= sz) {
                    throw new IllegalStateException();
                }
                for (long pos : toActivate) { // im being lazy, this boxes to Long i think
                    CollapsedDependencyGraphComponent comp = overlayGraph.getCollapsedGraph().getComponentLocations().get(pos);
                    if (comp.getIncoming().isEmpty()) {
                        if (remainingRoots.add(comp)) {
                            int cdgid = roots.size();
                            roots.add(comp);
                            componentToId.put(comp, cdgid);
                        }
                        ScaffoldingSearchNode newNode = new ScaffoldingSearchNode(new ScaffoldingSearchKey(pos, componentToId.getInt(comp)));
                        nodeMap.put(newNode.key, newNode);
                        openSet.add(newNode);
                    }
                }
                if (remainingRoots.size() == 1) {
                    return null;
                }
                if (remainingRoots.isEmpty()) {
                    throw new IllegalStateException();
                }
            }
            for (Face face : Face.VALUES) {
                int newCost = node.costSoFar + DijkstraScaffolder.edgeCost(face);
                if (overlayGraph.hypotheticalScaffoldingIncomingEdge(node.key.pos, face)) {
                    long neighborPos = face.offset(node.key.pos);
                    ScaffoldingSearchKey neighborKey = new ScaffoldingSearchKey(neighborPos, node.key.cdgid);
                    ScaffoldingSearchNode existingNode = nodeMap.get(neighborKey);
                    if (existingNode != null) {
                        if (existingNode.costSoFar > newCost) {
                            throw new IllegalStateException();
                        }
                        continue;
                    }
                    ScaffoldingSearchNode newNode = new ScaffoldingSearchNode(neighborKey);
                    newNode.costSoFar = newCost;
                    newNode.prev = node;
                    nodeMap.put(newNode.key, newNode);
                    openSet.add(newNode);
                }
            }
        }
        throw new UnsupportedOperationException();
    }

    private static class ScaffoldingSearchKey {
        long pos;
        int cdgid;

        public ScaffoldingSearchKey(long pos, int cdgid) {
            this.pos = pos;
            this.cdgid = cdgid;
        }

        @Override
        public boolean equals(Object o) {
            //if (this == o) return true;
            //if (!(o instanceof ScaffoldingSearchKey)) return false;
            ScaffoldingSearchKey that = (ScaffoldingSearchKey) o;
            return pos == that.pos && cdgid == that.cdgid;
        }

        @Override
        public int hashCode() {
            return HashCommon.murmurHash3(cdgid) + (int) HashCommon.murmurHash3(pos);
        }
    }

    private static class ScaffoldingSearchNode {

        private final ScaffoldingSearchKey key;
        private int costSoFar;
        private ScaffoldingSearchNode prev;

        private ScaffoldingSearchNode(ScaffoldingSearchKey key) {
            this.key = key;
        }
    }

    private static LongList reconstructPathTo(ScaffoldingSearchNode end) {
        LongList path = new LongArrayList();
        while (end != null) {
            path.add(end.key.pos);
            end = end.prev;
        }
        return path;
    }

    private static final long[] OFFS_INCL_ZERO = new long[Face.NUM_FACES + 1];

    static {
        for (int i = 0; i < Face.NUM_FACES; i++) {
            OFFS_INCL_ZERO[i] = Face.VALUES[i].offset;
        }
    }
}
