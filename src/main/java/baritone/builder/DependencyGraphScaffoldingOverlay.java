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
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

/**
 * A mutable addition of scaffolding blocks to a schematic
 * <p>
 * Contains a set of coordinates that are "air" in the schematic, but we are going to put scaffolding throwaway blocks there
 * <p>
 * Maintains and incrementally updates a collapsed dependency graph, which is the block placement dependency graph reduced to directed acyclic graph form by way of collapsing all strongly connected components into single nodes
 */
public class DependencyGraphScaffoldingOverlay {

    private final PlaceOrderDependencyGraph delegate;
    private final LongOpenHashSet scaffoldingAdded;
    private final CollapsedDependencyGraph collapsedGraph;

    public DependencyGraphScaffoldingOverlay(PlaceOrderDependencyGraph delegate) {
        this.delegate = delegate;
        this.scaffoldingAdded = new LongOpenHashSet();
        this.collapsedGraph = new CollapsedDependencyGraph(TarjansAlgorithm.run(this));
        //System.out.println("Num components: " + collapsedGraph.components.size());
    }

    public boolean outgoingEdge(long pos, Face face) {
        if (overrideOff(pos)) {
            return false;
        }
        if (overrideOff(face.offset(pos))) {
            return false;
        }
        return delegate.outgoingEdge(pos, face);
    }

    public boolean incomingEdge(long pos, Face face) {
        if (overrideOff(pos)) {
            return false;
        }
        if (overrideOff(face.offset(pos))) {
            return false;
        }
        return delegate.incomingEdge(pos, face);
    }

    public boolean hypotheticalScaffoldingIncomingEdge(long pos, Face face) {
        return delegate.incomingEdge(pos, face);
    }

    public CuboidBounds bounds() {
        return delegate.bounds();
    }

    private boolean overrideOff(long pos) {
        return bounds().inRangePos(pos) && air(pos);
    }

    public boolean real(long pos) {
        return !air(pos);
    }

    public void forEachReal(CuboidBounds.CuboidIndexConsumer consumer) {
        bounds().forEach(pos -> {
            if (real(pos)) {
                consumer.consume(pos);
            }
        });
    }

    public boolean air(long pos) {
        return delegate.airTreatedAsScaffolding(pos) && !scaffoldingAdded.contains(pos);
    }

    public void enable(long pos) {
        if (!delegate.airTreatedAsScaffolding(pos)) {
            throw new IllegalArgumentException();
        }
        if (!scaffoldingAdded.add(pos)) {
            throw new IllegalArgumentException();
        }
        collapsedGraph.incrementalUpdate(pos);
        if (Main.SLOW_DEBUG) {
            //System.out.println(collapsedGraph.posToComponent.containsKey(pos) + " " + scaffoldingAdded.contains(pos) + " " + real(pos));
            checkEquality(collapsedGraph, new CollapsedDependencyGraph(TarjansAlgorithm.run(this)));
            //System.out.println("Checked equality");
            //System.out.println("Num connected components: " + collapsedGraph.components.size());
        }
    }

    public BlockStateCachedData data(long pos) {
        if (Main.DEBUG && !real(pos)) {
            throw new IllegalStateException();
        }
        return delegate.data(pos);
    }

    /**
     * Remember that this returns a collapsed graph that will be updated in-place as positions are enabled. It does not return a copy.
     */
    public CollapsedDependencyGraph getCollapsedGraph() {
        return collapsedGraph;
    }

    public class CollapsedDependencyGraph {

        private int nextComponentID;
        private final Int2ObjectOpenHashMap<CollapsedDependencyGraphComponent> components;
        private final Long2ObjectOpenHashMap<CollapsedDependencyGraphComponent> posToComponent;

        private CollapsedDependencyGraph(TarjansAlgorithm.TarjansResult partition) {
            components = new Int2ObjectOpenHashMap<>();
            for (int i = 0; i < partition.numComponents(); i++) {
                addComponent();
            }
            posToComponent = new Long2ObjectOpenHashMap<>();
            forEachReal(pos -> {
                CollapsedDependencyGraphComponent component = components.get(partition.getComponent(pos));
                component.positions.add(pos);
                posToComponent.put(pos, component);
            });
            forEachReal(pos -> {
                for (Face face : Face.VALUES) {
                    if (outgoingEdge(pos, face)) {
                        CollapsedDependencyGraphComponent src = posToComponent.get(pos);
                        CollapsedDependencyGraphComponent dst = posToComponent.get(face.offset(pos));
                        if (src != dst) {
                            src.outgoingEdges.add(dst);
                            dst.incomingEdges.add(src);
                        }
                    }
                }
            });
            if (Main.DEBUG) {
                sanityCheck();
            }
        }

