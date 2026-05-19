package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;

/**
 * Some tasks may mess up royally if we interrupt them while mid air.
 * For instance, if we're doing some parkour and a baritone task is stopped,
 * the player will fall to whatever is below them, perhaps their death.
 */
public interface ITaskRequiresGrounded extends ITaskCanForce {
    @Override
    default boolean shouldForce(AltoClef mod, Task interruptingCandidate) {
        if (interruptingCandidate instanceof ITaskOverridesGrounded)
            return false;
        return !(mod.getPlayer().isOnGround() || mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isClimbing());
    }
}
