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
import it.unimi.dsi.fastutil.longs.*;
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

    private final IScaffolderStrategy strategy;
    private final DependencyGraphScaffoldingOverlay overlayGraph;
    // NOTE: these next three fields are updated in-place as the overlayGraph is updated :)
    private final CollapsedDependencyGraph collapsedGraph;
    private final Int2ObjectMap<CollapsedDependencyGraphComponent> components;
    private final Long2ObjectMap<CollapsedDependencyGraphComponent> componentLocations;

    private final List<CollapsedDependencyGraphComponent> rootComponents;

    private Scaffolder(PlaceOrderDependencyGraph graph, IScaffolderStrategy strategy) {
        this.strategy = strategy;
        this.overlayGraph = new DependencyGraphScaffoldingOverlay(graph);
        this.collapsedGraph = overlayGraph.getCollapsedGraph();
        this.components = collapsedGraph.getComponents();
        this.componentLocations = collapsedGraph.getComponentLocations();

        this.rootComponents = calcRoots();
    }

    public static Scaffolder run(PlaceOrderDependencyGraph graph, IScaffolderStrategy strategy) {
        Scaffolder scaffolder = new Scaffolder(graph, strategy);
        while (scaffolder.rootComponents.size() > 1) {
            scaffolder.loop();
        }
        return scaffolder;
    }

    private List<CollapsedDependencyGraphComponent> calcRoots() {
        // since the components form a DAG (because all strongly connected components, and therefore all cycles, have been collapsed)
        // we can locate all root components by simply finding the ones with no incoming edges
        return components
                .values()
                .stream()
                .filter(component -> component.getIncoming().isEmpty())
                .collect(Collectors.toCollection(ArrayList::new)); // ensure arraylist since we will be mutating the list
    }

    private void loop() {
        if (rootComponents.size() <= 1) {
            throw new IllegalStateException();
        }
        CollapsedDependencyGraphComponent root = rootComponents.get(rootComponents.size() - 1); // don't remove yet since we aren't sure which way it'll merge (in theory, in practice it'll stop being a root when STRICT_Y is true, since it'll become a descendant, but in theory with STRICT_Y false it could merge on equal footing with another component)
        if (!root.getIncoming().isEmpty()) {
            throw new IllegalStateException();
        }
        LongList path = strategy.scaffoldTo(root, overlayGraph);
        if (!root.getPositions().contains(path.get(path.size() - 1))) {
            throw new IllegalStateException();
        }
        if (!componentLocations.containsKey(path.get(0))) {
            throw new IllegalStateException();
        }
        for (int i = 1; i < path.size(); i++) {
            if (!overlayGraph.hypotheticalScaffoldingIncomingEdge(path.get(i), Face.between(path.get(i), path.get(i - 1)))) {
                throw new IllegalStateException();
            }
        }
        enable(path.subList(1, path.size() - 1));
    }

    private void enable(LongList positions) {
        positions.forEach(pos -> {
            if (componentLocations.containsKey(pos)) {
                throw new IllegalStateException();
            }
        });
        int cid = collapsedGraph.lastComponentID().getAsInt();

        positions.forEach(overlayGraph::enable); // TODO more performant to enable in reverse order maybe?

        int newCID = collapsedGraph.lastComponentID().getAsInt();
        for (int i = cid + 1; i <= newCID; i++) {
            if (components.get(i) != null && components.get(i).getIncoming().isEmpty()) {
                rootComponents.add(components.get(i));
            }
        }
        // this works because as we add new components and connect them up, we can say that
        rootComponents.removeIf(CollapsedDependencyGraphComponent::deleted);
        if (Main.DEBUG) {
            if (!rootComponents.equals(calcRoots())) {
                throw new IllegalStateException();
            }
        }
    }

    public void enableAncillaryScaffoldingAndRecomputeRoot(LongList positions) {
        System.out.println("TODO: should ancillary scaffolding even recompute the components? that scaffolding doesn't NEED to part of any component, and having all components be mutable even after the scaffolder is done is sketchy");
        getRoot();
        enable(positions);
        getRoot();
    }

    public CollapsedDependencyGraphComponent getRoot() { // TODO this should probably return a new class that is not mutable in-place
        if (rootComponents.size() != 1) {
            throw new IllegalStateException(); // this is okay because this can only possibly be called after Scaffolder.run is completed
        }
        CollapsedDependencyGraphComponent root = rootComponents.get(0);
        if (!root.getIncoming().isEmpty() || root.deleted()) {
            throw new IllegalStateException();
        }
        return root;
    }


    // TODO should Scaffolder return a different class? "CompletedScaffolding" or something that has these methods as non-delegate, as well as getRoot returning a immutable equivalent of CollapsedDependencyGraphComponent?
    public boolean real(long pos) {
        return overlayGraph.real(pos);
    }

    public void forEachReal(Bounds.BoundsLongConsumer consumer) {
        overlayGraph.forEachReal(consumer);
    }

    public LongSets.UnmodifiableSet scaffolding() {
        return overlayGraph.scaffolding();
    }

    public boolean air(long pos) {
        return overlayGraph.air(pos);
    }
}
