package baritone.bot.pathing.actions;

import baritone.bot.behavior.Behavior;
import baritone.bot.utils.Utils;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockWorldState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public abstract class Action extends Behavior {

    protected ActionState currentState;

    Action(BlockPos dest) {
        BlockPos playerEyePos = new BlockPos(player.posX, player.posY+1.62, player.posZ);
        Tuple<Float, Float> desiredRotation = Utils.calcRotationFromCoords(
                Utils.calcCenterFromCoords(dest, world),
                playerEyePos);

        // There's honestly not a good reason for this (Builder Pattern), I just believed strongly in it
        currentState = new ActionState().setDesiredMovement(dest)
                .setDesiredLook(desiredRotation)
                .setFinished(false);
    }

    /**
     * Lowest denominator of the dynamic costs.
     * TODO: Investigate performant ways to assign costs to actions
     *
     * @return Cost
     */
    public double cost() { return 0; }

    @Override
    public void onTick() {
        ActionState latestState = calcState();
        currentState = latestState;
        player.setPositionAndRotation(player.posX, player.posY, player.posZ,
                latestState.desiredRotation.getFirst(), latestState.desiredRotation.getSecond());
    }

    /**
     * Calculate latest action state.
     * Gets called once a tick.
     *
     * @return
     */
    public abstract ActionState calcState();
}
