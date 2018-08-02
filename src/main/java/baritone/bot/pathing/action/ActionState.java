package baritone.bot.pathing.action;

import baritone.bot.InputOverrideHandler.Input;
import baritone.bot.utils.DefaultHashMap;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class ActionState {

    protected ActionStatus status;
    public ActionGoal goal;
    protected final Map<Input, Boolean> inputState = new DefaultHashMap<>(false);

    public ActionState setStatus(ActionStatus status) {
        this.status = status;
        return this;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public static class ActionGoal {
        /**
         * Necessary movement to achieve
         * <p>
         * TODO: Decide desiredMovement type
         */
        public BlockPos position;
        /**
         * Yaw and pitch angles that must be matched
         * <p>
         * getFirst()  -> YAW
         * getSecond() -> PITCH
         */
        public Tuple<Float, Float> rotation;

        public ActionGoal(BlockPos position, Tuple<Float, Float> rotation) {
            this.position = position;
            this.rotation = rotation;
        }
    }

    public ActionGoal getGoal() {
        return goal;
    }

    public ActionState setGoal(ActionGoal goal) {
        this.goal = goal;
        return this;
    }

    public ActionState setInput(Input input, boolean forced) {
        this.inputState.put(input, forced);
        return this;
    }

    public enum ActionStatus {
        WAITING, RUNNING, SUCCESS, UNREACHABLE, FAILED;
    }
}
