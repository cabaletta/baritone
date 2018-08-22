/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.movement.movements;

import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.InputOverrideHandler;
import baritone.bot.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementAscend extends Movement {

    private BlockPos[] against = new BlockPos[3];
    private int ticksWithoutPlacement = 0;

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest, new BlockPos[]{dest, src.up(2), dest.up()}, new BlockPos[]{dest.down()});

        BlockPos placementLocation = positionsToPlace[0]; // dest.down()
        int i = 0;
        if (!placementLocation.north().equals(src))
            against[i++] = placementLocation.north();

        if (!placementLocation.south().equals(src))
            against[i++] = placementLocation.south();

        if (!placementLocation.east().equals(src))
            against[i++] = placementLocation.east();

        if (!placementLocation.west().equals(src))
            against[i] = placementLocation.west();

        // TODO: add ability to place against .down() as well as the cardinal directions
        // useful for when you are starting a staircase without anything to place against
        // Counterpoint to the above TODO ^ you should move then pillar instead of ascend
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        IBlockState toPlace = BlockStateInterface.get(positionsToPlace[0]);
        if (!MovementHelper.canWalkOn(positionsToPlace[0], toPlace)) {
            if (!BlockStateInterface.isAir(toPlace) && !BlockStateInterface.isWater(toPlace.getBlock())) {
                // TODO replace this check with isReplacable or similar
                return COST_INF;
            }
            if (!context.hasThrowaway()) {
                return COST_INF;
            }
            for (BlockPos against1 : against) {
                if (BlockStateInterface.get(against1).isBlockNormalCube()) {
                    return JUMP_ONE_BLOCK_COST + WALK_ONE_BLOCK_COST + context.placeBlockCost() + getTotalHardnessOfBlocksToBreak(context);
                }
            }
            return COST_INF;
        }
        if (BlockStateInterface.get(src.up(3)).getBlock() instanceof BlockFalling) {//it would fall on us and possibly suffocate us
            // HOWEVER, we assume that we're standing in the start position
            // that means that src and src.up(1) are both air
            // maybe they aren't now, but they will be by the time this starts
            Block srcUp = BlockStateInterface.get(src.up(1)).getBlock();
            Block srcUp2 = BlockStateInterface.get(src.up(2)).getBlock();
            if (!(srcUp instanceof BlockFalling) || !(srcUp2 instanceof BlockFalling)) {
                // if both of those are BlockFalling, that means that by standing on src
                // (the presupposition of this Movement)
                // we have necessarily already cleared the entire BlockFalling stack
                // on top of our head

                // but if either of them aren't BlockFalling, that means we're still in suffocation danger
                // so don't do it
                return COST_INF;
            }
            // you may think we only need to check srcUp2, not srcUp
            // however, in the scenario where glitchy world gen where unsupported sand / gravel generates
            // it's possible srcUp is AIR from the start, and srcUp2 is falling
            // and in that scenario, when we arrive and break srcUp2, that lets srcUp3 fall on us and suffocate us
        }
        // TODO maybe change behavior if src.down() is soul sand?
        double walk = WALK_ONE_BLOCK_COST;
        if (toPlace.getBlock().equals(Blocks.SOUL_SAND)) {
            walk *= WALK_ONE_OVER_SOUL_SAND_COST / WALK_ONE_BLOCK_COST;
        }
        // we hit space immediately on entering this action
        return Math.max(JUMP_ONE_BLOCK_COST, walk) + getTotalHardnessOfBlocksToBreak(context);
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        // TODO incorporate some behavior from ActionClimb (specifically how it waited until it was at most 1.2 blocks away before starting to jump
        // for efficiency in ascending minimal height staircases, which is just repeated MovementAscend, so that it doesn't bonk its head on the ceiling repeatedly)
        switch (state.getStatus()) {
            case WAITING:
                state.setStatus(MovementStatus.RUNNING);
            case RUNNING:
                break;
            default:
                return state;
        }
        if (playerFeet().equals(dest)) {
            state.setStatus(MovementStatus.SUCCESS);
            return state;
        }

        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            for (BlockPos anAgainst : against) {
                if (BlockStateInterface.get(anAgainst).isBlockNormalCube()) {
                    if (!MovementHelper.throwaway(true)) {//get ready to place a throwaway block
                        return state.setStatus(MovementStatus.UNREACHABLE);
                    }
                    double faceX = (dest.getX() + anAgainst.getX() + 1.0D) * 0.5D;
                    double faceY = (dest.getY() + anAgainst.getY()) * 0.5D;
                    double faceZ = (dest.getZ() + anAgainst.getZ() + 1.0D) * 0.5D;
                    state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations()), true));
                    EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;
                    if (Objects.equals(LookBehaviorUtils.getSelectedBlock().orElse(null), anAgainst) && LookBehaviorUtils.getSelectedBlock().get().offset(side).equals(positionsToPlace[0])) {
                        ticksWithoutPlacement++;
                        state.setInput(InputOverrideHandler.Input.SNEAK, true);
                        if (player().isSneaking()) {
                            state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                        }
                        if (ticksWithoutPlacement > 20) {
                            state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);//we might be standing in the way, move back
                        }
                    }
                    System.out.println("Trying to look at " + anAgainst + ", actually looking at" + LookBehaviorUtils.getSelectedBlock());
                    return state;
                }
            }
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        MovementHelper.moveTowards(state, dest);
        state.setInput(InputOverrideHandler.Input.JUMP, true);

        // TODO check if the below actually helps or hurts, it's weird
        //double flatDistToNext = Math.abs(to.getX() - from.getX()) * Math.abs((to.getX() + 0.5D) - thePlayer.posX) + Math.abs(to.getZ() - from.getZ()) * Math.abs((to.getZ() + 0.5D) - thePlayer.posZ);
        //boolean pointingInCorrectDirection = MovementManager.moveTowardsBlock(to);
        //MovementManager.jumping = flatDistToNext < 1.2 && pointingInCorrectDirection;
        //once we are pointing the right way and moving, start jumping
        //this is slightly more efficient because otherwise we might start jumping before moving, and fall down without moving onto the block we want to jump onto
        //also wait until we are close enough, because we might jump and hit our head on an adjacent block
        //return Baritone.playerFeet.equals(to);

        return state;
    }
}
