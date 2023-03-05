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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

import java.util.Collection;
import java.util.List;

public class SolverEngineInput {

    public final PlaceOrderDependencyGraph graph;
    public final LongSet intendedScaffolding;
    public final LongSet alreadyPlaced;
    public final LongSet allToPlaceNow;
    private final List<LongOpenHashSet> toPlaceNow;
    public final Bounds bounds;
    public final long player;

    /**
     * @param graph               The place dependency graph of the end goal (both schematic and terrain). Where the terrain ends and the schematic begins is immaterial.
     * @param intendedScaffolding These locations are overridden from air to scaffolding in the dependency graph. Also, bias towards placing scaffolding blocks in these locations (since the scaffolder tells us that'll be helpful to future sections)
     * @param alreadyPlaced       Locations that are currently not-air. Can include entries in intendedScaffolding (indicating placed scaffolding), entries that are non-air in the graph (indicating completed parts of the build), and even entries that are air in the graph and not in intendedScaffolding (indicating incidental scaffolding from previous sections that the scaffolder did not anticipate needing to place)
     * @param toPlaceNow          Locations that are currently top of mind and must be placed. For example, to place the rest of the graph, this would be intendedScaffolding|(graph.allNonAir&~alreadyPlaced)
     * @param player              Last but not least, where is the player standing?
     */
    public SolverEngineInput(PlaceOrderDependencyGraph graph, LongSets.UnmodifiableSet intendedScaffolding, LongOpenHashSet alreadyPlaced, List<LongOpenHashSet> toPlaceNow, long player) {
        this.graph = graph;
        this.intendedScaffolding = intendedScaffolding;
        this.alreadyPlaced = LongSets.unmodifiable(alreadyPlaced);
        this.toPlaceNow = toPlaceNow;
        this.player = player;
        this.allToPlaceNow = combine(toPlaceNow);
        this.bounds = graph.bounds();
        if (Main.DEBUG) {
            sanityCheck();
        }
    }

    private void sanityCheck() {
        if (!bounds.inRangePos(player)) {
            throw new IllegalStateException();
        }
        for (LongSet toVerify : new LongSet[]{intendedScaffolding, alreadyPlaced}) {
            for (long pos : toVerify) {
                if (!bounds.inRangePos(pos)) {
                    throw new IllegalStateException();
                }
            }
        }
        for (LongSet toPlace : toPlaceNow) {
            for (long pos : toPlace) {
                if (alreadyPlaced.contains(pos)) {
                    throw new IllegalStateException();
                }
                if (!bounds.inRangePos(pos)) {
                    throw new IllegalStateException();
                }
                if (intendedScaffolding.contains(pos) ^ graph.airTreatedAsScaffolding(pos)) {
                    throw new IllegalStateException();
                }
            }
        }
    }

    public PlacementDesire desiredToBePlaced(long pos) {
        if (Main.DEBUG && !graph.bounds().inRangePos(pos)) {
            throw new IllegalStateException();
        }
        if (allToPlaceNow.contains(pos)) {
            if (Main.DEBUG && (intendedScaffolding.contains(pos) != graph.airTreatedAsScaffolding(pos))) {
                throw new IllegalStateException("adding this sanity check 2+ years later, hope it's correct");
            }
            if (graph.airTreatedAsScaffolding(pos)) {
                return PlacementDesire.SCAFFOLDING_OF_CURRENT_GOAL;
            } else {
                return PlacementDesire.PART_OF_CURRENT_GOAL;
            }
        } else {
            // for positions NOT in allToPlaceNow, intendedScaffolding is not guaranteed to be equivalent to airTreatedAsScaffolding
            if (graph.airTreatedAsScaffolding(pos)) {
                if (intendedScaffolding.contains(pos)) {
                    return PlacementDesire.SCAFFOLDING_OF_FUTURE_GOAL;
                } else {
                    return PlacementDesire.ANCILLARY;
                }
            } else {
                return PlacementDesire.PART_OF_FUTURE_GOAL;
            }
        }
    }

    public enum PlacementDesire {
        PART_OF_CURRENT_GOAL,
        PART_OF_FUTURE_GOAL,
        SCAFFOLDING_OF_CURRENT_GOAL,
        SCAFFOLDING_OF_FUTURE_GOAL,
        ANCILLARY
    }

    private static LongOpenHashSet combine(List<LongOpenHashSet> entries) {
        LongOpenHashSet ret = new LongOpenHashSet(entries.stream().mapToInt(Collection::size).sum());
        for (LongOpenHashSet set : entries) {
            LongIterator it = set.iterator();
            while (it.hasNext()) {
                ret.add(it.nextLong());
            }
        }
        return ret;
    }

    public BlockStateCachedData at(long pos, WorldState inWorldState) {
        if (bounds.inRangePos(pos)) {
            if (inWorldState.blockExists(pos)) {
                return graph.data(pos);
            } else {
                return BlockStateCachedData.AIR;
            }
        } else {
            return BlockStateCachedData.OUT_OF_BOUNDS;
        }
    }
}
