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
 * A plane against which this block state can be placed
 * <p>
 * For a normal block, this will be a full face of a block. In that case, this class is no more than an EnumFacing
 * <p>
 * For a block like a slab or a stair, this will contain the information that the placement must be against the top or bottom half of the face
 */
public class BlockStatePlacementOption {

    /**
     * e.g. a torch placed down on the ground is placed against the bottom of "the torch bounding box", so this would be DOWN for the torch
     */
    public final Face against;
    public final Half half;

    public BlockStatePlacementOption(Face against, Half half) {
        this.against = against;
        this.half = half;
        if ((against == Face.DOWN || against == Face.UP) && half != Half.EITHER) {
            throw new IllegalArgumentException();
        }
    }

    public BlockStatePlacementOption(Face against) {
        this(against, Half.EITHER);
    }

    enum Half {
        TOP, BOTTOM, EITHER
    }
}
