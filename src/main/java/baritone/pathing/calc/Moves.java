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
import baritone.pathing.movement.movements.result.DescendResult;
import baritone.pathing.movement.movements.result.ParkourResult;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.util.EnumFacing;

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
        protected Movement apply0(BetterBlockPos src) {
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
            DescendResult res = MovementDescend.cost(context, x, y, z, x + 1, z);
            return new MoveResult(x + 1, res.y, z, res.cost);
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
            DescendResult res = MovementDescend.cost(context, x, y, z, x - 1, z);
            return new MoveResult(x - 1, res.y, z, res.cost);
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
            DescendResult res = MovementDescend.cost(context, x, y, z, x, z - 1);
            return new MoveResult(x, res.y, z - 1, res.cost);
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
            DescendResult res = MovementDescend.cost(context, x, y, z, x, z + 1);
            return new MoveResult(x, res.y, z + 1, res.cost);
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

    PARKOUR_NORTH(0, -4, true) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return MovementParkour.cost(new CalculationContext(), src, EnumFacing.NORTH);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            ParkourResult res = MovementParkour.cost(context, x, y, z, EnumFacing.NORTH);
            return new MoveResult(res.x, y, res.z, res.cost);
        }
    },

    PARKOUR_SOUTH(0, +4, true) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return MovementParkour.cost(new CalculationContext(), src, EnumFacing.SOUTH);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            ParkourResult res = MovementParkour.cost(context, x, y, z, EnumFacing.SOUTH);
            return new MoveResult(res.x, y, res.z, res.cost);
        }
    },

    PARKOUR_EAST(+4, 0, true) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return MovementParkour.cost(new CalculationContext(), src, EnumFacing.EAST);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            ParkourResult res = MovementParkour.cost(context, x, y, z, EnumFacing.EAST);
            return new MoveResult(res.x, y, res.z, res.cost);
        }
    },

    PARKOUR_WEST(-4, 0, true) {
        @Override
        protected Movement apply0(BetterBlockPos src) {
            return MovementParkour.cost(new CalculationContext(), src, EnumFacing.WEST);
        }

        @Override
        public MoveResult apply(CalculationContext context, int x, int y, int z) {
            ParkourResult res = MovementParkour.cost(context, x, y, z, EnumFacing.WEST);
            return new MoveResult(res.x, y, res.z, res.cost);
        }
    };

    protected abstract Movement apply0(BetterBlockPos src);

    public abstract MoveResult apply(CalculationContext context, int x, int y, int z);

    public final boolean dynamicXZ;

    public final int xOffset;
    public final int zOffset;

    Moves(int x, int z, boolean dynamicXZ) {
        this.xOffset = x;
        this.zOffset = z;
        this.dynamicXZ = dynamicXZ;
    }

    Moves(int x, int z) {
        this(x, z, false);
    }
}
