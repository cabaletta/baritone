package baritone.bot.pathing.movement;

import baritone.bot.InputOverrideHandler.Input;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MovementState {

    protected MovementStatus status;
    public MovementGoal goal = new MovementGoal();
    protected final Map<Input, Boolean> inputState = new HashMap<>();

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
        public Optional<Vec3d> position;
        /**
         * Yaw and pitch angles that must be matched
         * <p>
         * getFirst()  -> YAW
         * getSecond() -> PITCH
         */
        public Optional<Vec3d> rotation;

        public MovementGoal() {
            this.position = Optional.empty();
            this.rotation = Optional.empty();
        }
    }

    public MovementGoal getGoal() {
        return goal;
    }

    public MovementState setPosition(Vec3d posGoal) {
        this.goal.position = Optional.of(posGoal);
        return this;
    }

    public MovementState clearPosition() {
        this.goal.position = Optional.empty();
        return this;
    }

    public MovementState setLookDirection(Vec3d rotGoal) {
        this.goal.rotation = Optional.of(rotGoal);
        return this;
    }

    public MovementState clearLookDirection() {
        this.goal.rotation = Optional.empty();
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
        WAITING, RUNNING, SUCCESS, UNREACHABLE, FAILED;
    }
}
