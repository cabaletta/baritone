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

/**
 * An immutable graph representing block placement dependency order
 * <p>
 * Air blocks are treated as scaffolding!
 * <p>
 * Edge A --> B means that B can be placed against A
 */
public class PlaceOrderDependencyGraph {

    private final PackedBlockStateCuboid states;
    private final byte[] edges;

    public PlaceOrderDependencyGraph(PackedBlockStateCuboid states) {
        this.states = states;
        this.edges = new byte[states.bounds.volume()];

        bounds().forEach(this::compute);
    }

    private void compute(long pos) {
        byte val = 0;
        for (BlockStatePlacementOption option : data(pos).options) {
            if (Main.STRICT_Y && option.against == Face.UP) {
                throw new IllegalStateException();
            }
            long againstPos = option.against.offset(pos);
            BlockStateCachedData against;
            if (inRange(againstPos)) {
                against = data(againstPos);
            } else {
                against = BlockStateCachedData.SCAFFOLDING;
            }
            if (against.possibleAgainstMe(option)) {
                val |= 1 << option.against.index;
            }
        }
        edges[states.bounds.toIndex(pos)] = val;
    }

    public BlockStateCachedData data(long pos) {
        int state = state(pos);
        if (treatAsScaffolding(state)) {
            return BlockStateCachedData.SCAFFOLDING;
        }
        return BlockStateCachedData.get(state);
    }

    // example: dirt at 0,0,0 torch at 0,1,0. outgoingEdge(0,0,0,UP) returns true, incomingEdge(0,1,0,DOWN) returns true

    public boolean outgoingEdge(long pos, Face face) {
        if (!inRange(pos)) {
            return false;
        }
        return incomingEdge(face.offset(pos), face.opposite());
    }

    public boolean incomingEdge(long pos, Face face) {
        if (!inRange(face.offset(pos))) {
            return false;
        }
        return incomingEdgePermitExterior(pos, face);
    }

    public boolean incomingEdgePermitExterior(long pos, Face face) {
        if (!inRange(pos)) {
            return false;
        }
        return (edges[bounds().toIndex(pos)] & 1 << face.index) != 0;
    }

    public boolean airTreatedAsScaffolding(long pos) {
        return treatAsScaffolding(state(pos));
    }

    private int state(long pos) {
        return states.get(bounds().toIndex(pos));
    }

    private boolean inRange(long pos) {
        return bounds().inRangePos(pos);
    }

    public Bounds bounds() {
        return states.bounds;
    }

    private boolean treatAsScaffolding(int state) {
        return BlockStateCachedData.get(state).isAir;
    }
}
