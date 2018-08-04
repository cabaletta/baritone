package baritone.bot.pathing.movement;

public interface IMovement {

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    MovementState.MovementStatus update();

}
