package baritone.pathfinding.actions;

import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.ui.LookManager;
import baritone.util.Out;
import baritone.util.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Random;

/**
 * @author leijurv
 */
public class ActionBridge extends ActionPlaceOrBreak {

    BlockPos[] against = new BlockPos[3];

    public ActionBridge(BlockPos from, BlockPos to) {
        super(from, to, new BlockPos[]{to.up(), to}, new BlockPos[]{to.down()});
        int i = 0;
        if (!to.north().equals(from)) {
            against[i] = to.north().down();
            i++;
        }
        if (!to.south().equals(from)) {
            against[i] = to.south().down();
            i++;
        }
        if (!to.east().equals(from)) {
            against[i] = to.east().down();
            i++;
        }
        if (!to.west().equals(from)) {
            against[i] = to.west().down();
            i++;
        }
        //note: do NOT add ability to place against .down().down()
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        double WC = isWater(blocksToBreak[0]) || isWater(blocksToBreak[1]) ? WALK_ONE_IN_WATER_COST : WALK_ONE_BLOCK_COST;
        if (canWalkOn(positionsToPlace[0])) {//this is a walk, not a bridge
            if (canWalkThrough(positionsToBreak[0]) && canWalkThrough(positionsToBreak[1])) {
                return WC;
            }
            //double hardness1 = blocksToBreak[0].getBlockHardness(Minecraft.getMinecraft().world, positionsToBreak[0]);
            //double hardness2 = blocksToBreak[1].getBlockHardness(Minecraft.getMinecraft().world, positionsToBreak[1]);
            //Out.log("Can't walk through " + blocksToBreak[0] + " (hardness" + hardness1 + ") or " + blocksToBreak[1] + " (hardness " + hardness2 + ")");
            return WC + getTotalHardnessOfBlocksToBreak(ts);
        } else {//this is a bridge, so we need to place a block
            //return 1000000;
            Block f = Baritone.get(from.down()).getBlock();
            if (f instanceof BlockLadder || f instanceof BlockVine) {
                return COST_INF;
            }
            if (blocksToPlace[0].equals(Blocks.AIR) || (!isWater(blocksToPlace[0]) && blocksToPlace[0].isReplaceable(Minecraft.getMinecraft().world, positionsToPlace[0]))) {
                for (BlockPos against1 : against) {
                    if (Baritone.isBlockNormalCube(against1)) {
                        return WC + PLACE_ONE_BLOCK_COST + getTotalHardnessOfBlocksToBreak(ts);
                    }
                }
                WC = WC * SNEAK_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST;//since we are placing, we are sneaking
                return WC + PLACE_ONE_BLOCK_COST + getTotalHardnessOfBlocksToBreak(ts);
            }
            return COST_INF;
            //Out.log("Can't walk on " + Baritone.get(positionsToPlace[0]).getBlock());
        }
    }

    boolean wasTheBridgeBlockAlwaysThere = true;//did we have to place a bridge block or was it always there
    public Boolean oneInTen = null;//a one in ten chance

    public boolean amIGood() {
        return canWalkThrough(positionsToBreak[0]) && canWalkThrough(positionsToBreak[1]) && canWalkOn(positionsToPlace[0]);
    }

    public int dx() {
        return to.getX() - from.getX();
    }

    public int dz() {
        return to.getZ() - from.getZ();
    }

