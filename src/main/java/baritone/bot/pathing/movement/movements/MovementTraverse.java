package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class MovementTraverse extends Movement {

    private BlockPos[] against = new BlockPos[3];

    /**
     * Did we have to place a bridge block or was it always there
     */
    private boolean wasTheBridgeBlockAlwaysThere = true;

    public MovementTraverse(BlockPos from, BlockPos to) {
        super(from, to, new BlockPos[]{to.up(), to}, new BlockPos[]{to.down()});
        int i = 0;
        if (!to.north().equals(from))
            against[i++] = to.north().down();

        if (!to.south().equals(from))
            against[i++] = to.south().down();

        if (!to.east().equals(from))
            against[i++] = to.east().down();

        if (!to.west().equals(from))
            against[i] = to.west().down();

        //note: do NOT add ability to place against .down().down()
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        IBlockState pb0 = BlockStateInterface.get(positionsToBreak[0]);
        IBlockState pb1 = BlockStateInterface.get(positionsToBreak[1]);
        double WC = BlockStateInterface.isWater(pb0.getBlock()) || BlockStateInterface.isWater(pb1.getBlock()) ? WALK_ONE_IN_WATER_COST : WALK_ONE_BLOCK_COST;
        if (MovementHelper.canWalkOn(positionsToPlace[0])) {//this is a walk, not a bridge
            if (MovementHelper.canWalkThrough(positionsToBreak[0]) && MovementHelper.canWalkThrough(positionsToBreak[1])) {
                return WC;
            }
            //double hardness1 = blocksToBreak[0].getBlockHardness(Minecraft.getMinecraft().world, positionsToBreak[0]);
            //double hardness2 = blocksToBreak[1].getBlockHardness(Minecraft.getMinecraft().world, positionsToBreak[1]);
            //Out.log("Can't walk through " + blocksToBreak[0] + " (hardness" + hardness1 + ") or " + blocksToBreak[1] + " (hardness " + hardness2 + ")");
            return WC + getTotalHardnessOfBlocksToBreak(ts);
        } else {//this is a bridge, so we need to place a block
            //return 1000000;
            Block f = BlockStateInterface.get(src.down()).getBlock();
            if (f instanceof BlockLadder || f instanceof BlockVine) {
                return COST_INF;
            }
            IBlockState pp0 = BlockStateInterface.get(positionsToPlace[0]);
            if (pp0.getBlock().equals(Blocks.AIR) || (!BlockStateInterface.isWater(pp0.getBlock()) && pp0.getBlock().isReplaceable(Minecraft.getMinecraft().world, positionsToPlace[0]))) {
                for (BlockPos against1 : against) {
                    if (BlockStateInterface.get(against1).isBlockNormalCube()) {
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

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        System.out.println("Ticking with state " + state.getStatus());
        System.out.println(state.getTarget().rotation);
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
            case RUNNING:
                break;
            default:
                return state;
        }
        Block fd = BlockStateInterface.get(src.down()).getBlock();
        boolean ladder = fd instanceof BlockLadder || fd instanceof BlockVine;
        boolean isTheBridgeBlockThere = MovementHelper.canWalkOn(positionsToPlace[0]) || ladder;
        BlockPos whereAmI = playerFeet();
        if (whereAmI.getY() != dest.getY() && !ladder) {
            System.out.println("Wrong Y coordinate");
            if (whereAmI.getY() < dest.getY()) {
                state.setInput(InputOverrideHandler.Input.JUMP, true);
            }
            return state;
        }
        if (isTheBridgeBlockThere) {
            if (playerFeet().equals(dest)) {
                state.setStatus(MovementState.MovementStatus.SUCCESS);
                return state;
            }
            if (wasTheBridgeBlockAlwaysThere) {
                // player().setSprinting(true);
            }
            moveTowards(positionsToBreak[0]);
            return state;
        } else {
            wasTheBridgeBlockAlwaysThere = false;
            for (BlockPos against1 : against) {
                if (BlockStateInterface.get(against1).isBlockNormalCube()) {
                    if (!MovementHelper.switchtothrowaway()) { // get ready to place a throwaway block
                        displayChatMessageRaw("bb pls get me some blocks. dirt or cobble");
                        return state;
                    }
                    state.setInput(InputOverrideHandler.Input.SNEAK, true);
                    double faceX = (dest.getX() + against1.getX() + 1.0D) * 0.5D;
                    double faceY = (dest.getY() + against1.getY()) * 0.5D;
                    double faceZ = (dest.getZ() + against1.getZ() + 1.0D) * 0.5D;
                    state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations())));

                    EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;
                    if (Objects.equals(LookBehaviorUtils.getSelectedBlock().orElse(null), against1) && Minecraft.getMinecraft().player.isSneaking()) {
                        if (LookBehaviorUtils.getSelectedBlock().get().offset(side).equals(positionsToPlace[0])) {
                            state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true);
                        } else {
                            // Out.gui("Wrong. " + side + " " + LookBehaviorUtils.getSelectedBlock().get().offset(side) + " " + positionsToPlace[0], Out.Mode.Debug);
                        }
                    }
                    System.out.println("Trying to look at " + against1 + ", actually looking at" + LookBehaviorUtils.getSelectedBlock());
                    return state;
                }
            }
            state.setInput(InputOverrideHandler.Input.SNEAK, true);
            if (whereAmI.equals(dest)) {
                // if we are in the block that we are trying to get to, we are sneaking over air and we need to place a block beneath us against the one we just walked off of
                // Out.log(from + " " + to + " " + faceX + "," + faceY + "," + faceZ + " " + whereAmI);
                if (!MovementHelper.switchtothrowaway()) {// get ready to place a throwaway block
                    displayChatMessageRaw("bb pls get me some blocks. dirt or cobble");
                    return state;
                }
                double faceX = (dest.getX() + src.getX() + 1.0D) * 0.5D;
                double faceY = (dest.getY() + src.getY() - 1.0D) * 0.5D;
                double faceZ = (dest.getZ() + src.getZ() + 1.0D) * 0.5D;
                // faceX, faceY, faceZ is the middle of the face between from and to
                BlockPos goalLook = src.down(); // this is the block we were just standing on, and the one we want to place against
                state.setTarget(new MovementState.MovementTarget(Utils.calcRotationFromVec3d(playerHead(), new Vec3d(faceX, faceY, faceZ), playerRotations())));

                state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                state.setInput(InputOverrideHandler.Input.SNEAK, true);
                if (Objects.equals(LookBehaviorUtils.getSelectedBlock().orElse(null), goalLook)) {
                    state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true); // wait to right click until we are able to place
                    return state;
                }
                // Out.log("Trying to look at " + goalLook + ", actually looking at" + Baritone.whatAreYouLookingAt());
                return state;
            } else {
                moveTowards(positionsToBreak[0]);
                return state;
                // TODO MovementManager.moveTowardsBlock(to); // move towards not look at because if we are bridging for a couple blocks in a row, it is faster if we dont spin around and walk forwards then spin around and place backwards for every block
            }
        }
    }
}
