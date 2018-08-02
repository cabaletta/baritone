package baritone.bot.pathing.actions;

import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

public class ActionState {

    /**
     * Is Action finished?
     */
    protected boolean finished;

    public boolean isFinished() {
        return finished;
    }

    public ActionState setFinished(boolean finished) {
        this.finished = finished;
        return this;
    }

    /**
     * Necessary movement to achieve
     *
     * TODO: Decide desiredMovement type
     */
    protected BlockPos desiredMovement;

    public BlockPos getDesiredMovement() {
        return desiredMovement;
    }

    public ActionState setDesiredMovement(BlockPos desiredMovement) {
        this.desiredMovement = desiredMovement;
        return this;
    }

    /**
     * Yaw and pitch angles that must be matched
     *
     * getFirst()  -> YAW
     * getSecond() -> PITCH
     */
    protected Tuple<Float, Float> desiredRotation;

    public Tuple<Float, Float> getDesiredRotation() {
        return desiredRotation;
    }

    public ActionState setDesiredLook(Tuple<Float, Float> desiredRotation) {
        this.desiredRotation = desiredRotation;
        return this;
    }
}
