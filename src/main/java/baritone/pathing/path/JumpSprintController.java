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

package baritone.pathing.path;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.behavior.PathingBehavior;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.movements.MovementAscend;
import baritone.pathing.movement.movements.MovementTraverse;
import baritone.pathing.movement.movements.MovementDiagonal;
import baritone.pathing.movement.movements.MovementParkour;
import baritone.pathing.movement.movements.MovementDescend;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import baritone.utils.BlockStateInterface;
 

/**
 * Minimal, conservative jump-sprint assist for flat straight segments.
 */
public final class JumpSprintController implements Helper {

    // cadence guard so we don't spam jump multiple ticks in a row
    private int ticks;
    private int lastJumpPulseTick = -1000;
    private boolean wasOnGround;

    // warmup for trajectory stabilization on straight runs
    private static final int WARMUP_TILES = 2;
    private int straightDx = 0;
    private int straightDz = 0;
    private int warmupTilesRemaining = 0;
    private int lastPathIndex = -1;
    private static final int STABILIZE_TICKS_AFTER_TURN = 4;
    private int stabilizationTicksRemaining = 0;
    private String lastDebugNote = null;
    private static final int DEBUG_MIN_INTERVAL_TICKS = 20; // limit logs to ~once per second
    private int lastDebugTick = -100000;

