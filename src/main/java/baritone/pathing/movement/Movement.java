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

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.IMovement;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.behavior.PathingBehavior;
import baritone.utils.BlockStateInterface;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.AABB;

public abstract class Movement implements IMovement, MovementHelper {

    public static final Direction[] HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

    protected final IBaritone baritone;
    protected final IPlayerContext ctx;

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);

    protected final BetterBlockPos src;

    protected final BetterBlockPos dest;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    protected final BetterBlockPos[] positionsToBreak;

    /**
     * The position where we need to place a block before this movement can ensue
     */
    protected final BetterBlockPos positionToPlace;

    private Double cost;

    public List<BlockPos> toBreakCached = null;
    public List<BlockPos> toPlaceCached = null;
    public List<BlockPos> toWalkIntoCached = null;

    private Set<BetterBlockPos> validPositionsCached = null;

    private Boolean calculatedWhileLoaded;

    protected Movement(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest, BetterBlockPos[] toBreak, BetterBlockPos toPlace) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionToPlace = toPlace;
    }

    protected Movement(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest, BetterBlockPos[] toBreak) {
        this(baritone, src, dest, toBreak, null);
    }

    public double getCost() throws NullPointerException {
        return cost;
    }

    public double getCost(CalculationContext context) {
        if (cost == null) {
            cost = calculateCost(context);
        }
        return cost;
    }

    public abstract double calculateCost(CalculationContext context);

    public double recalculateCost(CalculationContext context) {
        cost = null;
        return getCost(context);
    }

    public void override(double cost) {
        this.cost = cost;
    }

    protected abstract Set<BetterBlockPos> calculateValidPositions();

    public Set<BetterBlockPos> getValidPositions() {
        if (validPositionsCached == null) {
            validPositionsCached = calculateValidPositions();
            Objects.requireNonNull(validPositionsCached);
        }
        return validPositionsCached;
    }

    protected boolean playerInValidPosition() {
        return getValidPositions().contains(ctx.playerFeet()) || getValidPositions().contains(((PathingBehavior) baritone.getPathingBehavior()).pathStart());
    }

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    @Override
    public MovementStatus update() {
        ctx.player().getAbilities().flying = false;
        currentState = updateState(currentState);
        if (MovementHelper.isLiquid(ctx, ctx.playerFeet()) || ctx.player().isInWater()) {
            if (shouldSwim()) {
                // sprint is what enters and keeps the swim state. vanilla only STARTS sprinting from
                // the key while on the ground or eye-underwater, and actively cancels it at the water
                // surface (which is exactly where we used to bob), so arm the flag ourselves.
                // MixinEntity.allowSwimStart then latches the swim state this same tick instead of
                // waiting for gravity to pull the eye under
                currentState.setInput(Input.SPRINT, true);
                ctx.player().setSprinting(true);
                if (currentState.getStatus() == MovementStatus.RUNNING && ctx.player().isSwimming() && !ctx.player().onGround()) {
                    holdWaterline();
                    preTurnIntoTheBend();
                } else if (!ctx.player().isSwimming() && ctx.player().onGround() && ctx.player().position().y < dest.y + 0.6) {
                    // swim state hasn't latched yet and we're on the bottom: the old bob, for one tick
                    currentState.setInput(Input.JUMP, true);
                }
            } else {
                if (Baritone.settings().allowSwimming.value && ctx.player().isSwimming()) {
                    // standing up to breathe (see shouldSwim). sprintInWater is on by default and traverse
                    // requests sprint every tick, which would keep us crawling along the bottom with our
                    // head under, so drop it explicitly
                    currentState.setInput(Input.SPRINT, false);
                    ctx.player().setSprinting(false);
                }
                if (ctx.player().position().y < dest.y + 0.6) {
                    currentState.setInput(Input.JUMP, true);
                }
            }
        }
        if (ctx.player().isInWall()) {
            ctx.getSelectedBlock().ifPresent(pos -> MovementHelper.switchToBestToolFor(ctx, BlockStateInterface.get(ctx, pos)));
            currentState.setInput(Input.CLICK_LEFT, true);
        }

        // If the movement target has to force the new rotations, or we aren't using silent move, then force the rotations
        currentState.getTarget().getRotation().ifPresent(rotation ->
                baritone.getLookBehavior().updateTarget(
                        rotation,
                        currentState.getTarget().hasToForceRotations()));
        baritone.getInputOverrideHandler().clearAllKeys();
        currentState.getInputStates().forEach((input, forced) -> {
            baritone.getInputOverrideHandler().setInputForceState(input, forced);
        });
        currentState.getInputStates().clear();

        // If the current status indicates a completed movement
        if (currentState.getStatus().isComplete()) {
            baritone.getInputOverrideHandler().clearAllKeys();
        }

        return currentState.getStatus();
    }

    /**
     * Where the feet sit below the water surface while swimming along it. The swim pose puts the eye 0.4
     * above the feet and vanilla checks the eye 1/9 below where it really is, so the eye is out of the water
     * (air refills) at any depth below 0.289; the body stays in water (swim state and sprint speed hold)
     * at any depth above 0. This is the middle of that band: head out, shoulders in, as much margin as
     * possible each way
     */
    private static final double WATERLINE_DEPTH = 0.15;

    /**
     * How far above a destination's block the feet aim when that would be higher than the waterline, i.e.
     * climbing out onto land. Below the waterline (a destination on the water itself, or a shore at the
     * same level that step assist handles) the waterline wins
     */
    private static final double DEST_CLEARANCE = 0.3;

    /**
     * Air ticks (of the 300 max) at which a swimmer crawling along the bottom of shallow water stands up to
     * breathe: 5 seconds of margin before drowning damage starts
     */
    private static final int SURFACE_AT_AIR = 100;

    // vanilla swim physics per tick, from LivingEntity.jumpInLiquid, Player.travel and LivingEntity.travel:
    // holding jump adds SWIM_JUMP_BOOST, then vertical speed is pulled toward the look vector's y by
    // SWIM_PULL (SWIM_PULL_STEEP when looking down steeper than SWIM_STEEP_LOOK), then it's scaled by
    // WATER_DRAG_Y. sprinting in water has no gravity at all, which is why swimmers float
    private static final double SWIM_JUMP_BOOST = 0.04;
    private static final double SWIM_PULL = 0.06;
    private static final double SWIM_PULL_STEEP = 0.085;
    private static final double SWIM_STEEP_LOOK = -0.2;
    private static final double WATER_DRAG_Y = 0.8;

    /**
     * Wanted vertical speed per block of height error, and per unit of current vertical speed (damping).
     * Tuned against the vanilla numbers above: no overshoot from any depth, settles within a second, and a
     * couple of degrees of random look offset moves the waterline by about a hundredth of a block
     */
    private static final double WATERLINE_GAIN = 0.3;
    private static final double WATERLINE_DAMPING = 0.9;

    // coast distance in blocks, per block per tick of speed, at water drag 0.8: 1 / (1 - 0.8).
    // the early turn starts when the corner is closer than this
    private static final double WATER_COAST_TICKS = 5;

    /**
     * Holding sprint is what keeps the vanilla swim state alive, so swimming is only on the table when
     * we're actually in water, sprinting is allowed, and we have the hunger to sprint
     */
    private boolean shouldSwim() {
        if (!Baritone.settings().allowSwimming.value
                || !ctx.player().isInWater()
                || !Baritone.settings().allowSprint.value
                || ctx.player().getFoodData().getFoodLevel() <= 6) {
            return false;
        }
        if (currentState.getStatus() == MovementStatus.PREPPING) {
            // something's in the way and prepared() is mining it. the swim pose is the worst place to do
            // that from: vanilla mines 5x slower off the ground and 5x slower again with the eye under,
            // and prepared() owns the pitch to aim at the block so holdWaterline can't keep the eye out.
            // stand up instead: eye out bobbing at the surface, full speed on the bottom of the shallows
            return false;
        }
        if (!ctx.player().onGround()) {
            // floating: holdWaterline keeps the eye out, so air is never a problem out here
            return true;
        }
        // shallow water: the swim pose crawls along the bottom with the eye under, so air is a clock.
        // crawl until it runs low, then stand up (which is what surfaces us here, pitch can't) and stay
        // standing until it's actually full, otherwise we'd get one tick of air and go right back under.
        // a swimmer just brushing the bottom at the shore keeps swimming, its eye is already out
        int air = ctx.player().getAirSupply();
        if (ctx.player().isUnderWater()) {
            return air >= SURFACE_AT_AIR;
        }
        return ctx.player().isSwimming() || air >= ctx.player().getMaxAirSupply();
    }

    /**
     * Sit exactly on the waterline while swimming: head in the air so we never drown, body in the water
     * so we keep the swim state and its sprint speed.
     * <p>
     * The trick is that with jump held, vanilla's vertical speed settles at a value set purely by the
     * look pitch (about 25 degrees down hovers, level rises, steeper down sinks), so pitch is a smooth
     * throttle instead of the on/off jump key. Cutting the jump key at a depth band is what used to
     * launch us out of the water and bob us back in: the momentum after a jump assisted rise carries four
     * times the last tick's speed
     */
    private void holdWaterline() {
        double feetY = ctx.player().position().y;
        double surfaceY = feetY + ctx.player().getFluidHeight(FluidTags.WATER);
        // the waterline, unless the movement is taking us higher (climbing out onto land)
        double targetY = Math.max(surfaceY - WATERLINE_DEPTH, dest.y + DEST_CLEARANCE);
        double wanted = WATERLINE_GAIN * (targetY - feetY) - WATERLINE_DAMPING * ctx.player().getDeltaMovement().y;
        float pitch = pitchForSwimVelocity(wanted);
        if (pitch == ctx.playerRotations().getPitch()) {
            // same convention as RotationUtils.reachable: equal to the current pitch means "don't care"
            pitch += 0.0001F;
        }
        final float swimPitch = pitch;
        currentState.getTarget().getRotation().ifPresent(rotation ->
                currentState.setTarget(new MovementState.MovementTarget(rotation.withPitch(swimPitch), false))
        );
        currentState.setInput(Input.JUMP, true);
    }

    /**
     * Water momentum coasts half a block past a bend, taking the 0.6 wide hitbox with it. braking
     * would be the honest fix but vanilla cancels sprint swim the moment W stops counting as forward
     * (LocalPlayer.aiStep), which flickers the whole swim state. so turn early instead: aim at the
     * block after the corner once the coast would reach it, and the momentum is the entry angle
     */
    private void preTurnIntoTheBend() {
        if (!MovementHelper.isWater(BlockStateInterface.get(ctx, dest))) {
            // climbing out onto land wants the speed
            return;
        }
        if (dest.x == src.x && dest.z == src.z) {
            // vertical (pillar, downward), no corner in the horizontal plane
            return;
        }
        IPathExecutor current = baritone.getPathingBehavior().getCurrent();
        if (current == null) {
            return;
        }
        List<BetterBlockPos> positions = current.getPath().positions();
        int pos = current.getPosition();
        if (pos + 2 >= positions.size()) {
            return; // path is over, nothing to aim the early turn at
        }
        BetterBlockPos next = positions.get(pos + 2);
        double ax = dest.x - src.x;
        double az = dest.z - src.z;
        double bx = next.x - dest.x;
        double bz = next.z - dest.z;
        if (bx == 0 && bz == 0) {
            return; // next leg is vertical (a descend), no corner to cut either
        }
        if (ax * bx + az * bz >= 0.99 * Math.hypot(ax, az) * Math.hypot(bx, bz)) {
            return; // straight chains share their heading exactly, anything else is a corner
        }
        double speed = Math.hypot(ctx.player().getDeltaMovement().x, ctx.player().getDeltaMovement().z);
        double dist = Math.hypot(dest.x + 0.5 - ctx.player().position().x, dest.z + 0.5 - ctx.player().position().z);
        if (dist < speed * WATER_COAST_TICKS) {
            float yaw = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(next), ctx.playerRotations()).getYaw();
            currentState.getTarget().getRotation().ifPresent(rotation ->
                    currentState.setTarget(new MovementState.MovementTarget(new Rotation(yaw, rotation.getPitch()), false))
            );
        }
    }

    /**
     * The pitch that, with jump held, makes vanilla settle at the given vertical speed. Clamped to
     * straight up / straight down, which is about +0.31 / -0.14 blocks per tick
     */
    private static float pitchForSwimVelocity(double velocity) {
        double look = lookForSwimVelocity(velocity, SWIM_PULL);
        if (look < SWIM_STEEP_LOOK) {
            // the stronger pull kicks in past this look, so the map is piecewise (and has a small gap
            // right at the threshold, which just pins to the threshold)
            look = Math.min(SWIM_STEEP_LOOK, lookForSwimVelocity(velocity, SWIM_PULL_STEEP));
        }
        look = Math.max(-1, Math.min(1, look));
        // look vector y is -sin(pitch)
        return (float) -Math.toDegrees(Math.asin(look));
    }

    private static double lookForSwimVelocity(double velocity, double pull) {
        // steady state of v = drag * ((1 - pull) * (v + boost) + pull * look), solved for look
        double carry = WATER_DRAG_Y * (1 - pull);
        return (velocity * (1 - carry) - carry * SWIM_JUMP_BOOST) / (WATER_DRAG_Y * pull);
    }

    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        boolean somethingInTheWay = false;
        for (BetterBlockPos blockPos : positionsToBreak) {
            if (!ctx.world().getEntitiesOfClass(FallingBlockEntity.class, new AABB(0, 0, 0, 1, 1.1, 1).move(blockPos)).isEmpty() && Baritone.settings().pauseMiningForFallingBlocks.value) {
                return false;
            }
            if (!MovementHelper.canWalkThrough(ctx, blockPos)) { // can't break air, so don't try
                somethingInTheWay = true;
                MovementHelper.switchToBestToolFor(ctx, BlockStateInterface.get(ctx, blockPos));
                Optional<Rotation> reachable = RotationUtils.reachable(ctx, blockPos, ctx.playerController().getBlockReachDistance());
                if (reachable.isPresent()) {
                    Rotation rotTowardsBlock = reachable.get();
                    state.setTarget(new MovementState.MovementTarget(rotTowardsBlock, true));
                    if (ctx.isLookingAt(blockPos) || ctx.playerRotations().isReallyCloseTo(rotTowardsBlock)) {
                        state.setInput(Input.CLICK_LEFT, true);
                    }
                    return false;
                }
                //get rekt minecraft
                //i'm doing it anyway
                //i dont care if theres snow in the way!!!!!!!
                //you dont own me!!!!
                state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        VecUtils.getBlockPosCenter(blockPos), ctx.playerRotations()), true)
                );
                // don't check selectedblock on this one, this is a fallback when we can't see any face directly, it's intended to be breaking the "incorrect" block
                state.setInput(Input.CLICK_LEFT, true);
                return false;
            }
        }
        if (somethingInTheWay) {
            // There's a block or blocks that we can't walk through, but we have no target rotation to reach any
            // So don't return true, actually set state to unreachable
            state.setStatus(MovementStatus.UNREACHABLE);
            return true;
        }
        return true;
    }

    @Override
    public boolean safeToCancel() {
        return safeToCancel(currentState);
    }

    protected boolean safeToCancel(MovementState currentState) {
        return true;
    }

    @Override
    public BetterBlockPos getSrc() {
        return src;
    }

    @Override
    public BetterBlockPos getDest() {
        return dest;
    }

    @Override
    public void reset() {
        currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    }

    /**
     * Calculate latest movement state. Gets called once a tick.
     *
     * @param state The current state
     * @return The new state
     */
    public MovementState updateState(MovementState state) {
        if (!prepared(state)) {
            return state.setStatus(MovementStatus.PREPPING);
        } else if (state.getStatus() == MovementStatus.PREPPING) {
            state.setStatus(MovementStatus.WAITING);
        }

        if (state.getStatus() == MovementStatus.WAITING) {
            state.setStatus(MovementStatus.RUNNING);
        }

        return state;
    }

    @Override
    public BlockPos getDirection() {
        return getDest().subtract(getSrc());
    }

    public void checkLoadedChunk(CalculationContext context) {
        calculatedWhileLoaded = context.bsi.worldContainsLoadedChunk(dest.x, dest.z);
    }

    @Override
    public boolean calculatedWhileLoaded() {
        return calculatedWhileLoaded;
    }

    @Override
    public void resetBlockCache() {
        toBreakCached = null;
        toPlaceCached = null;
        toWalkIntoCached = null;
    }

    public List<BlockPos> toBreak(BlockStateInterface bsi) {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BetterBlockPos positionToBreak : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(bsi, positionToBreak.x, positionToBreak.y, positionToBreak.z)) {
                result.add(positionToBreak);
            }
        }
        toBreakCached = result;
        return result;
    }

    public List<BlockPos> toPlace(BlockStateInterface bsi) {
        if (toPlaceCached != null) {
            return toPlaceCached;
        }
        List<BlockPos> result = new ArrayList<>();
        if (positionToPlace != null && !MovementHelper.canWalkOn(bsi, positionToPlace.x, positionToPlace.y, positionToPlace.z)) {
            result.add(positionToPlace);
        }
        toPlaceCached = result;
        return result;
    }

    public List<BlockPos> toWalkInto(BlockStateInterface bsi) { // overridden by movementdiagonal
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        return toWalkIntoCached;
    }

    public BlockPos[] toBreakAll() {
        return positionsToBreak;
    }
}
