package baritone.bot.behavior;

import baritone.bot.event.AbstractGameEventListener;
import baritone.bot.utils.Helper;

/**
 * A generic bot behavior.
 *
 * @author Brady
 * @since 8/1/2018 6:29 PM
 */
public class Behavior implements AbstractGameEventListener, Helper {

    /**
     * Whether or not this behavior is enabled
     */
    private boolean enabled;

    /**
     * Toggles the enabled state of this {@link Behavior}.
     *
     * @return The new state.
     */
    public final boolean toggle() {
        return this.setEnabled(!this.enabled);
    }

    /**
     * Sets the enabled state of this {@link Behavior}.
     *
     * @return The new state.
     */
    public final boolean setEnabled(boolean enabled) {
        boolean newState = getNewState(this.enabled, enabled);
        if (newState == this.enabled)
            return this.enabled;

        if (this.enabled = newState) {
            onStart();
        } else {
            onCancel();
        }

        return this.enabled;
    }

    /**
     * Function to determine what the new enabled state of this
     * {@link Behavior} should be given the old state, and the
     * proposed state. Intended to be overriden by behaviors
     * that should always be active, given that the bot itself is
     * active.
     *
     * @param oldState The old state
     * @param proposedState The proposed state
     * @return The new  state
     */
    public boolean getNewState(boolean oldState, boolean proposedState) {
        return proposedState;
    }

    /**
     * @return Whether or not this {@link Behavior} is active.
     */
    public final boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Called when the state changes from disabled to enabled
     */
    public void onStart() {}

    /**
     * Called when the state changes from enabled to disabled
     */
    public void onCancel() {}
}
