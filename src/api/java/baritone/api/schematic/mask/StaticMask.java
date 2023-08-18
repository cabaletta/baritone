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

package baritone.api.schematic.mask;

import baritone.api.schematic.mask.operator.BinaryOperatorMask;
import baritone.api.schematic.mask.operator.NotMask;
import baritone.api.utils.BooleanBinaryOperators;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A mask that is context-free. In other words, it doesn't require the current block state to determine if a relative
 * position is a part of the mask.
 *
 * @author Brady
 */
public interface StaticMask extends Mask {

    /**
     * Determines if a given relative coordinate is included in this mask, without the need for the current block state.
     *
     * @param x The relative x position of the block
     * @param y The relative y position of the block
     * @param z The relative z position of the block
     * @return Whether the given position is included in this mask
     */
    boolean partOfMask(int x, int y, int z);

    /**
     * Implements the parent {@link Mask#partOfMask partOfMask function} by calling the static function
     * provided in this functional interface without needing the {@link BlockState} argument. This {@code default}
     * implementation should <b><u>NOT</u></b> be overriden.
     *
     * @param x            The relative x position of the block
     * @param y            The relative y position of the block
     * @param z            The relative z position of the block
     * @param currentState The current state of that block in the world, may be {@code null}
     * @return Whether the given position is included in this mask
     */
    @Override
    default boolean partOfMask(int x, int y, int z, BlockState currentState) {
        return this.partOfMask(x, y, z);
    }

    @Override
    default StaticMask not() {
        return new NotMask.Static(this);
    }

    default StaticMask union(StaticMask other) {
        return new BinaryOperatorMask.Static(this, other, BooleanBinaryOperators.OR);
    }

    default StaticMask intersection(StaticMask other) {
        return new BinaryOperatorMask.Static(this, other, BooleanBinaryOperators.AND);
    }

    default StaticMask xor(StaticMask other) {
        return new BinaryOperatorMask.Static(this, other, BooleanBinaryOperators.XOR);
    }

    /**
     * Returns a pre-computed mask using {@code this} function, with the specified size parameters.
     */
    default StaticMask compute() {
        return new PreComputedMask(this);
    }
}
