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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.movements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * One sprint jump along a straight run of path, simulated tick by tick the same way LivingEntity does it
 */
final class SprintJump {

    // straight out of 1.21.4 LivingEntity.travelInAir / jumpFromGround / Player.getFlyingSpeed, aiStep scales move input by 0.98
    static final double GROUND_FRICTION = 0.6 * 0.91, AIR_FRICTION = 0.91, AIR_ACCEL = 0.026 * 0.98, JUMP_BOOST = 0.2;
    private static final double JUMP = 0.42, GRAVITY = 0.08, SAFE_FALL = 3, EPS = 1e-6;
    // a braked hop that lands short is a slow way to walk. ground sprint is ~0.28 a tick, going down steps or bonking one is way worse
    private static final double FLAT_PACE = 0.28, SLOW_PACE = 0.15;
    // ~4 blocks a jump, plus enough past that to see whether the next hop can pop a step
    static final int MAX_RUNWAY = 10, MAX_TICKS = 40;
    private static final double[] MARGINS = {0.85, 1, 1.15}; // we're exact about vanilla, not about what the server does with our yaw
    private static final double BRAKE_THEN_PUSH = Double.NEGATIVE_INFINITY; // not a spot on the line, a throttle policy

    record Landing(double s, double y, double apex, double v, int ticks) {}

    final double[] floors; // relative to the takeoff floor, cell 0 is the one we take off from
    private final double spacing, halfWidth, groundAccel;
    private double originX, originZ, takeoffY, ux, uz, target, apex;
    private boolean pushing;
    int ticks;

    SprintJump(double[] floors, double spacing, double groundAccel) {
        this.floors = floors;
        this.spacing = spacing;
        this.halfWidth = 0.3 * spacing; // an axis aligned box projected onto a diagonal reaches sqrt 2 times further
        this.groundAccel = groundAccel;
    }

