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

package baritone.api.schematic.mask.operator;

import baritone.api.schematic.mask.AbstractMask;
import baritone.api.schematic.mask.Mask;
import baritone.api.schematic.mask.StaticMask;
import baritone.api.utils.BooleanBinaryOperator;
import net.minecraft.block.BlockState;

/**
 * @author Brady
 */
public final class BinaryOperatorMask extends AbstractMask {

    private final Mask a;
    private final Mask b;
    private final BooleanBinaryOperator operator;

    public BinaryOperatorMask(Mask a, Mask b, BooleanBinaryOperator operator) {
        super(Math.max(a.widthX(), b.widthX()), Math.max(a.heightY(), b.heightY()), Math.max(a.lengthZ(), b.lengthZ()));
        this.a = a;
        this.b = b;
        this.operator = operator;
    }

    @Override
    public boolean partOfMask(int x, int y, int z, BlockState currentState) {
        return this.operator.applyAsBoolean(
                partOfMask(a, x, y, z, currentState),
                partOfMask(b, x, y, z, currentState)
        );
    }

    private static boolean partOfMask(Mask mask, int x, int y, int z, BlockState currentState) {
        return x < mask.widthX() && y < mask.heightY() && z < mask.lengthZ() && mask.partOfMask(x, y, z, currentState);
    }

    public static final class Static extends AbstractMask implements StaticMask {

        private final StaticMask a;
        private final StaticMask b;
        private final BooleanBinaryOperator operator;

        public Static(StaticMask a, StaticMask b, BooleanBinaryOperator operator) {
            super(Math.max(a.widthX(), b.widthX()), Math.max(a.heightY(), b.heightY()), Math.max(a.lengthZ(), b.lengthZ()));
            this.a = a;
            this.b = b;
            this.operator = operator;
        }

        @Override
        public boolean partOfMask(int x, int y, int z) {
            return this.operator.applyAsBoolean(
                    partOfMask(a, x, y, z),
                    partOfMask(b, x, y, z)
            );
        }

        private static boolean partOfMask(StaticMask mask, int x, int y, int z) {
            return x < mask.widthX() && y < mask.heightY() && z < mask.lengthZ() && mask.partOfMask(x, y, z);
        }
    }
}