    /**
     * Apply jump timing for the current tick. Sprint is handled elsewhere.
     */
    public void apply(PathingBehavior behavior,
                      IPlayerContext ctx,
                      Movement movement,
                      IPath path,
                      int pathPosition) {
        ticks++;
        if (!Baritone.settings().allowJumpSprint.value) {
            return;
        }

        // Consider flat straight traverses and long flat diagonals. Skip other movement types.
        boolean diagonalAllowed = Baritone.settings().allowJumpSprintDiagonal.value;
        if (!(movement instanceof MovementTraverse || (diagonalAllowed && movement instanceof MovementDiagonal))) {
            return;
        }

        try {
            // Don't interfere while interacting with blocks or in liquids
            if (MovementHelper.isLiquid(ctx, ctx.playerFeet())) {
                return;
            }
            BlockStateInterface bsi = new BlockStateInterface(ctx);
            if (!((Movement) movement).toBreak(bsi).isEmpty() || !((Movement) movement).toPlace(bsi).isEmpty()) {
                return;
            }

            // Aggressive low-ceiling boost: if headroom is blocked right now, spam jump while keeping sprint+forward
            if (Baritone.settings().allowHeadHits.value) {
                try {
                    BetterBlockPos headNow = ctx.playerFeet().above();
                    boolean lowCeilingNow = MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, headNow))
                            || MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, headNow.above()));
                    if (lowCeilingNow) {
                        behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                        behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                        behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                        debugOnce("jumpsprint: ceiling mash");
                        lastPathIndex = pathPosition;
                        return;
                    }
                } catch (Throwable ignored) {}
            }

            int dx = Integer.signum(movement.getDest().x - movement.getSrc().x);
            int dy = Integer.signum(movement.getDest().y - movement.getSrc().y);
            int dz = Integer.signum(movement.getDest().z - movement.getSrc().z);
            if (dy != 0) {
                return; // not flat
            }

            // Always keep sprint and forward held during traverse to prevent midair de-sprint and deceleration
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
            ctx.player().setSprinting(true);

            // Track straight run and apply warmup across tiles to stabilize trajectory
            boolean directionChanged = (dx != straightDx) || (dz != straightDz);
            boolean pathAdvanced = (lastPathIndex != -1 && pathPosition != lastPathIndex);
            if (directionChanged) {
                straightDx = dx;
                straightDz = dz;
                warmupTilesRemaining = WARMUP_TILES;
                stabilizationTicksRemaining = STABILIZE_TICKS_AFTER_TURN; // buffer after turn/diagonal change
                debugOnce("jumpsprint: direction changed, warmup+stabilize");
            } else if (pathAdvanced && warmupTilesRemaining > 0) {
                warmupTilesRemaining--;
            }
            if (pathAdvanced && stabilizationTicksRemaining > 0) {
                stabilizationTicksRemaining--;
            }

            // Require at least one more movement straight ahead in the same flat direction,
            // and avoid an upcoming ascend/parkour/turn within a short horizon (they control their timing).
            IMovement next = (pathPosition < path.length() - 1) ? path.movements().get(pathPosition + 1) : null;
            if (next == null) {
                return;
            }
            int ndx = Integer.signum(((Movement) next).getDest().x - ((Movement) next).getSrc().x);
            int ndz = Integer.signum(((Movement) next).getDest().z - ((Movement) next).getSrc().z);
            boolean sameFlat = dx == ndx && dz == ndz;
            if (!sameFlat) {
                // turning soon; let movement logic handle inputs this tick
                debugOnce("jumpsprint: suppressed (turn next)");
                lastPathIndex = pathPosition;
                return;
            }
            if (next instanceof MovementAscend || next instanceof MovementParkour) {
                debugOnce("jumpsprint: suppressed (ascend/parkour next)");
                lastPathIndex = pathPosition;
                return; // let ascend/parkour manage their own jump timing
            }

            // Horizon suppression: if an ascend/parkour or flat turn exists within 4 moves, don't jump-sprint; just regular sprint
            if (shouldSuppressForHorizon(path, pathPosition, dx, dz)) {
                // keep sprint and forward, but no jump this tick
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                debugOnce("jumpsprint: suppressed (horizon)");
                lastPathIndex = pathPosition;
                return;
            }

            // Elevation safety: analyze upcoming elevation to decide whether jump-sprint is beneficial
            ElevationEval eval = evaluateElevationHorizon(ctx, path, pathPosition, dx, dz);
            // If we are approaching a long climb (lots of ascends), jump-sprint provides little benefit and may destabilize
            if (eval.ascendHeavy) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                debugOnce("jumpsprint: suppressed (ascend-heavy)");
                lastPathIndex = pathPosition;
                return;
            }
            // If a dangerous drop is imminent (risk of fall damage), suppress jump-sprint to avoid overshooting the ledge
            if (eval.dangerousDropSoon) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                debugOnce("jumpsprint: suppressed (dangerous drop)");
                lastPathIndex = pathPosition;
                    return;
            }
            // If a small safe descend is next, allow downhill jump-sprint; ensure sprint held midair is already handled below

            // Two-step lookahead to avoid jumping into an imminent corner or ascend/parkour
            if (pathPosition + 2 < path.length()) {
                IMovement next2 = path.movements().get(pathPosition + 2);
                int ndx2 = Integer.signum(((Movement) next2).getDest().x - ((Movement) next2).getSrc().x);
                int ndz2 = Integer.signum(((Movement) next2).getDest().z - ((Movement) next2).getSrc().z);
                boolean sameFlat2 = dx == ndx2 && dz == ndz2;
                if (!sameFlat2 || next2 instanceof MovementAscend || next2 instanceof MovementParkour) {
                    // approaching a turn or ascend/parkour; leave inputs untouched for movement/PathExecutor to manage
                    debugOnce("jumpsprint: suppressed (2-ahead)");
                    lastPathIndex = pathPosition;
                    return;
                }
            }

            // If we're wildly off-center relative to the current segment, don't risk skewing
            BetterBlockPos anchor = path.positions().get(pathPosition);
            double lateralOffset = lateralOffsetFromCenterXZ(ctx, anchor, dx, dz);
            if (lateralOffset > 0.28) { // roughly quarter-block tolerance
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                wasOnGround = ctx.player().onGround();
                debugOnce("jumpsprint: suppressed (off-center)");
                return;
            }

            // Engagement conditions
            boolean onGround = ctx.player().onGround();
            boolean nearEnd = (path.length() - pathPosition) <= 3;
            if (nearEnd) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                wasOnGround = onGround;
                debugOnce("jumpsprint: suppressed (near end)");
                lastPathIndex = pathPosition;
                return;
            }

            // Warmup & stabilization: don't jump yet to stabilize heading and lateral velocity
            double vx = ctx.player().getDeltaMovement().x;
            double vz = ctx.player().getDeltaMovement().z;
            double perpVel = Math.abs(vx * (-dz) + vz * dx); // velocity perpendicular to our travel axis
            boolean lateralVelocityHigh = perpVel > 0.035; // small threshold to avoid oscillation
            if (warmupTilesRemaining > 0 || stabilizationTicksRemaining > 0 || lateralVelocityHigh) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                wasOnGround = onGround;
                if (warmupTilesRemaining > 0) debugOnce("jumpsprint: warmup");
                else if (stabilizationTicksRemaining > 0) debugOnce("jumpsprint: stabilize after turn");
                else debugOnce("jumpsprint: waiting (lateral vel)");
                lastPathIndex = pathPosition;
                return;
            }

            // Headroom check: avoid head-bonking into low ceilings (trees/leaves/etc.) in the next few tiles
            if (!headroomClear(ctx, path, pathPosition, 3)) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                debugOnce("jumpsprint: suppressed (low headroom ahead)");
                lastPathIndex = pathPosition;
                return;
            }

            // For diagonals, preserve midair momentum by not toggling jump while airborne; only ensure sprint
            if (movement instanceof MovementDiagonal) {
                if (!onGround) {
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                    debugOnce("jumpsprint: diagonal air preserve");
                    lastPathIndex = pathPosition;
                    return;
                }
            }

            // Engage: hold jump continuously during traverse/diagonal on ground
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
            ctx.player().setSprinting(true);
            debugOnce("jumpsprint: ENGAGE");

            // Ceiling-boost: if there is a low ceiling directly above while sprinting, pulse jump to gain micro speed bursts
            try {
                BetterBlockPos head = ctx.playerFeet().above();
                boolean lowCeiling = MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, head))
                        || MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, head.above()));
                if (lowCeiling) {
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                }
            } catch (Throwable ignored) {
            }

            wasOnGround = onGround;
            lastPathIndex = pathPosition;
        } catch (Throwable t) {
            logDebug("JumpSprintController suppressed an exception: " + t);
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
        }
    }

    private void debugOnce(String note) {
        if (note == null) {
            return;
        }
        // Rate limit: only log if enough ticks passed since last log
        if ((ticks - lastDebugTick) < DEBUG_MIN_INTERVAL_TICKS) {
            return;
        }
        // Log even if same note (but still rate-limited)
        logDebug(note);
        lastDebugNote = note;
        lastDebugTick = ticks;
    }

    private static double lateralOffsetFromCenterXZ(IPlayerContext ctx, BetterBlockPos anchor, int dx, int dz) {
        // Compute lateral offset from the center line of travel within the current tile
        double px = ctx.player().position().x;
        double pz = ctx.player().position().z;
        double cx = anchor.x + 0.5;
        double cz = anchor.z + 0.5;
        // Perpendicular unit (not normalized but consistent for signum components)
        double perpX = -dz;
        double perpZ = dx;
        double vx = px - cx;
        double vz = pz - cz;
        double lateral = Math.abs(vx * perpX + vz * perpZ);
        return lateral;
    }

    private static boolean shouldSuppressForHorizon(IPath path, int pathPosition, int dx, int dz) {
        // Look up to 4 moves ahead for:
        // - Ascend/parkour in same flat direction (we want to sprint into it, not jump-sprint before)
        // - Any flat direction change (turn), to avoid engaging too close to corners
        int maxAhead = 4;
        for (int ahead = 1; ahead <= maxAhead && pathPosition + ahead < path.length(); ahead++) {
            IMovement m = path.movements().get(pathPosition + ahead);
            int adx = Integer.signum(((Movement) m).getDest().x - ((Movement) m).getSrc().x);
            int adz = Integer.signum(((Movement) m).getDest().z - ((Movement) m).getSrc().z);
            boolean sameFlat = dx == adx && dz == adz;
            if (!sameFlat) {
                return true; // turn coming up; skip jump-sprint
            }
            if (m instanceof MovementAscend || m instanceof MovementParkour) {
                return true; // step-up/jump coming soon; prefer regular sprint approach
            }
        }
        return false;
    }

    private boolean headroomClear(IPlayerContext ctx, IPath path, int pathPosition, int tilesAhead) {
        int maxIdx = Math.min(path.positions().size() - 1, pathPosition + tilesAhead);
        for (int i = pathPosition + 1; i <= maxIdx; i++) {
            BetterBlockPos p = path.positions().get(i);
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, p.above()))) {
                return false;
            }
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, p.above(2)))) {
                return false;
            }
        }
        return true;
    }

    private static final class ElevationEval {
        final boolean ascendHeavy;
        final boolean dangerousDropSoon;
        ElevationEval(boolean ascendHeavy, boolean dangerousDropSoon) {
            this.ascendHeavy = ascendHeavy;
            this.dangerousDropSoon = dangerousDropSoon;
        }
    }

    private ElevationEval evaluateElevationHorizon(IPlayerContext ctx, IPath path, int pathPosition, int dx, int dz) {
        // Look ahead up to N positions to measure cumulative ascents and first significant drop
        final int horizon = Math.min(8, path.length() - pathPosition - 1);
        if (horizon <= 0) {
            return new ElevationEval(false, false);
        }
        int ascends = 0;
        int cumulativeClimb = 0;
        int firstDropMagnitude = 0;
        BetterBlockPos prev = path.positions().get(pathPosition);
        for (int i = 1; i <= horizon; i++) {
            BetterBlockPos cur = path.positions().get(pathPosition + i);
            int dy = cur.y - prev.y;
            if (dy > 0) {
                cumulativeClimb += dy;
                ascends++;
            } else if (dy < 0 && firstDropMagnitude == 0) {
                firstDropMagnitude = -dy; // positive magnitude
            }
            prev = cur;
        }

        // Ascend-heavy if we climb at least 3 blocks within horizon or more than half of upcoming moves are ascends
        boolean ascendHeavy = cumulativeClimb >= 3 || ascends >= Math.max(2, horizon / 2);

        // Dangerous drop if drop magnitude exceeds allowed no-water fall height
        boolean dangerousDropSoon = false;
        if (firstDropMagnitude > 0) {
            int safeNoWater = Baritone.settings().maxFallHeightNoWater.value + 1; // +1 because landing block reduces effective fall
            if (firstDropMagnitude > safeNoWater) {
                // Check if landing in water would mitigate
                BetterBlockPos landing = path.positions().get(Math.min(pathPosition + 1 + firstDropMagnitude, path.positions().size() - 1));
                if (!MovementHelper.isWater(BlockStateInterface.get(ctx, landing))) {
                    dangerousDropSoon = true;
                }
            }
        }
        return new ElevationEval(ascendHeavy, dangerousDropSoon);
    }
}

