/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.actions;

import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.ui.LookManager;
import baritone.util.Out;
import baritone.util.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class ActionPillar extends ActionPlaceOrBreak {

    public ActionPillar(BlockPos start) {
        super(start, start.up(), new BlockPos[]{start.up(2)}, new BlockPos[]{start});
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        Block fromDown = Baritone.get(from).getBlock();
        boolean ladder = fromDown instanceof BlockLadder || fromDown instanceof BlockVine;
        if (!ladder) {
            Block d = Baritone.get(from.down()).getBlock();
            if (d instanceof BlockLadder || d instanceof BlockVine) {
                return COST_INF;
            }
        }
        if ((!Baritone.hasThrowaway && !ladder) || !Baritone.allowVerticalMotion) {
            return COST_INF;
        }
        if (fromDown instanceof BlockVine) {
            if (getAgainst(from) == null) {
                return COST_INF;
            }
        }
        double hardness = getTotalHardnessOfBlocksToBreak(ts);
        if (hardness != 0) {
            Block tmp = Baritone.get(from.up(2)).getBlock();
            if (tmp instanceof BlockLadder || tmp instanceof BlockVine) {
                hardness = 0;
            } else if (!canWalkOn(from.up(3)) || canWalkThrough(from.up(3)) || Baritone.get(from.up(3)).getBlock() instanceof BlockFalling) {//if the block above where we want to break is not a full block, don't do it
                return COST_INF;
            }
        }
        if (isLiquid(from) || isLiquid(from.down())) {//can't pillar on water or in water
            return COST_INF;
        }
        if (ladder) {
            return LADDER_UP_ONE_COST + hardness;
        } else {
            return JUMP_ONE_BLOCK_COST + PLACE_ONE_BLOCK_COST + hardness;
        }
    }
    int numTicks = 0;

    public BlockPos getAgainst(BlockPos vine) {
        if (Baritone.isBlockNormalCube(vine.north())) {
            return vine.north();
        }
        if (Baritone.isBlockNormalCube(vine.south())) {
            return vine.south();
        }
        if (Baritone.isBlockNormalCube(vine.east())) {
            return vine.east();
        }
        if (Baritone.isBlockNormalCube(vine.west())) {
            return vine.west();
        }
        return null;
    }

    @Override
    protected boolean tick0() {
        IBlockState fromDown = Baritone.get(from);
        boolean ladder = fromDown.getBlock() instanceof BlockLadder || fromDown.getBlock() instanceof BlockVine;
        boolean vine = fromDown.getBlock() instanceof BlockVine;
        if (!ladder && !LookManager.lookAtBlock(positionsToPlace[0], true)) {
            return false;
        }
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        boolean blockIsThere = canWalkOn(from) || ladder;
        if (ladder) {
            BlockPos against = vine ? getAgainst(from) : from.offset(fromDown.getValue(BlockLadder.FACING).getOpposite());
            if (against == null) {
                Out.gui("Unable to climb vines", Out.Mode.Standard);
                return false;
            }
            if (Baritone.playerFeet.equals(against.up()) || Baritone.playerFeet.equals(to)) {
                return true;
            }
            /*if (thePlayer.getPosition0().getX() != from.getX() || thePlayer.getPosition0().getZ() != from.getZ()) {
             Baritone.moveTowardsBlock(from);
             }*/
            MovementManager.moveTowardsBlock(against);
            return false;
        } else {
            if (!switchtothrowaway(true)) {//get ready to place a throwaway block
                return false;
            }
            numTicks++;
            MovementManager.jumping = thePlayer.posY < to.getY(); //if our Y coordinate is above our goal, stop jumping
            MovementManager.sneak = true;
            //otherwise jump
            if (numTicks > 40) {
                double diffX = thePlayer.posX - (to.getX() + 0.5);
                double diffZ = thePlayer.posZ - (to.getZ() + 0.5);
                double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                if (dist > 0.17) {//why 0.17? because it seemed like a good number, that's why
                    MovementManager.forward = true;//if it's been more than forty ticks of trying to jump and we aren't done yet, go forward, maybe we are stuck
                }
            }
            if (!blockIsThere) {
                Out.log("Block not there yet");
                Block fr = Baritone.get(from).getBlock();
                if (!(Baritone.isAir(from) || fr.isReplaceable(Minecraft.getMinecraft().world, from))) {
                    MovementManager.isLeftClick = true;
                    blockIsThere = false;
                } else if (Minecraft.getMinecraft().player.isSneaking()) {
                    MovementManager.rightClickMouse();//constantly right click
                }
            }
        }
        BlockPos whereAmI = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        if (whereAmI.equals(to) && blockIsThere) {//if we are at our goal and the block below us is placed
            Out.log("Done pillaring to " + to);
            MovementManager.jumping = false;//stop jumping
            return true;//we are done
        }
        return false;
    }
}
