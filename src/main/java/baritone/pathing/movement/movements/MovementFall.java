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
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.behavior.InventoryBehavior;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MovementFall extends Movement {

    private static final ItemStack STACK_BUCKET_WATER = new ItemStack(Items.WATER_BUCKET);
    private static final ItemStack STACK_BUCKET_EMPTY = new ItemStack(Items.BUCKET);

    public MovementFall(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, MovementFall.buildPositionsToBreak(src, dest));
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        MovementDescend.cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a descend not a fall
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        Set<BetterBlockPos> set = new HashSet<>();
        set.add(src);
        for (int y = src.y - dest.y; y >= 0; y--) {
            set.add(dest.above(y));
        }
        return set;
    }

    private enum FallMode {
        NONE, BUCKET, CLUTCH
    }

    // the clutch ran out of things to try (nothing in reach in time, nothing to hang on), so it's the bucket or nothing
    private boolean clutchGaveUp;

    @Override
    public void reset() {
        super.reset();
        clutchGaveUp = false;
    }

    private FallMode fallMode() {
        CalculationContext context = new CalculationContext(baritone);
        MutableMoveResult result = new MutableMoveResult();
        if (MovementDescend.dynamicFallCost(context, src.x, src.y, src.z, dest.x, dest.z, 0, context.get(dest.x, src.y - 2, dest.z), result)) {
            return FallMode.BUCKET;
        }
        if (!result.clutch) {
            return FallMode.NONE;
        }
        if (clutchGaveUp) {
            return context.hasWaterBucket ? FallMode.BUCKET : FallMode.NONE;
        }
        return FallMode.CLUTCH;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        Rotation toDest = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations());
        Rotation targetRotation = null;
        BlockState destState = ctx.world().getBlockState(dest);
        Block destBlock = destState.getBlock();

        if (ctx.world().getBlockState(dest.below()).is(Blocks.MAGMA_BLOCK) && MovementHelper.steppingOnBlocks(ctx).stream().allMatch(block -> MovementHelper.canWalkThrough(ctx, block))) {
            state.setInput(Input.SNEAK, true);
        }

        boolean isWater = destState.getFluidState().getType() instanceof WaterFluid;
        FallMode mode = isWater ? FallMode.NONE : fallMode();
        if (mode == FallMode.BUCKET && !playerFeet.equals(dest)) {
            if (!Inventory.isHotbarSlot(ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_WATER)) || ctx.world().dimension() == Level.NETHER) {
                return state.setStatus(MovementStatus.UNREACHABLE);
            }

            if (ctx.player().position().y - dest.getY() < ctx.playerController().getBlockReachDistance() && !ctx.player().onGround()) {
                ctx.player().getInventory().selected = ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_WATER);

                targetRotation = new Rotation(toDest.getYaw(), 90.0F);

                if (ctx.isLookingAt(dest) || ctx.isLookingAt(dest.below())) {
                    state.setInput(Input.CLICK_RIGHT, true);
                }
            }
        }

        if (!MovementHelper.openDoors(ctx, state, src, new BetterBlockPos(dest.x, src.y, dest.z))) {
            return state;
        }

        if (targetRotation != null) {
            state.setTarget(new MovementTarget(targetRotation, true));
        } else {
            state.setTarget(new MovementTarget(toDest, false));
        }
        if (playerFeet.equals(dest) && (ctx.player().position().y - playerFeet.getY() < 0.094 || isWater)) { // 0.094 because lilypads
            if (isWater) { // only match water, not flowing water (which we cannot pick up with a bucket)
                if (Inventory.isHotbarSlot(ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_EMPTY))) {
                    ctx.player().getInventory().selected = ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_EMPTY);
                    if (ctx.player().getDeltaMovement().y >= 0) {
                        return state.setInput(Input.CLICK_RIGHT, true);
                    } else {
                        return state;
                    }
                } else {
                    if (ctx.player().getDeltaMovement().y >= 0) {
                        return state.setStatus(MovementStatus.SUCCESS);
                    } // don't else return state; we need to stay centered because this water might be flowing under the surface
                }
            } else {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        }
        if (mode == FallMode.CLUTCH && !playerFeet.equals(dest) && clutch(state)) {
            return state; // it's doing the steering, the centering below would fight it
        }
        Vec3 destCenter = VecUtils.getBlockPosCenter(dest); // we are moving to the 0.5 center not the edge (like if we were falling on a ladder)
        if (Math.abs(ctx.player().position().x + ctx.player().getDeltaMovement().x - destCenter.x) > 0.1 || Math.abs(ctx.player().position().z + ctx.player().getDeltaMovement().z - destCenter.z) > 0.1) {
            if (!ctx.player().onGround() && Math.abs(ctx.player().getDeltaMovement().y) > 0.4) {
                state.setInput(Input.SNEAK, true);
            }
            state.setInput(Input.MOVE_FORWARD, true);
        }
        Vec3i avoid = Optional.ofNullable(avoid()).map(Direction::getUnitVec3i).orElse(null);
        if (avoid == null) {
            avoid = src.subtract(dest);
        } else {
            double dist = Math.abs(avoid.getX() * (destCenter.x - avoid.getX() / 2.0 - ctx.player().position().x)) + Math.abs(avoid.getZ() * (destCenter.z - avoid.getZ() / 2.0 - ctx.player().position().z));
            if (dist < 0.6) {
                state.setInput(Input.MOVE_FORWARD, true);
            } else if (!ctx.player().onGround()) {
                state.setInput(Input.SNEAK, false);
            }
        }
        if (targetRotation == null) {
            Vec3 destCenterOffset = new Vec3(destCenter.x + 0.125 * avoid.getX(), destCenter.y, destCenter.z + 0.125 * avoid.getZ());
            state.setTarget(new MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), destCenterOffset, ctx.playerRotations()), false));
        }
        return state;
    }

    // ladder clutch, one tick of it. the whole idea: a vine or ladder in one of the last few cells before the floor wipes the
    // fall distance (see LadderClutch for how, there's two ways and one of them works at terminal velocity). minecraft picks
    // what's under the crosshair at the start of a tick, before our onTick, and then does handleKeybinds (the click, so
    // the block is in the world) before the player ticks. so the click goes on the tick we want the climbable to exist for,
    // and the aim goes on the tick before that, pointed from where our eye is going to be
    // returns true if it took over the inputs this tick
    private boolean clutch(MovementState state) {
        LocalPlayer player = ctx.player();
        if (player.onGround() || player.getDeltaMovement().y >= 0) {
            return false;
        }
        InventoryBehavior inv = ((Baritone) baritone).getInventoryBehavior();
        Item item = inv.pickClutchItem(false);
        if (item == null) {
            return false; // the last one's already in the wall, or somebody ate the vines
        }
        BlockStateInterface bsi = new BlockStateInterface(ctx);
        Vec3 pos = player.position();
        Vec3 mot = player.getDeltaMovement();
        int mask = 0;
        for (int k = 0; k < LadderClutch.CELLS && dest.y + k < src.y; k++) {
            BlockState there = bsi.get0(dest.above(k));
            if (MovementHelper.isClimbable(there.getBlock())) {
                return false; // already one in here (us, a tick ago), physics takes it from here
            }
            if (there.isAir() && !walls(bsi, dest.above(k), pos, false).isEmpty()) {
                mask |= 1 << k;
            }
        }
        double reach = ctx.playerController().getBlockReachDistance();
        // what's under the crosshair right now is exactly what a click will hit
        BlockHitResult hit = ctx.objectMouseOver() instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK ? b : null;
        int aimed = hit == null ? -1 : aimedCell(bsi, hit, mask);
        if (aimed >= 0) {
            LadderClutch.Plan now = LadderClutch.pick(pos.y, mot.y, player.fallDistance, dest.y, 1 << aimed, 0, reach);
            boolean roomy = item != Items.LADDER || gap(dest.above(aimed), hit.getDirection().getOpposite(), pos) >= LadderClutch.LADDER_CLEARANCE + 0.01;
            if (now != null && now.place() == 0 && roomy) {
                inv.pickClutchItem(true);
                state.setTarget(new MovementTarget(ctx.playerRotations(), true));
                state.setInput(Input.CLICK_RIGHT, true);
                return true;
            }
        }
        LadderClutch.Plan plan = LadderClutch.pick(pos.y, mot.y, player.fallDistance, dest.y, mask, 1, reach);
        if (plan == null) {
            clutchGaveUp = true;
            return false;
        }
        BlockPos cell = dest.above(plan.cell());
        boolean ladder = item == Items.LADDER;
        if (plan.place() > 1) {
            if (ladder && plan.place() > 3) {
                // sneaking is a third of the air control, and the ladder needs us about 0.6 off the wall (see LadderClutch.WALL_GAP)
                // not within the last couple ticks though, the crouch is a lower eye and the aim is worked out for a standing one
                List<Direction> sides = walls(bsi, cell, pos, false);
                steer(state, cell, sides.get(0), pos, mot);
            }
            return ladder; // a vine doesn't care where we are, the plain centering is fine
        }
        inv.pickClutchItem(true);
        return aim(state, bsi, cell, ladder, pos, mot, reach);
    }

    // line the crosshair up with the wall next to this cell for the next tick, and only if the ray from where we'll be
    // really ends up on the face (same check as attemptToPlaceABlock, except from the next eye instead of this one)
    private boolean aim(MovementState state, BlockStateInterface bsi, BlockPos cell, boolean ladder, Vec3 pos, Vec3 mot, double reach) {
        Vec3 eye = new Vec3(pos.x + mot.x, pos.y + mot.y + ctx.player().getEyeHeight(), pos.z + mot.z);
        Vec3 feet = new Vec3(eye.x, pos.y + mot.y, eye.z);
        for (Direction side : walls(bsi, cell, feet, ladder)) {
            for (double height : AIM_HEIGHTS) {
                Rotation want = RotationUtils.calcRotationFromVec3d(eye, faceSpot(cell, side, eye, height), ctx.playerRotations());
                Rotation actual = baritone.getLookBehavior().getAimProcessor().peekRotation(want);
                Vec3 dir = RotationUtils.calcLookDirectionFromRotation(actual);
                HitResult res = ctx.world().clip(new ClipContext(eye, eye.add(dir.scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, ctx.player()));
                if (res instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK && b.getBlockPos().equals(cell.relative(side)) && b.getDirection() == side.getOpposite()) {
                    state.setTarget(new MovementTarget(want, true));
                    return true;
                }
            }
        }
        // nothing lines up. sit tight, if it stays that way the plan runs out of ticks and the clutch gives up
        return true;
    }

    private static final double[] AIM_HEIGHTS = {LadderClutch.AIM_HEIGHT, 0.5, 0.2};

    // a hair into the wall, so the ray ends up past the face instead of rounding off short of it
    private static Vec3 faceSpot(BlockPos cell, Direction side, Vec3 eye, double height) {
        double x = side.getStepX() != 0 ? cell.getX() + 0.5 + 0.51 * side.getStepX() : Mth.clamp(eye.x, cell.getX() + 0.15, cell.getX() + 0.85);
        double z = side.getStepZ() != 0 ? cell.getZ() + 0.5 + 0.51 * side.getStepZ() : Mth.clamp(eye.z, cell.getZ() + 0.15, cell.getZ() + 0.85);
        return new Vec3(x, cell.getY() + height, z);
    }

    // which cell a click on the crosshair would put something in, if that's one we planned for
    private int aimedCell(BlockStateInterface bsi, BlockHitResult hit, int mask) {
        Direction face = hit.getDirection();
        BlockPos cell = hit.getBlockPos().relative(face);
        int k = cell.getY() - dest.y;
        if (!face.getAxis().isHorizontal() || cell.getX() != dest.x || cell.getZ() != dest.z || k < 0 || k >= LadderClutch.CELLS || (mask & (1 << k)) == 0) {
            return -1;
        }
        BlockPos wall = hit.getBlockPos();
        return MovementHelper.canPlaceAgainst(bsi, wall) && MovementDescend.clutchWall(bsi.get0(wall)) ? k : -1;
    }

    // walls we could hang something on next to this cell, best first. a ladder is only roomy if we're not so close to the wall
    // that its slab would be inside our hitbox (the placement gets refused, or we'd land on top of it)
    private List<Direction> walls(BlockStateInterface bsi, BlockPos cell, Vec3 pos, boolean roomy) {
        List<Direction> out = new ArrayList<>(4);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos wall = cell.relative(side);
            if (MovementHelper.canPlaceAgainst(bsi, wall) && MovementDescend.clutchWall(bsi.get0(wall)) && (!roomy || gap(cell, side, pos) >= LadderClutch.LADDER_CLEARANCE + 0.02)) {
                out.add(side);
            }
        }
        out.sort(Comparator.comparingDouble(side -> Math.abs(gap(cell, side, pos) - LadderClutch.WALL_GAP)));
        return out;
    }

    // how far we are from the wall plane on this side of the cell
    private static double gap(BlockPos cell, Direction side, Vec3 pos) {
        if (side.getStepX() != 0) {
            return side.getStepX() * (cell.getX() + 0.5 + 0.5 * side.getStepX() - pos.x);
        }
        return side.getStepZ() * (cell.getZ() + 0.5 + 0.5 * side.getStepZ() - pos.z);
    }

    // walk to the spot WALL_GAP off the wall and in the middle along it. air drag is 0.91, so with no more input we'd coast
    // v / 0.09 further, that's the spot we judge
    private void steer(MovementState state, BlockPos cell, Direction side, Vec3 pos, Vec3 mot) {
        double goalX = cell.getX() + 0.5 + side.getStepX() * (0.5 - LadderClutch.WALL_GAP);
        double goalZ = cell.getZ() + 0.5 + side.getStepZ() * (0.5 - LadderClutch.WALL_GAP);
        double ex = goalX - (pos.x + mot.x / 0.09);
        double ez = goalZ - (pos.z + mot.z / 0.09);
        if (Math.hypot(ex, ez) < 0.03) {
            return;
        }
        Vec3 head = ctx.playerHead();
        state.setTarget(new MovementTarget(RotationUtils.calcRotationFromVec3d(head, new Vec3(pos.x + ex, head.y, pos.z + ez), ctx.playerRotations()), false));
        state.setInput(Input.MOVE_FORWARD, true);
        state.setInput(Input.SNEAK, true);
    }

    private Direction avoid() {
        for (int i = 0; i < 15; i++) {
            BlockState state = ctx.world().getBlockState(ctx.playerFeet().below(i));
            if (state.getBlock() == Blocks.LADDER) {
                return state.getValue(LadderBlock.FACING);
            }
        }
        return null;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we haven't started walking off the edge yet, or if we're in the process of breaking blocks before doing the fall
        // then it's safe to cancel this
        return ctx.playerFeet().equals(src) || state.getStatus() != MovementStatus.RUNNING;
    }

    private static BetterBlockPos[] buildPositionsToBreak(BetterBlockPos src, BetterBlockPos dest) {
        BetterBlockPos[] toBreak;
        int diffX = src.getX() - dest.getX();
        int diffZ = src.getZ() - dest.getZ();
        int diffY = Math.abs(src.getY() - dest.getY());
        toBreak = new BetterBlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BetterBlockPos(src.getX() - diffX, src.getY() + 1 - i, src.getZ() - diffZ);
        }
        return toBreak;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        // only break if one of the first three needs to be broken
        // specifically ignore the last one which might be water
        for (int i = 0; i < 4 && i < positionsToBreak.length; i++) {
            if (!MovementHelper.canWalkThrough(ctx, positionsToBreak[i])) {
                return super.prepared(state);
            }
        }
        return true;
    }
}
