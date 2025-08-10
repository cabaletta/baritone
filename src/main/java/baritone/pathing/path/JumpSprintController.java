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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import baritone.utils.BlockStateInterface;
 

/**
 * Minimal, conservative jump-sprint assist for flat straight segments.
 *
 * Design goals:
 * - Never fight movement classes for sprint control
 * - Never touch rotations (no yaw/pitch steering)
 * - Only emit short JUMP pulses when clearly safe and beneficial
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

    /**
     * Apply jump timing for the current tick. Sprint is handled elsewhere.
     */
    public void apply(PathingBehavior behavior,
                      IPlayerContext ctx,
                      Movement movement,
                      IPath path,
                      int pathPosition) {
        ticks++;
        if (!Baritone.settings().jumpsprint.value) {
            return;
        }

        // Only consider flat straight traverses to avoid corner skew. Skip diagonals and others.
        if (!(movement instanceof MovementTraverse)) {
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
            } else if (pathAdvanced && warmupTilesRemaining > 0) {
                warmupTilesRemaining--;
            }

            // Require at least one more movement straight ahead in the same flat direction,
            // and avoid an immediate ascend/parkour in the same direction (they control their timing).
            IMovement next = (pathPosition < path.length() - 1) ? path.movements().get(pathPosition + 1) : null;
            if (next == null) {
                return;
            }
            int ndx = Integer.signum(((Movement) next).getDest().x - ((Movement) next).getSrc().x);
            int ndz = Integer.signum(((Movement) next).getDest().z - ((Movement) next).getSrc().z);
            boolean sameFlat = dx == ndx && dz == ndz;
            if (!sameFlat) {
                // turning soon; let movement logic handle inputs this tick
                lastPathIndex = pathPosition;
                return;
            }
            if (next instanceof MovementAscend || next instanceof MovementParkour) {
                lastPathIndex = pathPosition;
                return; // let ascend/parkour manage their own jump timing
            }

            // Two-step lookahead to avoid jumping into an imminent corner or ascend/parkour
            if (pathPosition + 2 < path.length()) {
                IMovement next2 = path.movements().get(pathPosition + 2);
                int ndx2 = Integer.signum(((Movement) next2).getDest().x - ((Movement) next2).getSrc().x);
                int ndz2 = Integer.signum(((Movement) next2).getDest().z - ((Movement) next2).getSrc().z);
                boolean sameFlat2 = dx == ndx2 && dz == ndz2;
                if (!sameFlat2 || next2 instanceof MovementAscend || next2 instanceof MovementParkour) {
                    // approaching a turn or ascend/parkour; leave inputs untouched for movement/PathExecutor to manage
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
                return;
            }

            // Engagement conditions
            boolean onGround = ctx.player().onGround();
            boolean nearEnd = (path.length() - pathPosition) <= 3;
            if (nearEnd) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                wasOnGround = onGround;
                lastPathIndex = pathPosition;
                return;
            }

            // Warmup: don't jump yet to stabilize heading
            if (warmupTilesRemaining > 0) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                wasOnGround = onGround;
                lastPathIndex = pathPosition;
                return;
            }

            // Engage: hold jump continuously during straight traverse
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
            ctx.player().setSprinting(true);

            wasOnGround = onGround;
            lastPathIndex = pathPosition;
        } catch (Throwable t) {
            logDebug("JumpSprintController suppressed an exception: " + t);
            behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
        }
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
}

