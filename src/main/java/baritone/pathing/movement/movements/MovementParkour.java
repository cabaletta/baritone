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
import baritone.pathing.movement.ParkourPresets;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.WaterFluid;

import java.util.HashSet;
import java.util.Set;

public class MovementParkour extends Movement {

    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};

    /**
     * Total X offset from src to dest (can be negative).
     */
    private final int dx;

    /**
     * Total Z offset from src to dest (can be negative).
     */
    private final int dz;

    /**
     * Jump category distance: max(|dx|, |dz|). Used for sprint thresholds etc.
     */
    private final int dist;

    /** True when the destination is 1 block above the source. */
    private final boolean ascend;

    /** True when both dx and dz are non-zero (diagonal parkour). */
    private final boolean diagonal;

    private MovementParkour(IBaritone baritone, BetterBlockPos src, int dx, int dz, boolean ascend) {
        super(baritone, src,
                new BetterBlockPos(src.x + dx, src.y + (ascend ? 1 : 0), src.z + dz),
                EMPTY,
                new BetterBlockPos(src.x + dx, src.y + (ascend ? 0 : -1), src.z + dz));
        this.dx = dx;
        this.dz = dz;
        this.dist = Math.max(Math.abs(dx), Math.abs(dz));
        this.ascend = ascend;
        this.diagonal = dx != 0 && dz != 0;
    }

    // ──────────────────────────────────────
    //  Factory methods
    // ──────────────────────────────────────

    /**
     * Cardinal-direction factory (backwards-compatible with existing Moves).
     */
    public static MovementParkour cost(CalculationContext context, BetterBlockPos src, Direction direction) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, direction, res);
        int resolvedDx = res.x - src.x;
        int resolvedDz = res.z - src.z;
        return new MovementParkour(context.getBaritone(), src, resolvedDx, resolvedDz, res.y > src.y);
    }

    /**
     * Diagonal / sign-based factory — xSign and zSign must be -1, 0, or 1.
     * The cost method iterates through applicable presets and the resolved
     * dx/dz are taken from the result, not the sign values themselves.
     * Falls back to cardinal when one sign is zero.
     */
    public static MovementParkour cost(CalculationContext context, BetterBlockPos src, int xSign, int zSign) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, xSign, zSign, res);
        if (res.cost >= COST_INF) {
            return null;
        }
        int resolvedDx = res.x - src.x;
        int resolvedDz = res.z - src.z;
        return new MovementParkour(context.getBaritone(), src, resolvedDx, resolvedDz, res.y > src.y);
    }

    // ──────────────────────────────────────
    //  Cost calculation — Direction delegate
    // ──────────────────────────────────────

    /**
     * Cardinal parkour cost (called from Moves enum).
     * Delegates to the dx/dz-based method with unit-step iteration.
     */
    public static void cost(CalculationContext context, int x, int y, int z, Direction dir, MutableMoveResult res) {
        int xStep = dir.getStepX();
        int zStep = dir.getStepZ();
        costCardinal(context, x, y, z, xStep, zStep, res);
    }

    /**
     * Sign-based cost entry-point. Routes to cardinal or diagonal logic
     * depending on whether both xSign and zSign are non-zero.
     * xSign / zSign must be -1, 0, or 1.
     */
    public static void cost(CalculationContext context, int x, int y, int z, int xSign, int zSign, MutableMoveResult res) {
        if (xSign != 0 && zSign != 0) {
            costDiagonal(context, x, y, z, xSign, zSign, res);
        } else {
            costCardinal(context, x, y, z, xSign, zSign, res);
        }
    }

    // ──────────────────────────────────────
    //  Cardinal parkour (existing logic, dx/dz based)
    // ──────────────────────────────────────

    private static void costCardinal(CalculationContext context, int x, int y, int z,
                                      int xStep, int zStep, MutableMoveResult res) {
        if (!context.allowParkour) {
            return;
        }
        if (!context.allowJumpAtBuildLimit && y >= context.world.getMaxBuildHeight()) {
            return;
        }
        if (!MovementHelper.fullyPassable(context, x + xStep, y, z + zStep)) {
            return;
        }
        BlockState adj = context.get(x + xStep, y - 1, z + zStep);
        if (MovementHelper.canWalkOn(context, x + xStep, y - 1, z + zStep, adj)) {
            return;
        }
        if (MovementHelper.avoidWalkingInto(adj) && !(adj.getFluidState().getType() instanceof WaterFluid)) {
            return;
        }
        if (!MovementHelper.fullyPassable(context, x + xStep, y + 1, z + zStep)) {
            return;
        }
        if (!MovementHelper.fullyPassable(context, x + xStep, y + 2, z + zStep)) {
            return;
        }
        if (!MovementHelper.fullyPassable(context, x, y + 2, z)) {
            return;
        }
        BlockState standingOn = context.get(x, y - 1, z);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof StairBlock || MovementHelper.isBottomSlab(standingOn)) {
            return;
        }
        if (context.assumeWalkOnWater && !standingOn.getFluidState().isEmpty()) {
            return;
        }
        if (!context.get(x, y, z).getFluidState().isEmpty()) {
            return;
        }
        int maxJump;
        if (context.allowWalkOnMagmaBlocks && standingOn.is(Blocks.MAGMA_BLOCK)) {
            maxJump = 2;
        } else if (standingOn.getBlock() == Blocks.SOUL_SAND) {
            maxJump = 2;
        } else if (context.canSprint) {
            maxJump = 4;
        } else {
            maxJump = 3;
        }

        int verifiedMaxJump = 1;
        for (int i = 2; i <= maxJump; i++) {
            int destX = x + xStep * i;
            int destZ = z + zStep * i;

            if (!MovementHelper.fullyPassable(context, destX, y + 1, destZ)) {
                break;
            }
            if (!MovementHelper.fullyPassable(context, destX, y + 2, destZ)) {
                break;
            }

            BlockState destInto = context.bsi.get0(destX, y, destZ);
            if (!MovementHelper.fullyPassable(context, destX, y, destZ, destInto)) {
                if (i <= 3 && context.allowParkourAscend && context.canSprint && MovementHelper.canWalkOn(context, destX, y, destZ, destInto) && checkOvershootSafety(context.bsi, destX + xStep, y + 1, destZ + zStep)) {
                    res.x = destX;
                    res.y = y + 1;
                    res.z = destZ;
                    res.cost = i * SPRINT_ONE_BLOCK_COST + context.jumpPenalty;
                    return;
                }
                break;
            }

            BlockState landingOn = context.bsi.get0(destX, y - 1, destZ);
            if ((landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context, destX, y - 1, destZ, landingOn))
                    || (Math.min(16, context.frostWalker + 2) >= i && MovementHelper.canUseFrostWalker(context, landingOn))
            ) {
                if (checkOvershootSafety(context.bsi, destX + xStep, y, destZ + zStep)) {
                    res.x = destX;
                    res.y = y;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(i) + context.jumpPenalty;
                    return;
                }
                break;
            }

            if (!MovementHelper.fullyPassable(context, destX, y + 3, destZ)) {
                break;
            }

            verifiedMaxJump = i;
        }

        // parkour place
        if (!context.allowParkourPlace) {
            return;
        }
        for (int i = verifiedMaxJump; i > 1; i--) {
            int destX = x + i * xStep;
            int destZ = z + i * zStep;
            BlockState toReplace = context.get(destX, y - 1, destZ);
            double placeCost = context.costOfPlacingAt(destX, y - 1, destZ, toReplace);
            if (placeCost >= COST_INF) {
                continue;
            }
            if (!MovementHelper.isReplaceable(destX, y - 1, destZ, toReplace, context.bsi)) {
                continue;
            }
            if (!checkOvershootSafety(context.bsi, destX + xStep, y, destZ + zStep)) {
                continue;
            }
            for (int j = 0; j < 5; j++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getStepX();
                int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getStepY();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getStepZ();
                if (againstX == destX - xStep && againstZ == destZ - zStep) {
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                    res.x = destX;
                    res.y = y;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(i) + placeCost + context.jumpPenalty;
                    return;
                }
            }
        }
    }

    // ──────────────────────────────────────
    //  Diagonal parkour (new)
    // ──────────────────────────────────────

    /**
     * Iterate through all presets whose sign pattern matches xSign / zSign,
     * in increasing difficulty order. For each preset, perform the full set
     * of feasibility checks: corner blocks, intermediate path, head clearance,
     * landing block, and sprint availability.
     */
    private static void costDiagonal(CalculationContext context, int x, int y, int z,
                                      int xSign, int zSign, MutableMoveResult res) {
        if (!context.allowParkour) {
            return;
        }
        if (!context.allowJumpAtBuildLimit && y >= context.world.getMaxBuildHeight()) {
            return;
        }

        // Start-position preconditions (shared with cardinal)
        if (!MovementHelper.fullyPassable(context, x, y + 2, z)) {
            return;
        }
        BlockState standingOn = context.get(x, y - 1, z);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof StairBlock || MovementHelper.isBottomSlab(standingOn)) {
            return;
        }
        if (context.assumeWalkOnWater && !standingOn.getFluidState().isEmpty()) {
            return;
        }
        if (!context.get(x, y, z).getFluidState().isEmpty()) {
            return;
        }

        // Determine max jump distance for sprint gating
        int maxJumpDist;
        if (context.allowWalkOnMagmaBlocks && standingOn.is(Blocks.MAGMA_BLOCK)) {
            maxJumpDist = 2;
        } else if (standingOn.getBlock() == Blocks.SOUL_SAND) {
            maxJumpDist = 2;
        } else if (context.canSprint) {
            maxJumpDist = 4;
        } else {
            maxJumpDist = 3;
        }

        for (ParkourPresets preset : ParkourPresets.values()) {
            // Skip cardinal presets in diagonal mode
            if (!preset.isDiagonal()) {
                continue;
            }
            // Skip if jump exceeds our sprint capability
            if (preset.requiresSprint && !context.canSprint) {
                continue;
            }

            int jumpDx = preset.dx * xSign;
            int jumpDz = preset.dz * zSign;
            int destX = x + jumpDx;
            int destZ = z + jumpDz;

            // ---- Corner block checks ----
            // Both corner positions at landing level and +1 must be passable
            int corner1X = x + jumpDx;
            int corner1Z = z;          // (x+dx, y, z)
            int corner2X = x;
            int corner2Z = z + jumpDz; // (x, y, z+dz)

            if (!MovementHelper.fullyPassable(context, corner1X, y, corner1Z)) {
                continue;
            }
            if (!MovementHelper.fullyPassable(context, corner1X, y + 1, corner1Z)) {
                continue;
            }
            if (!MovementHelper.fullyPassable(context, corner2X, y, corner2Z)) {
                continue;
            }
            if (!MovementHelper.fullyPassable(context, corner2X, y + 1, corner2Z)) {
                continue;
            }

            // ---- Intermediate path: step through each block along the diagonal ----
            int maxSteps = Math.max(Math.abs(jumpDx), Math.abs(jumpDz));
            boolean pathClear = true;
            for (int s = 1; s < maxSteps; s++) {
                // Interpolate to nearest integer position along the path
                int midX = x + (jumpDx * s) / maxSteps;
                int midZ = z + (jumpDz * s) / maxSteps;
                if (!MovementHelper.fullyPassable(context, midX, y, midZ)) {
                    pathClear = false;
                    break;
                }
                if (!MovementHelper.fullyPassable(context, midX, y + 1, midZ)) {
                    pathClear = false;
                    break;
                }
                if (!MovementHelper.fullyPassable(context, midX, y + 2, midZ)) {
                    pathClear = false;
                    break;
                }
            }
            if (!pathClear) {
                continue;
            }

            // ---- Destination head checks ----
            if (!MovementHelper.fullyPassable(context, destX, y + 1, destZ)) {
                continue;
            }
            if (!MovementHelper.fullyPassable(context, destX, y + 2, destZ)) {
                continue;
            }

            BlockState destInto = context.bsi.get0(destX, y, destZ);
            if (!MovementHelper.fullyPassable(context, destX, y, destZ, destInto)) {
                // Ascend landing
                if (preset.dx <= 3 && preset.dz <= 2 && context.allowParkourAscend && context.canSprint
                        && MovementHelper.canWalkOn(context, destX, y, destZ, destInto)) {
                    if (checkOvershootSafety(context.bsi, destX + xSign, y + 1, destZ + zSign)) {
                        res.x = destX;
                        res.y = y + 1;
                        res.z = destZ;
                        res.cost = preset.distance * SPRINT_ONE_BLOCK_COST + context.jumpPenalty;
                        return;
                    }
                }
                continue;
            }

            // ---- Landing check ----
            BlockState landingOn = context.bsi.get0(destX, y - 1, destZ);
            if ((landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context, destX, y - 1, destZ, landingOn))
                    || (Math.min(16, context.frostWalker + 2) >= preset.distance && MovementHelper.canUseFrostWalker(context, landingOn))
            ) {
                if (checkOvershootSafety(context.bsi, destX + xSign, y, destZ + zSign)) {
                    res.x = destX;
                    res.y = y;
                    res.z = destZ;
                    res.cost = preset.distance * SPRINT_ONE_BLOCK_COST + context.jumpPenalty;
                    return;
                }
            }
        }
    }

    // ──────────────────────────────────────
    //  Shared helpers
    // ──────────────────────────────────────

    private static boolean checkOvershootSafety(BlockStateInterface bsi, int x, int y, int z) {
        return !MovementHelper.avoidWalkingInto(bsi.get0(x, y, z)) && !MovementHelper.avoidWalkingInto(bsi.get0(x, y + 1, z));
    }

    private static double costFromJumpDistance(int dist) {
        switch (dist) {
            case 2:
                return WALK_ONE_BLOCK_COST * 2;
            case 3:
                return WALK_ONE_BLOCK_COST * 3;
            case 4:
                return SPRINT_ONE_BLOCK_COST * 4;
            default:
                throw new IllegalStateException("LOL " + dist);
        }
    }

    // ──────────────────────────────────────
    //  Instance methods
    // ──────────────────────────────────────

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult res = new MutableMoveResult();
        if (diagonal) {
            int xSign = Integer.signum(dx);
            int zSign = Integer.signum(dz);
            costDiagonal(context, src.x, src.y, src.z, xSign, zSign, res);
        } else {
            int xStep = Integer.signum(dx);
            int zStep = Integer.signum(dz);
            costCardinal(context, src.x, src.y, src.z, xStep, zStep, res);
        }
        if (res.x != dest.x || res.y != dest.y || res.z != dest.z) {
            return COST_INF;
        }
        return res.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        Set<BetterBlockPos> set = new HashSet<>();
        int maxSteps = Math.max(Math.abs(dx), Math.abs(dz));
        if (diagonal) {
            // Walk diagonal bounding box: for each step along the major axis,
            // insert the interpolated block and the one above it.
            for (int i = 0; i <= maxSteps; i++) {
                int px = src.x + (dx * i) / maxSteps;
                int pz = src.z + (dz * i) / maxSteps;
                set.add(new BetterBlockPos(px, src.y, pz));
                set.add(new BetterBlockPos(px, src.y + 1, pz));
            }
        } else {
            int xStep = Integer.signum(dx);
            int zStep = Integer.signum(dz);
            for (int i = 0; i <= dist; i++) {
                for (int yOff = 0; yOff < 2; yOff++) {
                    set.add(new BetterBlockPos(src.x + xStep * i, src.y + yOff, src.z + zStep * i));
                }
            }
        }
        // Include dest + above
        set.add(new BetterBlockPos(dest.x, dest.y, dest.z));
        set.add(new BetterBlockPos(dest.x, dest.y + 1, dest.z));
        return set;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return state.getStatus() != MovementStatus.RUNNING;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }
        if (ctx.playerFeet().y < src.y) {
            logDebug("sorry");
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        if (dist >= 4 || ascend || (diagonal && dist >= 2)) {
            state.setInput(Input.SPRINT, true);
        }
        if (Baritone.settings().allowWalkOnMagmaBlocks.value && ctx.world().getBlockState(ctx.playerFeet().below()).is(Blocks.MAGMA_BLOCK)) {
            state.setInput(Input.SNEAK, true);
        }

        MovementHelper.moveTowards(ctx, state, dest);
        if (ctx.playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(ctx, dest);
            if (d == Blocks.VINE || d == Blocks.LADDER) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (ctx.player().position().y - ctx.playerFeet().getY() < 0.094) {
                state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (!ctx.playerFeet().equals(src)) {
            if (!diagonal && ctx.playerFeet().equals(new BetterBlockPos(src.x + Integer.signum(dx), src.y, src.z + Integer.signum(dz)))
                    || ctx.player().position().y - src.y > 0.0001) {
                if (Baritone.settings().allowPlace.value
                        && ((Baritone) baritone).getInventoryBehavior().hasGenericThrowaway()
                        && !MovementHelper.canWalkOn(ctx, dest.below())
                        && !ctx.player().isOnGround()
                        && MovementHelper.attemptToPlaceABlock(state, baritone, dest.below(), true, false) == PlaceResult.READY_TO_PLACE
                ) {
                    state.setInput(Input.CLICK_RIGHT, true);
                }
                if (dist == 3 && !ascend) {
                    double xDiff = (src.x + 0.5) - ctx.player().position().x;
                    double zDiff = (src.z + 0.5) - ctx.player().position().z;
                    double distFromStart = Math.max(Math.abs(xDiff), Math.abs(zDiff));
                    if (distFromStart < 0.7) {
                        return state;
                    }
                }

                state.setInput(Input.JUMP, true);
            } else if (!diagonal && !ctx.playerFeet().equals(dest)) {
                BetterBlockPos backOne = new BetterBlockPos(dest.x - Integer.signum(dx), dest.y, dest.z - Integer.signum(dz));
                if (!ctx.playerFeet().equals(backOne)) {
                    state.setInput(Input.SPRINT, false);
                    BetterBlockPos backFromSrc = new BetterBlockPos(src.x - Integer.signum(dx), src.y, src.z - Integer.signum(dz));
                    if (ctx.playerFeet().equals(backFromSrc)) {
                        MovementHelper.moveTowards(ctx, state, src);
                    } else {
                        MovementHelper.moveTowards(ctx, state, backFromSrc);
                    }
                }
            } else if (diagonal && !ctx.playerFeet().equals(dest)) {
                // For diagonal, keep sprinting and jumping toward dest
                state.setInput(Input.JUMP, true);
            }
        }
        return state;
    }
}