        public Int2ObjectMap<CollapsedDependencyGraphComponent> getComponents() {
            return Int2ObjectMaps.unmodifiable(components);
        }

        public Long2ObjectMap<CollapsedDependencyGraphComponent> getComponentLocations() {
            return Long2ObjectMaps.unmodifiable(posToComponent);
        }

        public OptionalInt lastComponentID() {
            return nextComponentID == 0 ? OptionalInt.empty() : OptionalInt.of(nextComponentID - 1);
        }

        private CollapsedDependencyGraphComponent addComponent() {
            CollapsedDependencyGraphComponent component = new CollapsedDependencyGraphComponent(nextComponentID);
            components.put(component.id, component);
            nextComponentID++;
            return component;
        }

        private CollapsedDependencyGraphComponent mergeInto(CollapsedDependencyGraphComponent child, CollapsedDependencyGraphComponent parent) {
            if (child.deleted() || parent.deleted()) {
                throw new IllegalStateException();
            }
            if (child == parent) {
                throw new IllegalStateException();
            }
            if (child.positions.size() > parent.positions.size()) {
                return mergeInto(parent, child);
            }
            if (Main.DEBUG) {
                //System.out.println("Merging " + child.index + " into " + parent.index);
            }
            child.incomingEdges.forEach(intoChild -> {
                intoChild.outgoingEdges.remove(child);
                if (intoChild == parent) {
                    return;
                }
                intoChild.outgoingEdges.add(parent);
                parent.incomingEdges.add(intoChild);
            });
            child.outgoingEdges.forEach(outOfChild -> {
                outOfChild.incomingEdges.remove(child);
                if (outOfChild == parent) {
                    return;
                }
                outOfChild.incomingEdges.add(parent);
                parent.outgoingEdges.add(outOfChild);
            });
            parent.positions.addAll(child.positions);
            LongIterator it = child.positions.iterator();
            while (it.hasNext()) {
                long pos = it.nextLong();
                posToComponent.put(pos, parent);
            }
            components.remove(child.id);
            child.deleted = true;
            //System.out.println("Debug child contains: " + child.positions.contains(963549069314L) + " " + parent.positions.contains(963549069314L));
            return parent;
        }

        private void incrementalEdgeAddition(long src, long dst) { // TODO put in a param here like "bias" that determines which of the two components gets to eat the other if they are of the same size? could help with the scaffolder if we could guarantee that no new components would be added without cause
            CollapsedDependencyGraphComponent srcComponent = posToComponent.get(src);
            CollapsedDependencyGraphComponent dstComponent = posToComponent.get(dst);
            if (srcComponent == dstComponent) { // already strongly connected
                return;
            }
            if (srcComponent.outgoingEdges.contains(dstComponent)) { // we already know about this edge
                return;
            }
            List<List<CollapsedDependencyGraphComponent>> paths = new ArrayList<>();
            if (!srcComponent.incomingEdges.isEmpty() && pathExists(dstComponent, srcComponent, paths) > 0) {
                CollapsedDependencyGraphComponent survivor = srcComponent;
                for (List<CollapsedDependencyGraphComponent> path : paths) {
                    if (path.get(0) != srcComponent || path.get(path.size() - 1) != dstComponent) {
                        throw new IllegalStateException();
                    }
                    for (int i = 1; i < path.size(); i++) {
                        if (path.get(i).deleted() || path.get(i) == survivor) { // two different paths to the same goal, only merge the components once, so skip is already survivor or deleted
                            continue;
                        }
                        survivor = mergeInto(survivor, path.get(i));
                    }
                }
                // can't run sanityCheck after each mergeInto because it could leave a 2-way connection between components as an intermediary state while collapsing
                if (Main.DEBUG) {
                    sanityCheck();
                }
                return;
            }
            srcComponent.outgoingEdges.add(dstComponent);
            dstComponent.incomingEdges.add(srcComponent);
        }

