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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

public class SolverEngineInput {

    public final PlaceOrderDependencyGraph graph;
    public final LongSet intendedScaffolding;
    public final LongSet alreadyPlaced;
    private final LongOpenHashSet toPlaceNow;
    public final long player;

    /**
     * @param graph               The place dependency graph of the end goal (both schematic and terrain). Where the terrain ends and the schematic begins is immaterial.
     * @param intendedScaffolding These locations are overridden from air to scaffolding in the dependency graph. Also, bias towards placing scaffolding blocks in these locations (since the scaffolder tells us that'll be helpful to future sections)
     * @param alreadyPlaced       Locations that are currently not-air. Can include entries in intendedScaffolding (indicating placed scaffolding), entries that are non-air in the graph (indicating completed parts of the build), and even entries that are air in the graph and not in intendedScaffolding (indicating incidental scaffolding from previous sections that the scaffolder did not anticipate needing to place)
     * @param toPlaceNow          Locations that are currently top of mind and must be placed. For example, to place the rest of the graph, this would be intendedScaffolding|(graph.allNonAir&~alreadyPlaced)
     * @param player              Last but not least, where is the player standing?
     */
    public SolverEngineInput(PlaceOrderDependencyGraph graph, LongOpenHashSet intendedScaffolding, LongOpenHashSet alreadyPlaced, LongOpenHashSet toPlaceNow, long player) {
        this.graph = graph;
        this.intendedScaffolding = LongSets.unmodifiable(intendedScaffolding);
        this.alreadyPlaced = LongSets.unmodifiable(alreadyPlaced);
        this.toPlaceNow = toPlaceNow;
        this.player = player;
        if (Main.DEBUG) {
            sanityCheck();
        }
    }

    private void sanityCheck() {
        if (!graph.bounds().inRangePos(player)) {
            throw new IllegalStateException();
        }
        for (LongSet toVerify : new LongSet[]{intendedScaffolding, alreadyPlaced, toPlaceNow}) {
            for (long pos : toVerify) {
                if (!graph.bounds().inRangePos(pos)) {
                    throw new IllegalStateException();
                }
            }
        }
        for (long pos : toPlaceNow) {
            if (alreadyPlaced.contains(pos)) {
                throw new IllegalStateException();
            }
        }
    }

    public boolean desiredToBePlaced(long pos) {
        if (Main.DEBUG && !graph.bounds().inRangePos(pos)) {
            throw new IllegalStateException();
        }
        return !graph.data(pos).isAir || intendedScaffolding.contains(pos);
    }
}