    @Override
    protected boolean tick0() {
        if (oneInTen == null) {
            oneInTen = new Random().nextInt(10) == 0;
        }
        Block fd = Baritone.get(from.down()).getBlock();
        boolean ladder = fd instanceof BlockLadder || fd instanceof BlockVine;
        boolean isTheBridgeBlockThere = canWalkOn(positionsToPlace[0]) || ladder;
        //Out.log("is block there: " + isTheBridgeBlockThere + " block " + Baritone.get(positionsToPlace[0]).getBlock());
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        BlockPos whereAmI = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        if (whereAmI.getY() != to.getY() && !ladder) {
            Out.log("Wrong Y coordinate");
            if (whereAmI.getY() < to.getY()) {
                MovementManager.jumping = true;
            }
            return false;
        }
        if (isTheBridgeBlockThere) {//either the bridge block was there the whole time or we just placed it
            if (oneInTen && wasTheBridgeBlockAlwaysThere) {
                //basically one in every ten blocks we walk forwards normally without sneaking and placing, rotate to look forwards.
                //this way we tend towards looking forwards
                MovementManager.forward = LookManager.lookAtBlock(to, false);
            } else {
                MovementManager.moveTowardsBlock(to);
            }
            if (wasTheBridgeBlockAlwaysThere) {
                if (MovementManager.forward && !MovementManager.backward) {
                    thePlayer.setSprinting(true);
                }
            }
            if (whereAmI.equals(to)) {//if we are there
                Out.log("Done walking to " + to);
                return true;//and we are done
            }
            Out.log("Trying to get to " + to + " currently at " + whereAmI);
            return false;//not there yet
        } else {
            wasTheBridgeBlockAlwaysThere = false;
            for (BlockPos against1 : against) {
                if (Baritone.isBlockNormalCube(against1)) {
                    if (!switchtothrowaway(true)) {//get ready to place a throwaway block
                        return false;
                    }
                    MovementManager.sneak = true;
                    double faceX = (to.getX() + against1.getX() + 1.0D) * 0.5D;
                    double faceY = (to.getY() + against1.getY()) * 0.5D;
                    double faceZ = (to.getZ() + against1.getZ() + 1.0D) * 0.5D;
                    LookManager.lookAtCoords(faceX, faceY, faceZ, true);
                    EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;
                    if (Objects.equals(Baritone.whatAreYouLookingAt(), against1) && Minecraft.getMinecraft().player.isSneaking()) {
                        if (Baritone.whatAreYouLookingAt().offset(side).equals(positionsToPlace[0])) {
                            MovementManager.rightClickMouse();
                        } else {
                            Out.gui("Wrong. " + side + " " + Baritone.whatAreYouLookingAt().offset(side) + " " + positionsToPlace[0], Out.Mode.Debug);
                        }
                    }
                    Out.log("Trying to look at " + against1 + ", actually looking at" + Baritone.whatAreYouLookingAt());
                    return false;
                }
            }
            MovementManager.sneak = true;
            if (whereAmI.equals(to)) {
                //if we are in the block that we are trying to get to, we are sneaking over air and we need to place a block beneath us against the one we just walked off of
                //Out.log(from + " " + to + " " + faceX + "," + faceY + "," + faceZ + " " + whereAmI);
                if (!switchtothrowaway(true)) {//get ready to place a throwaway block
                    return false;
                }
                double faceX = (to.getX() + from.getX() + 1.0D) * 0.5D;
                double faceY = (to.getY() + from.getY() - 1.0D) * 0.5D;
                double faceZ = (to.getZ() + from.getZ() + 1.0D) * 0.5D;
                //faceX,faceY,faceZ is the middle of the face between from and to
                BlockPos goalLook = from.down();//this is the block we were just standing on, and the one we want to place against
                MovementManager.backward = LookManager.lookAtCoords(faceX, faceY, faceZ, true);//if we are in the block, then we are off the edge of the previous looking backward, so we should be moving backward
                if (Objects.equals(Baritone.whatAreYouLookingAt(), goalLook)) {
                    MovementManager.rightClickMouse();//wait to right click until we are able to place
                    return false;
                }
                Out.log("Trying to look at " + goalLook + ", actually looking at" + Baritone.whatAreYouLookingAt());
                return false;
            } else {
                Out.log("Not there yet m9");
                MovementManager.moveTowardsBlock(to);//move towards not look at because if we are bridging for a couple blocks in a row, it is faster if we dont spin around and walk forwards then spin around and place backwards for every block
                return false;
            }
        }
    }
}