        private int pathExists(CollapsedDependencyGraphComponent src, CollapsedDependencyGraphComponent dst, List<List<CollapsedDependencyGraphComponent>> paths) {
            if (src == dst) {
                paths.add(new ArrayList<>(Collections.singletonList(src)));
                return 1;
            }
            if (Main.DEBUG && dst.incomingEdges.isEmpty()) {
                throw new IllegalStateException();
            }
            if (Main.STRICT_Y && src.y() > dst.y()) {
                return 0; // no downward edges in strict_y mode
            }
            int numAdded = 0;
            for (CollapsedDependencyGraphComponent nxt : src.outgoingEdges) {
                int cnt = pathExists(nxt, dst, paths);
                if (cnt > 0) {
                    for (int i = 0; i < cnt; i++) {
                        paths.get(paths.size() - 1 - i).add(src);
                    }
                    numAdded += cnt;
                }
            }
            return numAdded;
        }

        private void incrementalUpdate(long pos) {
            if (posToComponent.containsKey(pos)) {
                throw new IllegalStateException();
            }
            CollapsedDependencyGraphComponent component = addComponent();
            component.positions.add(pos);
            posToComponent.put(pos, component);
            if (Main.DEBUG) {
                sanityCheck();
            }
            //System.out.println("Incremental " + pos);
            //System.out.println("Pos core " + posToComponent.get(pos).index);
            for (Face face : Face.VALUES) {
                if (outgoingEdge(pos, face)) {
                    //System.out.println("Pos outgoing edge " + face + " goes to " + posToComponent.get(face.offset(pos)).index);
                    incrementalEdgeAddition(pos, face.offset(pos));
                }
                if (incomingEdge(pos, face)) {
                    //System.out.println("Pos incoming edge " + face + " comes from " + posToComponent.get(face.offset(pos)).index);
                    incrementalEdgeAddition(face.offset(pos), pos);
                }
            }
            if (Main.DEBUG) {
                sanityCheck();
            }
        }

        public class CollapsedDependencyGraphComponent {

            private final int id;
            private final int hash;
            private final LongOpenHashSet positions = new LongOpenHashSet();
            private final Set<CollapsedDependencyGraphComponent> outgoingEdges = new ObjectOpenHashSet<>();
            private final Set<CollapsedDependencyGraphComponent> incomingEdges = new ObjectOpenHashSet<>();
            // if i change ^^ that "Set" to "ObjectOpenHashSet" it actually makes the bench about 15% SLOWER?!?!?
            private int y = -1;
            private boolean deleted;
            private final Set<CollapsedDependencyGraphComponent> unmodifiableOutgoing = Collections.unmodifiableSet(outgoingEdges);
            private final Set<CollapsedDependencyGraphComponent> unmodifiableIncoming = Collections.unmodifiableSet(incomingEdges);

            private CollapsedDependencyGraphComponent(int id) {
                this.id = id;
                this.hash = HashCommon.murmurHash3(id);
            }

            @Override
            public int hashCode() {
                return hash; // no need to enter native code to get a hashCode, that saves a few nanoseconds
            }

            private int y() {
                if (!Main.STRICT_Y || positions.isEmpty()) {
                    throw new IllegalStateException();
                }
                if (y == -1) {
                    y = BetterBlockPos.YfromLong(positions.iterator().nextLong());
                    if (y == -1) {
                        throw new IllegalStateException();
                    }
                }
                return y;
            }

            public boolean deleted() {
                return deleted;
            }

            public LongSet getPositions() {
                return LongSets.unmodifiable(positions);
            }

            public Set<CollapsedDependencyGraphComponent> getIncoming() {
                return unmodifiableIncoming;
            }

            public Set<CollapsedDependencyGraphComponent> getOutgoing() {
                return unmodifiableOutgoing;
            }
        }