    static SprintJump plan(IPlayerContext ctx, IPath path, int pathPosition) {
        Player player = ctx.player();
        BetterBlockPos start = path.positions().get(pathPosition);
        BlockPos dir = path.movements().get(pathPosition).getDirection();
        boolean diagonal = dir.getX() != 0 && dir.getZ() != 0;
        // the sim knows nothing about potions or modded physics, and jump boost off a hill is how you break your legs.
        // the head tops out at y + 3.05, so y + 3 has to be clear. a ceiling at y + 2 is a head hitter, not ours
        if (!player.onGround() || !player.isSprinting() || player.isInWater() || player.isInLava() || player.onClimbable()
                || !ctx.playerFeet().equals(start) || Math.abs(player.getY() - start.y) > 1e-3
                || diagonal && !Baritone.settings().sprintJumpingDiagonals.value
                || player.hasEffect(MobEffects.JUMP) || player.hasEffect(MobEffects.SLOW_FALLING) || player.hasEffect(MobEffects.LEVITATION)
                || Math.abs(player.getAttributeValue(Attributes.JUMP_STRENGTH) - JUMP) > EPS || player.getAttributeValue(Attributes.GRAVITY) != GRAVITY
                || player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) < SAFE_FALL
                || !solidFloor(ctx, start.below()) || !clear(ctx, start.x, start.y, start.z, start.y + 3)) {
            return null;
        }
        double[] floors = new double[MAX_RUNWAY + 1];
        int n = 1;
        for (int i = pathPosition; i < path.movements().size() && n <= MAX_RUNWAY; i++, n++) {
            Movement m = (Movement) path.movements().get(i);
            BetterBlockPos src = m.getSrc();
            BetterBlockPos dest = m.getDest();
            int top = Math.max(dest.y + 2, start.y + 3); // we fly over lower cells at takeoff height
            boolean walking = diagonal ? m instanceof MovementDiagonal && dest.y == src.y
                    : m instanceof MovementTraverse || m instanceof MovementAscend || m instanceof MovementDescend;
            // breaking is like 5x slower when you're jumping, and momentum doesn't wait for a block to be placed.
            // on a diagonal the hitbox corners sweep through both side cells, and drift can land us in one
            if (!walking || dest.x - src.x != dir.getX() || dest.z - src.z != dir.getZ()
                    || m.toBreakCached == null || !m.toBreakCached.isEmpty() || m.toPlaceCached == null || !m.toPlaceCached.isEmpty()
                    || !solidFloor(ctx, dest.below()) || !clear(ctx, dest.x, dest.y, dest.z, top)
                    || diagonal && !(side(ctx, src.x + dir.getX(), src.y, src.z, top) && side(ctx, src.x, src.y, src.z + dir.getZ(), top))) {
                break;
            }
            floors[n] = dest.y - start.y;
        }
        double spacing = diagonal ? Math.sqrt(2) : 1;
        // solidFloor pins friction to 0.6, so the 0.216 / f^3 in the ground acceleration is 1
        SprintJump jump = new SprintJump(Arrays.copyOf(floors, n), spacing, player.getSpeed() * 0.98);
        jump.originX = start.x + 0.5;
        jump.originZ = start.z + 0.5;
        jump.takeoffY = start.y;
        jump.ux = dir.getX() / spacing;
        jump.uz = dir.getZ() / spacing;
        Vec3 pos = player.position().subtract(jump.originX, 0, jump.originZ);
        Vec3 vel = player.getDeltaMovement();
        // drifting sideways is how you clip a corner
        if (n < 3 || Math.abs(jump.across(pos)) > 0.2 || Math.abs(jump.across(vel)) > 0.08) {
            return null;
        }
        Double target = jump.plan(jump.along(pos), jump.along(vel));
        if (target == null) {
            return null;
        }
        jump.target = target;
        return jump;
    }

    private static boolean solidFloor(IPlayerContext ctx, BlockPos pos) {
        BlockState state = ctx.world().getBlockState(pos);
        // ice/slime change friction, soul sand/honey/farmland aren't full height, and hopping on magma without sneaking hurts
        return state.getBlock().getFriction() == 0.6F && state.getBlock().getSpeedFactor() == 1.0F && state.getBlock().getJumpFactor() == 1.0F
                && !state.is(Blocks.MAGMA_BLOCK) && state.isCollisionShapeFullBlock(ctx.world(), pos);
    }

    private static boolean side(IPlayerContext ctx, int x, int y, int z, int top) {
        return solidFloor(ctx, new BlockPos(x, y - 1, z)) && clear(ctx, x, y, z, top);
    }

    private static boolean clear(IPlayerContext ctx, int x, int y, int z, int top) {
        for (int yy = y; yy <= top; yy++) {
            if (!MovementHelper.fullyPassable(ctx, new BlockPos(x, yy, z))) {
                return false;
            }
        }
        return true;
    }

    private double along(Vec3 vec) {
        return vec.x * ux + vec.z * uz;
    }

    private double across(Vec3 vec) {
        return vec.z * ux - vec.x * uz;
    }

    /**
     * Full send (NaN), brake to land around a distance along the line, or don't jump (null)
     */
    Double plan(double s, double v) {
        // a hop that lands right in front of a step leaves the next one nothing to do but bonk the face, lose sprint,
        // and hand it to MovementAscend. so with a step ahead, pick a hop the next one can pop it from, or sprint
        // a tick or two more if that sets one up
        boolean climb = stepUpAhead(0);
        Double chained = climb ? choose(s, v, MARGINS, true) : null;
        return chained != null || climb && canHop(s, v, MARGINS, true, 1) ? chained : choose(s, v, MARGINS, false);
    }

    private Double choose(double s, double v, double[] margins, boolean chain) {
        if (works(s, v, margins, Double.NaN, chain)) {
            return Double.NaN;
        }
        // full send flies too far (over the first step of a hill and down the second) or sets up a bonk. braking only
        // ever shortens a hop, so nothing past where full send lands
        Landing full = jump(s, v, Double.NaN);
        for (int k = floors.length - 1; k >= 1; k--) {
            if ((full == null || k * spacing <= full.s() + spacing / 2) && works(s, v, margins, k * spacing, chain)) {
                return k * spacing;
            }
        }
        return null;
    }

    private boolean works(double s, double v, double[] margins, double target, boolean chain) {
        for (double margin : margins) {
            Landing l = jump(s, v * margin, target);
            if (!safe(l) || !Double.isNaN(target) && l.s() - s < (l.y() < -EPS || chain ? SLOW_PACE : FLAT_PACE) * l.ticks()) {
                return false;
            }
        }
        return !chain || chains(jump(s, v, target));
    }

    private boolean chains(Landing l) {
        int cell = (int) Math.round(l.s() / spacing);
        if (!stepUpAhead(cell)) {
            return true;
        }
        double[] next = new double[floors.length - cell];
        Arrays.setAll(next, i -> floors[cell + i] - l.y());
        return new SprintJump(next, spacing, groundAccel).canHop(l.s() - cell * spacing, l.v(), new double[]{1}, false, 0);
    }

    private boolean canHop(double s, double v, double[] margins, boolean chain, int from) {
        for (int wait = 0; wait <= 2 && highest(s) <= EPS; wait++) {
            if (wait >= from && choose(s, v, margins, chain) != null) {
                return true;
            }
            s += v + groundAccel;
            v = (v + groundAccel) * GROUND_FRICTION;
        }
        return false;
    }

    private boolean stepUpAhead(int cell) {
        for (int k = cell + 1; k < floors.length; k++) {
            if (floors[k] > floors[cell] + EPS) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return null if we hit a wall, leave the runway, or never come down
     */
    Landing jump(double s, double v, double target) {
        return fly(s, v + JUMP_BOOST, 0, JUMP, 0, true, target);
    }

    private Landing fly(double s, double v, double y, double vy, double apex, boolean jumping, double target) {
        boolean committed = false;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            // onGround is still true on the jump tick, so it gets ground acceleration and ground friction
            boolean jumpTick = jumping && tick == 0;
            int throttle = jumpTick ? 0 : target == BRAKE_THEN_PUSH ? (tick == 0 ? -1 : 1) : throttle(s, v, y, vy, apex, target, committed);
            committed = throttle > 0;
            double disp = v + (jumpTick ? groundAccel : AIR_ACCEL * throttle);
            // vanilla collides y before x/z, so we're already as high as we're going to get this tick when we move forward
            y += vy;
            apex = Math.max(apex, y);
            boolean landed = vy < 0 && y <= highest(s) + EPS;
            y = landed ? highest(s) : y;
            s += disp;
            if (highest(s) > y + EPS) {
                return null; // face first into a step, sprint gets cancelled and we just fall back down
            }
            if (landed) {
                return new Landing(s, y, apex, disp * AIR_FRICTION, tick + 1);
            }
            v = disp * (jumpTick ? GROUND_FRICTION : AIR_FRICTION);
            vy = (vy - GRAVITY) * 0.98;
        }
        return null;
    }

    /**
     * Hold W facing forward (1) or backward (-1, brakes without losing sprint)
     */
    private int throttle(double s, double v, double y, double vy, double apex, double target, boolean committed) {
        if (Double.isNaN(target) || committed) {
            return 1;
        }
        // brake first, push last. same landing spot, but we come down moving, which is what the next hop needs to pop a
        // step. brake while braking now and pushing after still gets there, then push for good: one flip, no chatter
        Landing late = fly(s, v, y, vy, apex, false, BRAKE_THEN_PUSH);
        return late == null || late.s() >= target ? -1 : 1;
    }

    boolean safe(Landing l) {
        if (l == null || l.apex() - l.y() >= SAFE_FALL) {
            return false; // small hills only
        }
        int cell = (int) Math.round(l.s() / spacing);
        // the lip of a step puts our feet in a block that isn't on the path. and we slide after landing (v / (1 - 0.546)
        // with W let go), which has to end over the runway too in case the path turns right after it
        return cell >= 0 && cell < floors.length && Math.abs(floors[cell] - l.y()) < EPS
                && highest(l.s() + Math.max(0, l.v()) / (1 - GROUND_FRICTION)) != Double.POSITIVE_INFINITY;
    }

    /**
     * Tallest floor under a hitbox centered at s. Past the runway is a wall so the whole flight stays over checked cells,
     * behind it is where we came from (a hard braked prediction can drift back there)
     */
    private double highest(double s) {
        double top = Double.NEGATIVE_INFINITY;
        for (int k = (int) Math.floor((s - halfWidth) / spacing - 0.5) + 1; k <= Math.ceil((s + halfWidth) / spacing + 0.5) - 1; k++) {
            if (k >= floors.length) {
                return Double.POSITIVE_INFINITY;
            }
            top = Math.max(top, floors[Math.max(k, 0)]);
        }
        return top;
    }

    /**
     * Which way to face this tick: forward on takeoff so the jump boost goes down the line, then push or brake,
     * tilted to kill sideways drift
     */
    Rotation steer(IPlayerContext ctx) {
        Vec3 pos = ctx.player().position().subtract(originX, 0, originZ);
        Vec3 vel = ctx.player().getDeltaMovement();
        int throttle = 1;
        double accel = JUMP_BOOST + groundAccel;
        if (ticks++ > 0) {
            apex = Math.max(apex, pos.y - takeoffY);
            throttle = throttle(along(pos), along(vel), pos.y - takeoffY, vel.y, apex, target, pushing);
            pushing = throttle > 0;
            accel = AIR_ACCEL;
        }
        // aiming at a point on the line ignores how fast we're already drifting toward it, so we'd sail past and swing
        // back forever (the diagonal wiggle). ask for a sideways speed that shrinks with the offset instead
        double side = Math.max(-0.5, Math.min(0.5, (-across(pos) * 0.3 - across(vel)) / accel));
        double forward = throttle * Math.sqrt(1 - side * side);
        float yaw = (float) Math.toDegrees(Math.atan2(uz * forward + ux * side, ux * forward - uz * side)) - 90;
        return new Rotation(yaw, ctx.playerRotations().getPitch());
    }
}
