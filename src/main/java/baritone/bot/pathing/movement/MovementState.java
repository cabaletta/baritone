package baritone.bot.pathing.movement;

import baritone.bot.InputOverrideHandler.Input;
import baritone.bot.utils.Rotation;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MovementState {

    private MovementStatus status;
    private MovementTarget goal = new MovementTarget();
    private MovementTarget target = new MovementTarget();
    protected final Map<Input, Boolean> inputState = new HashMap<>();

    public MovementState setStatus(MovementStatus status) {
        this.status = status;
        return this;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public static class MovementTarget {
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
        public Rotation rotation;

        public MovementTarget() {
            this(null, null);
        }

        public MovementTarget(Vec3d position) {
            this(position, null);
        }

        public MovementTarget(Rotation rotation) {
            this(null, rotation);
        }

        public MovementTarget(Vec3d position, Rotation rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        public final Optional<Vec3d> getPosition() {
            return Optional.of(this.position);
        }

        public final Optional<Rotation> getRotation() {
            return Optional.of(this.rotation);
        }
    }

    public MovementTarget getGoal() {
        return goal;
    }

    public MovementState setGoal(MovementTarget goal) {
        this.goal = goal;
        return this;
    }

    public MovementTarget getTarget() {
        return target;
    }

    public MovementState setTarget(MovementTarget target) {
        this.target = target;
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
        PREPPING, WAITING, RUNNING, SUCCESS, UNREACHABLE, FAILED, CANCELED;
    }
}
