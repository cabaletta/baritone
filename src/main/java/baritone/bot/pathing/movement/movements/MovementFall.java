package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.pathing.movement.ActionCosts;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.pathing.movement.MovementState.MovementTarget;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Rotation;
import baritone.bot.utils.ToolSet;
import baritone.bot.utils.Utils;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MovementFall extends Movement {

    private static BlockPos[] buildPositionsToBreak(BlockPos src, BlockPos dest) {
        BlockPos[] toBreak;
        int diffX = src.getX() - dest.getX();
        int diffZ = src.getZ() - dest.getZ();
        int diffY = src.getY() - dest.getY();
        toBreak = new BlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BlockPos(src.getX() - diffX, src.getY() + 1 - i, src.getZ() - diffZ);
        }
        return toBreak;
    }


    public MovementFall(BlockPos src, BlockPos dest) {
        super(src, dest, MovementFall.buildPositionsToBreak(src, dest), new BlockPos[]{dest.down()});
    }

    public static Movement generateMovementFallOrDescend(BlockPos pos, EnumFacing direction) {
        BlockPos dest = pos.offset(direction);
        BlockPos destUp = dest.up();
        BlockPos destDown = dest.down();
        for (int i = 0; i < 4; i++) {
            if (!(BlockStateInterface.get(destUp.down(i)).getBlock() instanceof BlockAir)) {
                //if any of these four aren't air, that means that a fall N isn't possible
                //so try a movementdescend

                //if all four of them are air, a movementdescend isn't possible anyway
                return new MovementDescend(pos, destDown);
            }
        }
        // we're clear for a fall 2
        // let's see how far we can fall
        for (int fallHeight = 3; true; fallHeight++) {
            BlockPos onto = dest.down(fallHeight);
            if (onto.getY() <= 0) {
                break;
            }
            IBlockState fallOn = BlockStateInterface.get(onto);
            if (fallOn.getBlock() instanceof BlockAir) {
                continue;
            }
            if (BlockStateInterface.isWater(fallOn.getBlock())) {
                return new MovementFall(pos, onto);
            }
            if (MovementHelper.canWalkOn(onto)) {
                return new MovementFall(pos, onto);
            }
            break;
        }
        return null;
    }

    @Override
    protected double calculateCost(ToolSet ts) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        double placeBucketCost = 0.0;
        if (!BlockStateInterface.isWater(dest) && src.getY() - dest.getY() > 3) {
            placeBucketCost = ActionCosts.PLACE_ONE_BLOCK_COST;
        }
        double cost = getTotalHardnessOfBlocksToBreak(ts);
        if (cost != 0) {
            return COST_INF;
        }
        return WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[positionsToBreak.length - 1] + cost + placeBucketCost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);

        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
                state.setStatus(MovementStatus.RUNNING);
            case RUNNING:
                BlockPos playerFeet = playerFeet();
                if (src.getY() - dest.getY() > 3 && !BlockStateInterface.isWater(dest) && !player().inventory.hasItemStack(new ItemStack(new ItemBucket(Blocks.WATER)))) {
                    return state.setStatus(MovementStatus.UNREACHABLE);
                }
                if (playerFeet.equals(dest) && (player().posY - playerFeet.getY() < 0.01 || BlockStateInterface.isWater(dest)))
                    return state.setStatus(MovementStatus.SUCCESS);
                Vec3d destCenter = Utils.calcCenterFromCoords(dest, world());
                if (Math.abs(player().posX - destCenter.x) > 0.2 || Math.abs(player().posZ - destCenter.z) > 0.2) {
                    state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
                }
                if (!BlockStateInterface.isWater(dest) && src.getY() - dest.getY() > 3) {
                    LookBehaviorUtils.reachable(dest).ifPresent(rotation ->
                            state.setInput(InputOverrideHandler.Input.CLICK_RIGHT, true)
                                    .setTarget(new MovementTarget(rotation))
                    );
                } else {
                    Rotation rotationToBlock = Utils.calcRotationFromVec3d(playerHead(), Utils.calcCenterFromCoords(dest, world()));
                    state.setTarget(new MovementTarget(rotationToBlock));
                }
                return state;
            default:
                return state;
        }
    }

}
