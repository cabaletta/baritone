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

package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Alternate of MovementDiagonal, better optimised for large and flat surfaces to sprint more efficiently without
// switching between straight<->diagonal constantly.
//
// "Knight": In chess, a Knight can move 2 up and 1 to the side.
public class MovementKnight extends Movement {

    private static final double SQRT_5 = Math.sqrt(5);

    private final KnightDirection direction;

    public MovementKnight(IBaritone baritone, BetterBlockPos start, EnumFacing longSide, EnumFacing shortSide) {
        this(baritone, start, start.offset(longSide, 2).offset(shortSide));
    }

    private MovementKnight(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        this(baritone, start, end, new KnightDirection(start, end));
    }

    private MovementKnight(IBaritone baritone, BetterBlockPos start, BetterBlockPos end, KnightDirection dir) {
        super(baritone, start, end, new BetterBlockPos[]{});
        direction = dir;
    }

    static class KnightDirection {
        private final int dX;
        private final int dZ;

        public KnightDirection(BetterBlockPos start, BetterBlockPos end) {
            this(end.x - start.x, end.z - start.z);
        }

        public KnightDirection(int dX, int dZ) {
            if (dX == 0 || dZ == 0
                    || dX > 2 || dZ > 2 || dX < -2 || dZ < -2
                    || (Math.abs(dX) == Math.abs(dZ))
            ) {
                throw new IllegalArgumentException("Illegal direction; dX: " + dX + " dZ: " + dZ);
            }

            this.dX = dX;
            this.dZ = dZ;
        }

        private boolean isXLongest() {
            return Math.abs(dX) > Math.abs(dZ);
        }

        public BetterBlockPos[] allBlocks(int x, int y, int z) {
            return new BetterBlockPos[]{
                    new BetterBlockPos(x, y, z), // start
                    new BetterBlockPos(x + dX, y, z + dZ), // end
                    new BetterBlockPos(x + dX, y, z), // extreme X
                    new BetterBlockPos(x, y, z + dZ), // extreme z
                    longMiddle(x, y, z),
                    sideMiddle(x, y, z),
            };
        }

        public BetterBlockPos longFarCorner(int x, int y, int z) {
            boolean xIsLongest = isXLongest();

            return new BetterBlockPos(x + (xIsLongest ? dX : 0), y, z + (!xIsLongest ? dZ : 0));
        }

        public BetterBlockPos sideNearCorner(int x, int y, int z) {
            boolean xIsShortest = !isXLongest();

            return new BetterBlockPos(x + (xIsShortest ? dX : 0), y, z + (!xIsShortest ? dZ : 0));
        }

        public BetterBlockPos longMiddle(int x, int y, int z) {
            boolean xIsLongest = isXLongest();

            return new BetterBlockPos(x + (xIsLongest ? (dX > 0 ? 1 : -1) : 0), y, z + (!xIsLongest ? (dZ > 0 ? 1 : -1) : 0));
        }

        public BetterBlockPos sideMiddle(int x, int y, int z) {
            return new BetterBlockPos(x + (dX > 0 ? 1 : -1), y, z + (dZ > 0 ? 1 : -1));
        }
    }

