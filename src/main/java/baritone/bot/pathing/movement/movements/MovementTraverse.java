package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class MovementTraverse extends Movement {

    BlockPos[] against = new BlockPos[3];

    public MovementTraverse(BlockPos from, BlockPos to) {
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
        IBlockState pb0 = BlockStateInterface.get(positionsToBreak[0]);
        IBlockState pb1 = BlockStateInterface.get(positionsToBreak[1]);
        double WC = MovementHelper.isWater(pb0.getBlock()) || MovementHelper.isWater(pb1.getBlock()) ? WALK_ONE_IN_WATER_COST : WALK_ONE_BLOCK_COST;
        if (MovementHelper.canWalkOn(positionsToPlace[0], BlockStateInterface.get(positionsToPlace[0]))) {//this is a walk, not a bridge
            if (MovementHelper.canWalkThrough(positionsToBreak[0], pb0) && MovementHelper.canWalkThrough(positionsToBreak[1], pb1)) {
                return WC;
            }
            //double hardness1 = blocksToBreak[0].getBlockHardness(Minecraft.getMinecraft().world, positionsToBreak[0]);
            //double hardness2 = blocksToBreak[1].getBlockHardness(Minecraft.getMinecraft().world, positionsToBreak[1]);
            //Out.log("Can't walk through " + blocksToBreak[0] + " (hardness" + hardness1 + ") or " + blocksToBreak[1] + " (hardness " + hardness2 + ")");
            return WC + getTotalHardnessOfBlocksToBreak(ts);
        } else {//this is a bridge, so we need to place a block
            if (true) {
                System.out.println(src + " " + dest);
                return COST_INF;//TODO
            }
            //return 1000000;
            Block f = BlockStateInterface.get(src.down()).getBlock();
            if (f instanceof BlockLadder || f instanceof BlockVine) {
                return COST_INF;
            }
            IBlockState pp0 = BlockStateInterface.get(positionsToPlace[0]);
            if (pp0.getBlock().equals(Blocks.AIR) || (!MovementHelper.isWater(pp0.getBlock()) && pp0.getBlock().isReplaceable(Minecraft.getMinecraft().world, positionsToPlace[0]))) {
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
                if (playerFeet().equals(dest)) {
                    state.setStatus(MovementState.MovementStatus.SUCCESS);
                    return state;
                }
                state.setTarget(new MovementState.MovementTarget(Optional.empty(), Optional.of(Utils.calcRotationFromVec3d(new Vec3d(player().posX, player().posY, player().posZ), Utils.calcCenterFromCoords(positionsToBreak[1], world()))))).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
                return state;

            default:
                return state;
        }
    }
}
