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
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Pair;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.clutch.Clutch;
import baritone.pathing.clutch.ClutchHelper;
import baritone.pathing.movement.*;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableClutchResult;
import baritone.utils.pathing.MutableMoveResult;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;

public class MovementDescend extends Movement {
    private int numTicks = 0;
    public boolean forceSafeMode = false;

    public MovementDescend(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end, new BetterBlockPos[]{end.above(2), end.above(), end}, end.below());
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
        forceSafeMode = false;
    }

    /**
     * Called by PathExecutor if needing safeMode can only be detected with knowledge about the next movement
     */
    public void forceSafeMode() {
        forceSafeMode = true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a fall not a descend
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest.above(), dest);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        double totalCost = 0;
        BlockState destDown = context.get(destX, y - 1, destZ);
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y - 1, destZ, destDown, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y, destZ, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, true); // only the top block in the 3 we need to mine needs to consider the falling blocks above
        if (totalCost >= COST_INF) {
            return;
        }

        Block fromDown = context.get(x, y - 1, z).getBlock();
        if (fromDown == Blocks.LADDER || fromDown == Blocks.VINE) {
            return;
        }

        // A
        //SA
        // A
        // B
        // C
        // D
        //if S is where you start, B needs to be air for a movementfall
        //A is plausibly breakable by either descend or fall
        //C, D, etc determine the length of the fall

        BlockState below = context.get(destX, y - 2, destZ);
        if (!MovementHelper.canWalkOn(context, destX, y - 2, destZ, below)) {
            dynamicFallCost(context, x, y, z, destX, destZ, totalCost, below, res, null);
            return;
        }

        if (MovementHelper.canUseFrostWalker(context, destDown)) { // no need to check assumeWalkOnWater
            return; // the water will freeze when we try to walk into it
        }

        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        double walk = WALK_OFF_BLOCK_COST;
        if (fromDown == Blocks.SOUL_SAND) {
            // use this ratio to apply the soul sand speed penalty to our 0.8 block distance
            walk *= WALK_ONE_OVER_SOUL_SAND_COST / WALK_ONE_BLOCK_COST;
        }
        totalCost += walk + Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
        res.x = destX;
        res.y = y - 1;
        res.z = destZ;
        res.cost = totalCost;
    }

    public static void dynamicFallCost(CalculationContext context, int x, int y, int z, int destX, int destZ, double frontBreak, BlockState below, MutableMoveResult res, MutableClutchResult clutchRes) {
        if (frontBreak != 0 && context.get(destX, y + 2, destZ).getBlock() instanceof FallingBlock) {
            // if frontBreak is 0 we can actually get through this without updating the falling block and making it actually fall
            // but if frontBreak is nonzero, we're breaking blocks in front, so don't let anything fall through this column,
            // and potentially replace the water we're going to fall into
            return;
        }
        if (!MovementHelper.canWalkThrough(context, destX, y - 2, destZ, below)) {
            return;
        }
        LocalPlayer player = context.getBaritone().getPlayerContext().player();
        if (context.considerPotionEffects && player.hasEffect(MobEffects.LEVITATION)) {
            return;
        }
        double tentativeCost = WALK_OFF_BLOCK_COST + frontBreak;
        Optional<Double> aboveBlockCost = Optional.empty();
        boolean aboveBlockPriority = true;
        double velocity = 0;
        int effectiveStartHeight = y;
        int newY;
        for (int fallHeight = context.minFallHeight; (newY = y - fallHeight) >= context.world.getMinBuildHeight(); fallHeight++) {
            BlockState ontoBlock = context.get(destX, newY, destZ);
            if (MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                if (aboveBlockCost.isPresent()) {
                    tentativeCost += aboveBlockCost.get();
                    aboveBlockCost = Optional.empty();
                    aboveBlockPriority = true;
                }
                Direction[] availableDirections = MovementHelper.canPlace(context.bsi, new BetterBlockPos(destX, newY, destZ));
                continue;
            }
            int unprotectedFallHeight = effectiveStartHeight - 1 - newY;
            System.out.println("unprotectedFallHeight: " + unprotectedFallHeight);
            if (context.considerPotionEffects && player.hasEffect(MobEffects.SLOW_FALLING)) {
                res.cost = tentativeCost + ActionCosts.distanceToTicks(unprotectedFallHeight, 0.01d, velocity);
                res.x = destX;
                res.y = newY + 1;
                res.z = destZ;
                break;
            }
            if (unprotectedFallHeight <= context.maxFallHeightNoClutch &&
                    MovementHelper.canWalkOn(context, destX, newY, destZ, ontoBlock) &&
                    !MovementHelper.isBottomSlab(ontoBlock)) {
                // fallHeight = 4 means onto.up() is 3 blocks down, which is the max
                double newCost = tentativeCost + ActionCosts.distanceToTicks(unprotectedFallHeight, 0.08d, velocity);
                if (newCost < res.cost) {
                    res.cost = newCost;
                    res.x = destX;
                    res.y = newY + 1;
                    res.z = destZ;
                }
                break;
            }
            BlockState aboveBlock = context.get(destX, newY + 1, destZ);
            Optional<Clutch> nonSolidClutchBlock = Optional.empty();
            if (unprotectedFallHeight > context.maxFallHeightNoClutch) {
                for (Clutch clutch : ClutchHelper.CLUTCHES) {
                    if (clutch.compare(ontoBlock) &&
                            clutch.getFallDamage(unprotectedFallHeight) <= context.maxFallHeightNoClutch) {
                        if (clutch.isSolid(context)) {
                            double newCost = tentativeCost + clutch.getCost(unprotectedFallHeight, 1d, velocity).first() + clutch.getAdditionalCost();
                            if (newCost < res.cost) {
                                res.cost = newCost;
                                res.x = destX;
                                res.y = newY + 1;// this is the block we're falling onto, so dest is +1
                                res.z = destZ;
                                if (clutchRes != null) {
                                    clutchRes.clutch = clutch;
                                }
                            }
                        } else {
                            nonSolidClutchBlock = Optional.of(clutch);
                        }
                        break;
                    }
                }
            }
            if (unprotectedFallHeight - 1 <= context.maxFallHeightClutch &&
                    context.allowPlace &&
                    !context.isPossiblyProtected(destX, newY + 1, destZ) &&
                    context.worldBorder.canPlaceAt(destX, destZ) &&
                    MovementHelper.isReplaceable(destX, newY + 1, destZ, aboveBlock, context.bsi) &&
                    !aboveBlock.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                for (Clutch clutch : ClutchHelper.CLUTCHES) {
                    ItemStack item = clutch.getClutchingItem(context);
                    if (clutch.getFallDamage(unprotectedFallHeight - 1) <= context.maxFallHeightNoClutch &&
                            clutch.isPlaceable(context, destX, newY, destZ, ontoBlock) &&
                            item != null) {
                        double newCost = tentativeCost + context.placeBlockCost;
                        if (clutch.isSolid(context)) {
                            newCost += ActionCosts.distanceToTicks(unprotectedFallHeight, 0.08d, velocity) + clutch.getAdditionalCost();
                        } else if (MovementHelper.canWalkOn(context, destX, newY, destZ, ontoBlock)) {
                            newCost += clutch.getCost(unprotectedFallHeight, 1d, velocity).first();
                        } else {
                            continue;
                        }
                        if (newCost < res.cost) {
                            res.cost = newCost;
                            res.x = destX;
                            res.y = newY + 2;
                            res.z = destZ;
                            if (clutchRes != null) {
                                clutchRes.clutch = clutch;
                                clutchRes.item = item;
                                clutchRes.placeBelow = true;
                            }
                        }
                        break;
                    }
                }
            }
            if (nonSolidClutchBlock.isPresent()) {
                Pair<Double, Double> fallCostAndVelocity = nonSolidClutchBlock.get().getCost(unprotectedFallHeight, 1d, velocity);
                if (aboveBlockCost.isPresent() && aboveBlockPriority) {
                    tentativeCost += aboveBlockCost.get();
                    aboveBlockCost = Optional.of(fallCostAndVelocity.first());
                } else {
                    tentativeCost += fallCostAndVelocity.first();
                    aboveBlockCost = nonSolidClutchBlock.get().slowsOnTopBlock() ? Optional.of(fallCostAndVelocity.first()) : Optional.empty();
                }
                aboveBlockPriority = nonSolidClutchBlock.get().topBlockPriority();
                if (res.cost > tentativeCost) {
                    velocity = fallCostAndVelocity.second();
                    effectiveStartHeight = newY;
                    continue;
                } else {
                    break;
                }
            }
            break;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        BlockPos fakeDest = new BlockPos(dest.getX() * 2 - src.getX(), dest.getY(), dest.getZ() * 2 - src.getZ());
        if ((playerFeet.equals(dest) || playerFeet.equals(fakeDest)) && (MovementHelper.isLiquid(ctx, dest) || ctx.player().position().y - dest.getY() < 0.5)) { // lilypads
            // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
            return state.setStatus(MovementStatus.SUCCESS);
            /* else {
                // System.out.println(player().position().y + " " + playerFeet.getY() + " " + (player().position().y - playerFeet.getY()));
            }*/
        }
        if (safeMode()) {
            double destX = (src.getX() + 0.5) * 0.17 + (dest.getX() + 0.5) * 0.83;
            double destZ = (src.getZ() + 0.5) * 0.17 + (dest.getZ() + 0.5) * 0.83;
            state.setTarget(new MovementState.MovementTarget(
                    RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                            new Vec3(destX, dest.getY(), destZ),
                            ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                    false
            )).setInput(Input.MOVE_FORWARD, true);
            return state;
        }
        double diffX = ctx.player().position().x - (dest.getX() + 0.5);
        double diffZ = ctx.player().position().z - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = ctx.player().position().x - (src.getX() + 0.5);
        double z = ctx.player().position().z - (src.getZ() + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);

        state.setInput(Input.SNEAK, Baritone.settings().allowWalkOnMagmaBlocks.value && ctx.world().getBlockState(ctx.player().blockPosition().below()).is(Blocks.MAGMA_BLOCK));

        if (!playerFeet.equals(dest) || ab > 0.25) {
            if (numTicks++ < 20 && fromStart < 1.25) {
                MovementHelper.moveTowards(ctx, state, fakeDest);
            } else {
                MovementHelper.moveTowards(ctx, state, dest);
            }
        }
        return state;
    }

    public boolean safeMode() {
        if (forceSafeMode) {
            return true;
        }
        // (dest - src) + dest is offset 1 more in the same direction
        // so it's the block we'd need to worry about running into if we decide to sprint straight through this descend
        BlockPos into = dest.subtract(src.below()).offset(dest);
        if (skipToAscend()) {
            // if dest extends into can't walk through, but the two above are can walk through, then we can overshoot and glitch in that weird way
            return true;
        }
        for (int y = 0; y <= 2; y++) { // we could hit any of the three blocks
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.get(ctx, into.above(y)))) {
                return true;
            }
        }
        return false;
    }

    public boolean skipToAscend() {
        BlockPos into = dest.subtract(src.below()).offset(dest);
        return !MovementHelper.canWalkThrough(ctx, new BetterBlockPos(into)) && MovementHelper.canWalkThrough(ctx, new BetterBlockPos(into).above()) && MovementHelper.canWalkThrough(ctx, new BetterBlockPos(into).above(2));
    }
}
