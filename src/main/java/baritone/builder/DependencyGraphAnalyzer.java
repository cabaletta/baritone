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
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Some initial checks on the schematic
 * <p>
 * The intent is to provide reasonable error messages, which we can do by catching common cases as early as possible
 * <p>
 * So that it's an actual comprehensible error **that tells you where the problem is** instead of just "pathing failed"
 */
public class DependencyGraphAnalyzer {

    /**
     * Just a simple check to make sure that everything is placeable.
     * <p>
     * Mostly for my own testing because every Minecraft block is placeable, and if the schematic has something weird
     * and funky, it should be caught earlier anyway.
     */
    public static void prevalidate(PlaceOrderDependencyGraph graph) {
        List<String> locs = new ArrayList<>();
        graph.bounds().forEach(pos -> {
            if (graph.airTreatedAsScaffolding(pos)) {
                // completely fine to, for example, have an air pocket with non-place-against-able stuff all around it
                return;
            }
            for (Face face : Face.VALUES) {
                if (graph.incomingEdge(pos, face)) {
                    return;
                }
            }
            locs.add(BetterBlockPos.fromLong(pos).toString());
        });
        if (!locs.isEmpty()) {
            throw new IllegalStateException("Unplaceable from any side: " + cuteTrim(locs));
        }
    }

    /**
     * Search from all exterior nodes breadth-first to ensure that, theoretically, everything is reachable.
     * <p>
     * This is NOT a sufficient test, because later we are going to ensure that everything is scaffold-placeable which
     * requires a single root node at the bottom.
     */
    public static void prevalidateExternalToInteriorSearch(PlaceOrderDependencyGraph graph) {
        LongOpenHashSet reachable = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        graph.bounds().forEach(pos -> {
            for (Face face : Face.VALUES) {
                if (graph.incomingEdgePermitExterior(pos, face) && !graph.incomingEdge(pos, face)) {
                    // this block is placeable from the exterior of the schematic!
                    queue.enqueue(pos); // this will intentionally put the top of the schematic at the front
                }
            }
        });
        while (!queue.isEmpty()) {
            long pos = queue.dequeueLong();
            if (reachable.add(pos)) {
                for (Face face : Face.VALUES) {
                    if (graph.outgoingEdge(pos, face)) {
                        queue.enqueueFirst(face.offset(pos));
                    }
                }
            }
        }
        List<String> locs = new ArrayList<>();
        graph.bounds().forEach(pos -> {
            if (graph.airTreatedAsScaffolding(pos)) {
                // same as previous validation
                return;
            }
            if (!reachable.contains(pos)) {
                locs.add(BetterBlockPos.fromLong(pos).toString());
            }
        });
        if (!locs.isEmpty()) {
            throw new IllegalStateException("Placeable, in theory, but in practice there is no valid path from the exterior to it: " + cuteTrim(locs));
        }
    }

    private static List<String> cuteTrim(List<String> pos) {
        if (pos.size() <= 20) {
            return pos;
        }
        pos = pos.subList(0, 20);
        pos.set(pos.size() - 1, "...");
        return pos;
    }
}