    public static void cost(CalculationContext context, int srcX, int y, int srcZ, int dstX, int dstZ, MutableMoveResult res) {
        if (!MovementHelper.canWalkThrough(context.bsi, dstX, y, dstZ)
                || !MovementHelper.canWalkThrough(context.bsi, dstX, y + 1, dstZ)
                || !MovementHelper.canWalkOn(context.bsi, dstX, y - 1, dstZ)) {
            return;
        }

        KnightDirection dir = new KnightDirection(dstX - srcX, dstZ - srcZ);
        BetterBlockPos longFarCorner = dir.longFarCorner(srcX, y, srcZ);
        BetterBlockPos sideNearCorner = dir.sideNearCorner(srcX, y, srcZ);
        BetterBlockPos longMiddle = dir.longMiddle(srcX, y, srcZ);
        BetterBlockPos sideMiddle = dir.sideMiddle(srcX, y, srcZ);

        // check middles

        if (testFullyWalkable(context, longMiddle) || testFullyWalkable(context, sideMiddle)) return;

        // check safety of corner blocks

        if (!safeCorner(context, longFarCorner) || !safeCorner(context, sideNearCorner)) return;

        double multiplier = WALK_ONE_BLOCK_COST;
        boolean water = false;

        if (MovementHelper.isWater(context.getBlock(srcX, y, srcZ))
                || MovementHelper.isWater(context.getBlock(dstX, y, dstZ))
                || MovementHelper.isWater(context.get(longMiddle).getBlock())
                || MovementHelper.isWater(context.get(sideMiddle).getBlock())) {

            multiplier = context.waterWalkSpeed;
            water = true;
        }

        if (context.canSprint && !water) {
            // If we aren't edging around anything, and we aren't in water
            // We can sprint =D
            // Don't check for soul sand, since we can sprint on that too
            multiplier *= SPRINT_MULTIPLIER;
        }

        res.cost = multiplier * SQRT_5;
        res.y = y;
        res.x = dstX;
        res.z = dstZ;
    }

    private static boolean testFullyWalkable(CalculationContext context, BetterBlockPos pos) {
        return !MovementHelper.canWalkThrough(context.bsi, pos.x, pos.y, pos.z)
                || !MovementHelper.canWalkThrough(context.bsi, pos.x, pos.y + 1, pos.z)
                || !MovementHelper.canWalkOn(context.bsi, pos.x, pos.y - 1, pos.z);
    }

    private static boolean safeCorner(CalculationContext context, BetterBlockPos pos) {
        Block under = context.get(pos.down()).getBlock();

        if (under == Blocks.MAGMA
                // todo: maybe we dont need to check for lava on the corners? could be walkable with momentum, no bueno
                || MovementHelper.isLava(under)) {
            return false;
        }

        Block posBlk = context.get(pos).getBlock();
        Block posUpBlk = context.get(pos.up()).getBlock();

        return (!MovementHelper.avoidWalkingInto(posBlk) || posBlk == Blocks.WATER)
                && (!MovementHelper.avoidWalkingInto(posUpBlk));
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        } else if (!playerInValidPosition() && !(MovementHelper.isLiquid(ctx, src) && getValidPositions().contains(ctx.playerFeet().up()))) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        if (sprint()) {
            state.setInput(Input.SPRINT, true);
        }
        MovementHelper.moveTowards(ctx, state, dest);
        return state;
    }

    private boolean sprint() {
        return !MovementHelper.isLiquid(ctx, ctx.playerFeet()) || Baritone.settings().sprintInWater.value;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF;
        }
        return result.cost;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return Arrays.stream(direction.allBlocks(src.x, src.y, src.z)).collect(Collectors.toSet());
    }

    @Override
    public List<BlockPos> toWalkInto(BlockStateInterface bsi) {
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();

            BetterBlockPos lfc = direction.longFarCorner(src.x, src.y, src.z);

            if (!MovementHelper.canWalkThrough(bsi, lfc.x, lfc.y, lfc.z)) {
                toWalkIntoCached.add(lfc);
            }

            if (!MovementHelper.canWalkThrough(bsi, lfc.x, lfc.y + 1, lfc.z)) {
                toWalkIntoCached.add(lfc);
            }

            BetterBlockPos snc = direction.sideNearCorner(src.x, src.y, src.z);

            if (!MovementHelper.canWalkThrough(bsi, snc.x, snc.y, snc.z)) {
                toWalkIntoCached.add(snc);
            }

            if (!MovementHelper.canWalkThrough(bsi, snc.x, snc.y + 1, snc.z)) {
                toWalkIntoCached.add(snc);
            }
        }
        return toWalkIntoCached;
    }
}
