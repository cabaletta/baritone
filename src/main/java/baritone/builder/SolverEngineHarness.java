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
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

public class SolverEngineHarness {

    private final ISolverEngine engine;
    private final PackedBlockStateCuboid blocks;
    private final PlaceOrderDependencyGraph graph;
    private final Scaffolder scaffolder;

    public SolverEngineHarness(ISolverEngine engine, PackedBlockStateCuboid blocks) {
        this.engine = engine;
        this.blocks = blocks;
        this.graph = new PlaceOrderDependencyGraph(blocks);
        this.scaffolder = Scaffolder.run(graph);
    }

    public List<SolvedActionStep> solve(long playerStartPos) {
        LongOpenHashSet alreadyPlacedSoFar = new LongOpenHashSet();
        List<SolvedActionStep> steps = new ArrayList<>();
        while (true) {
            Set<CollapsedDependencyGraphComponent> frontier = calculateCurrentSolverFrontier(alreadyPlacedSoFar);
            if (frontier.isEmpty()) {
                // nothing on the table!
                break;
            }
            List<LongOpenHashSet> goals = expandAndSubtract(frontier, alreadyPlacedSoFar);
            long playerPos = steps.isEmpty() ? playerStartPos : steps.get(steps.size() - 1).playerMovesTo();
            SolverEngineInput inp = new SolverEngineInput(graph, scaffolder.scaffolding(), alreadyPlacedSoFar, goals, playerPos);
            SolverEngineOutput out = engine.solve(inp);
            if (Main.DEBUG) {
                out.sanityCheck(inp);
            }
            steps.addAll(out.getSteps());
            LongList ancillaryScaffolding = new LongArrayList();
            for (SolvedActionStep step : out.getSteps()) {
                OptionalLong blockPlace = step.placeAt();
                if (blockPlace.isPresent()) {
                    long pos = blockPlace.getAsLong();
                    if (!alreadyPlacedSoFar.add(pos)) {
                        throw new IllegalStateException();
                    }
                    if (scaffolder.air(pos)) { // not part of the schematic, nor intended scaffolding
                        ancillaryScaffolding.add(pos); // therefore it must be ancillary scaffolding, some throwaway block we needed to place in order to achieve something else, maybe to get a needed vantage point on some particularly tricky placement
                    }
                }
            }
            scaffolder.enableAncillaryScaffoldingAndRecomputeRoot(ancillaryScaffolding);
        }
        if (Main.DEBUG) {
            scaffolder.forEachReal(pos -> {
                if (!alreadyPlacedSoFar.contains(pos)) {
                    throw new IllegalStateException();
                }
            });
            alreadyPlacedSoFar.forEach(pos -> {
                if (!scaffolder.real(pos)) {
                    throw new IllegalStateException();
                }
            });
        }
        return steps;
    }

    private List<LongOpenHashSet> expandAndSubtract(Set<CollapsedDependencyGraphComponent> frontier, LongSet already) {
        return frontier.stream()
                .map(component -> {
                    LongOpenHashSet remainingPositionsInComponent = new LongOpenHashSet(component.getPositions().size());
                    LongIterator it = component.getPositions().iterator();
                    while (it.hasNext()) {
                        long pos = it.nextLong();
                        if (!already.contains(pos)) {
                            remainingPositionsInComponent.add(pos);
                        }
                    }
                    return remainingPositionsInComponent;
                }).collect(Collectors.toList());
    }

    private Set<CollapsedDependencyGraphComponent> calculateCurrentSolverFrontier(LongSet alreadyPlacedSoFar) {
        Set<CollapsedDependencyGraphComponent> currentFrontier = new ObjectOpenHashSet<>();
        Set<CollapsedDependencyGraphComponent> confirmedFullyCompleted = new ObjectOpenHashSet<>();
        ObjectArrayFIFOQueue<CollapsedDependencyGraphComponent> toExplore = new ObjectArrayFIFOQueue<>();
        toExplore.enqueue(scaffolder.getRoot());
        outer:
        while (!toExplore.isEmpty()) {
            CollapsedDependencyGraphComponent component = toExplore.dequeue();
            for (CollapsedDependencyGraphComponent parent : component.getIncoming()) {
                if (!confirmedFullyCompleted.contains(parent)) {
                    // to be here, one parent must have been fully completed, but it's possible a different parent is not yet fully completed
                    // this is because while the collapsed block placement dependency graph is a directed acyclic graph, it can still have a diamond shape
                    // imagine the dependency is A->B, A->C, C->D, B->E, D->E (so, A is root, it splits in two, then converges at E)
                    // it might explore it in order A, B, C, E (because B, yet canceled as D is not completed yet), D (because C), E (now succeeds since B and D are confirmed)
                    continue outer;
                }
            }
            LongIterator it = component.getPositions().iterator();
            while (it.hasNext()) {
                if (!alreadyPlacedSoFar.contains(it.nextLong())) {
                    currentFrontier.add(component);
                    continue outer;
                }
            }
            if (confirmedFullyCompleted.add(component)) {
                for (CollapsedDependencyGraphComponent child : component.getOutgoing()) {
                    toExplore.enqueue(child); // always reenqueue children because we added ourselves to confirmedFullyCompleted, meaning this time they may well have all parents completed
                }
            }
        }
        return currentFrontier;
    }
}
