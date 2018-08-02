package baritone.bot.pathing.action;

import baritone.bot.InputOverrideHandler.Input;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class ActionState {

    protected ActionStatus status;
    public ActionGoal goal;
    protected final Map<Input, Boolean> inputState = new HashMap<>();

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
        public Vec3d position;
        /**
         * Yaw and pitch angles that must be matched
         * <p>
         * getFirst()  -> YAW
         * getSecond() -> PITCH
         */
        public Vec3d rotation;

        public ActionGoal(Vec3d position, Vec3d rotation) {
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
        inputState.put(input, forced);
        return this;
    }

    public boolean getInput(Input input) {
        return inputState.getOrDefault(input, false);
    }

    public enum ActionStatus {
        WAITING, RUNNING, SUCCESS, UNREACHABLE, FAILED;
    }
}