        private void sanityCheck() {
            LongOpenHashSet inComponents = new LongOpenHashSet();
            for (int componentID : components.keySet()) {
                CollapsedDependencyGraphComponent component = components.get(componentID);
                if (component.id != componentID) {
                    throw new IllegalStateException();
                }
                if (component.incomingEdges.contains(component) || component.outgoingEdges.contains(component)) {
                    throw new IllegalStateException(component.id + "");
                }
                if (component.positions.isEmpty()) {
                    throw new IllegalStateException();
                }
                int y = Main.STRICT_Y ? component.y() : -1;
                for (CollapsedDependencyGraphComponent out : component.outgoingEdges) {
                    if (Main.STRICT_Y && out.y() < y) {
                        throw new IllegalStateException();
                    }
                    if (!out.incomingEdges.contains(component)) {
                        throw new IllegalStateException();
                    }
                    if (component.incomingEdges.contains(out)) {
                        throw new IllegalStateException(out.id + " is both an incoming AND and outgoing of " + component.id);
                    }
                }
                for (CollapsedDependencyGraphComponent in : component.incomingEdges) {
                    if (Main.STRICT_Y && in.y() > y) {
                        throw new IllegalStateException();
                    }
                    if (!in.outgoingEdges.contains(component)) {
                        throw new IllegalStateException(in.id + " is an incoming edge of " + component.id + " but it doesn't have that as an outgoing edge");
                    }
                    if (component.outgoingEdges.contains(in)) {
                        throw new IllegalStateException();
                    }
                }
                LongIterator it = component.positions.iterator();
                while (it.hasNext()) {
                    long l = it.nextLong();
                    if (posToComponent.get(l) != component) {
                        throw new IllegalStateException();
                    }
                    if (Main.STRICT_Y && BetterBlockPos.YfromLong(l) != y) {
                        throw new IllegalStateException();
                    }
                    if (!real(l)) {
                        throw new IllegalStateException();
                    }
                }
                inComponents.addAll(component.positions);
            }
            if (!inComponents.equals(posToComponent.keySet())) {
                for (long l : posToComponent.keySet()) {
                    if (!inComponents.contains(l)) {
                        System.out.println(l);
                        System.out.println(posToComponent.get(l).id);
                        System.out.println(posToComponent.get(l).positions.contains(l));
                        System.out.println(posToComponent.get(l).deleted());
                        System.out.println(components.containsValue(posToComponent.get(l)));
                        throw new IllegalStateException(l + " is in posToComponent but not actually in any component");
                    }
                }
                throw new IllegalStateException("impossible");
            }
        }
    }

    private void checkEquality(CollapsedDependencyGraph a, CollapsedDependencyGraph b) {
        if (a.components.size() != b.components.size()) {
            throw new IllegalStateException(a.components.size() + " " + b.components.size());
        }
        if (a.posToComponent.size() != b.posToComponent.size()) {
            throw new IllegalStateException();
        }
        if (!a.posToComponent.keySet().equals(b.posToComponent.keySet())) {
            throw new IllegalStateException();
        }
        a.sanityCheck();
        b.sanityCheck();
        Int2IntOpenHashMap aToB = new Int2IntOpenHashMap();
        for (int key : a.components.keySet()) {
            aToB.put(key, b.posToComponent.get(a.components.get(key).positions.iterator().nextLong()).id);
        }
        for (int i : a.components.keySet()) {
            int bInd = aToB.get(i);
            if (!a.components.get(i).positions.equals(b.components.get(bInd).positions)) {
                throw new IllegalStateException();
            }
            for (List<Set<CollapsedDependencyGraph.CollapsedDependencyGraphComponent>> toCompare : Arrays.asList(
                    Arrays.asList(a.components.get(i).incomingEdges, b.components.get(bInd).incomingEdges),
                    Arrays.asList(a.components.get(i).outgoingEdges, b.components.get(bInd).outgoingEdges)
            )) {
                Set<CollapsedDependencyGraph.CollapsedDependencyGraphComponent> aEdges = toCompare.get(0);
                Set<CollapsedDependencyGraph.CollapsedDependencyGraphComponent> bEdges = toCompare.get(1);
                if (aEdges.size() != bEdges.size()) {
                    throw new IllegalStateException();
                }
                for (CollapsedDependencyGraph.CollapsedDependencyGraphComponent dst : aEdges) {
                    if (!bEdges.contains(b.components.get(aToB.get(dst.id)))) {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }
}
