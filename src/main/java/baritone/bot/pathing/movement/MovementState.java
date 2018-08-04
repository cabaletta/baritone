package baritone.bot.pathing.movement;

import baritone.bot.InputOverrideHandler.Input;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class MovementState {

    private MovementStatus status;
    private MovementGoal goal;
    protected final Map<Input, Boolean> inputState = new HashMap<>();

    private Vec3d interme;

    public MovementState setStatus(MovementStatus status) {
        this.status = status;
        return this;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public static class MovementGoal {
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

        public MovementGoal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
        }
    }

    public MovementGoal getGoal() {
        return goal;
    }

    public MovementState setGoal(MovementGoal goal) {
        this.goal = goal;
        return this;
    }

    public MovementState setInput(Input input, boolean forced) {
        inputState.put(input, forced);
        return this;
    }

    public boolean getInput(Input input) {
        return inputState.getOrDefault(input, false);
    }

    public enum MovementStatus {
        PREPPING, WAITING, RUNNING, SUCCESS, UNREACHABLE, FAILED;
    }
}
