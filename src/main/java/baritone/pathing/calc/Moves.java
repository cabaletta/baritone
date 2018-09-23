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

package baritone.pathing.calc;

import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.movements.*;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;

public enum Moves {
    DOWNWARD(0, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) { // TODO specific return types
            return new MovementDownward(src, src.down());
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x, y - 1, z, MovementDownward.cost(context, x, y, z));
        }
    },

    PILLAR(0, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) { // TODO specific return types
            return new MovementPillar(src, src.up());
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x, y + 1, z, MovementPillar.cost(context, x, y, z));
        }
    },

    TRAVERSE_NORTH(0, -1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementTraverse(src, src.north());
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x, y, z - 1, MovementTraverse.cost(context, x, y, z, x, z - 1));
        }
    },

    TRAVERSE_SOUTH(0, +1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementTraverse(src, src.south());
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x, y, z + 1, MovementTraverse.cost(context, x, y, z, x, z + 1));
        }
    },

    TRAVERSE_EAST(+1, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementTraverse(src, src.east());
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x + 1, y, z, MovementTraverse.cost(context, x, y, z, x + 1, z));
        }
    },

    TRAVERSE_WEST(-1, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementTraverse(src, src.west());
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x - 1, y, z, MovementTraverse.cost(context, x, y, z, x - 1, z));
        }
    },

    ASCEND_NORTH(0, -1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementAscend(src, new BetterBlockPos(src.x, src.y + 1, src.z - 1));
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x, y + 1, z - 1, MovementAscend.cost(context, x, y, z, x, z - 1));
        }
    },

    ASCEND_SOUTH(0, +1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementAscend(src, new BetterBlockPos(src.x, src.y + 1, src.z + 1));
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x, y + 1, z + 1, MovementAscend.cost(context, x, y, z, x, z + 1));
        }
    },

    ASCEND_EAST(+1, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementAscend(src, new BetterBlockPos(src.x + 1, src.y + 1, src.z));
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x + 1, y + 1, z, MovementAscend.cost(context, x, y, z, x + 1, z));
        }
    },

    ASCEND_WEST(-1, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementAscend(src, new BetterBlockPos(src.x - 1, src.y + 1, src.z));
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x - 1, y + 1, z, MovementAscend.cost(context, x, y, z, x - 1, z));
        }
    },

    DESCEND_EAST(+1, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            MoveResult res = apply(new CalculationContext(), src.x, src.y, src.z);
            if (res.destY == src.y - 1) {
                return new MovementDescend(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            } else {
                return new MovementFall(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            }
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            Tuple<Integer, Double> res = MovementDescend.cost(context, x, y, z, x + 1, z);
            return new MoveResult(x + 1, res.getFirst(), z, res.getSecond());
        }
    },

    DESCEND_WEST(-1, 0) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            MoveResult res = apply(new CalculationContext(), src.x, src.y, src.z);
            if (res.destY == src.y - 1) {
                return new MovementDescend(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            } else {
                return new MovementFall(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            }
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            Tuple<Integer, Double> res = MovementDescend.cost(context, x, y, z, x - 1, z);
            return new MoveResult(x - 1, res.getFirst(), z, res.getSecond());
        }
    },

    DESCEND_NORTH(0, -1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            MoveResult res = apply(new CalculationContext(), src.x, src.y, src.z);
            if (res.destY == src.y - 1) {
                return new MovementDescend(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            } else {
                return new MovementFall(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            }
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            Tuple<Integer, Double> res = MovementDescend.cost(context, x, y, z, x, z - 1);
            return new MoveResult(x, res.getFirst(), z - 1, res.getSecond());
        }
    },

    DESCEND_SOUTH(0, +1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            MoveResult res = apply(new CalculationContext(), src.x, src.y, src.z);
            if (res.destY == src.y - 1) {
                return new MovementDescend(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            } else {
                return new MovementFall(src, new BetterBlockPos(res.destX, res.destY, res.destZ));
            }
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            Tuple<Integer, Double> res = MovementDescend.cost(context, x, y, z, x, z + 1);
            return new MoveResult(x, res.getFirst(), z + 1, res.getSecond());
        }
    },

    DIAGONAL_NORTHEAST(+1, -1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementDiagonal(src, EnumFacing.NORTH, EnumFacing.EAST);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x + 1, y, z - 1, MovementDiagonal.cost(context, x, y, z, x + 1, z - 1));
        }
    },

    DIAGONAL_NORTHWEST(-1, -1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementDiagonal(src, EnumFacing.NORTH, EnumFacing.WEST);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x - 1, y, z - 1, MovementDiagonal.cost(context, x, y, z, x - 1, z - 1));
        }
    },

    DIAGONAL_SOUTHEAST(+1, +1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementDiagonal(src, EnumFacing.SOUTH, EnumFacing.EAST);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x + 1, y, z + 1, MovementDiagonal.cost(context, x, y, z, x + 1, z + 1));
        }
    },

    DIAGONAL_SOUTHWEST(-1, +1) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return new MovementDiagonal(src, EnumFacing.SOUTH, EnumFacing.WEST);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            return new MoveResult(x - 1, y, z + 1, MovementDiagonal.cost(context, x, y, z, x - 1, z + 1));
        }
    },
    // TODO parkour
    ;

    protected abstract Movement apply0(BetterBlockPos src);


    public abstract MoveResult apply(CalculationContext context, int x, int y, int z);

    public final int xOffset;
    public final int zOffset;

    Moves(int x, int z) {
        this.xOffset = x;
        this.zOffset = z;
    }
}
