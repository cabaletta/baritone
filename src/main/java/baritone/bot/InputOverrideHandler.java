package baritone.bot;

import baritone.bot.utils.Helper;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * This serves as a replacement to the old {@code MovementManager}'s
 * input overriding capabilities. It is vastly more extensible in the
 * inputs that can be overriden.
 *
 * @author Brady
 * @since 7/31/2018 11:20 PM
 */
public final class InputOverrideHandler implements Helper {

    InputOverrideHandler() {}

    /**
     * Maps keybinds to whether or not we are forcing their state down
     */
    private final Map<KeyBinding, Boolean> inputForceStateMap = new HashMap<>();

    /**
     * Maps keycodes to whether or not we are forcing their state down
     */
    private final Map<Integer, Boolean> keyCodeForceStateMap = new HashMap<>();

    /**
     * Returns whether or not we are forcing down the specified {@link KeyBinding}.
     *
     * @param key The KeyBinding object
     * @return Whether or not it is being forced down
     */
    public final boolean isInputForcedDown(KeyBinding key) {
        return inputForceStateMap.computeIfAbsent(key, k -> false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    public final void setInputForceState(Input input, boolean forced) {
        inputForceStateMap.put(input.getKeyBinding(), forced);
    }

    /**
     * A redirection in multiple places of {@link Keyboard#isKeyDown}.
     *
     * @return Whether or not the specified key is down or overriden.
     */
    public boolean isKeyDown(int keyCode) {
        return Keyboard.isKeyDown(keyCode) || keyCodeForceStateMap.computeIfAbsent(keyCode, k -> false);
    }

    /**
     * Sets whether or not the specified key code is being forced down.
     *
     * @param keyCode The key code
     * @param forced Whether or not the state is being forced
     */
    public final void setKeyForceState(int keyCode, boolean forced) {
        keyCodeForceStateMap.put(keyCode, forced);
    }

    /**
     * An {@link Enum} representing the possible inputs that we may want to force.
     */
    public enum Input {

        /**
         * The move forward input
         */
        MOVE_FORWARD(mc.gameSettings.keyBindForward),

        /**
         * The move back input
         */
        MOVE_BACK(mc.gameSettings.keyBindBack),

        /**
         * The move left input
         */
        MOVE_LEFT(mc.gameSettings.keyBindLeft),

        /**
         * The move right input
         */
        MOVE_RIGHT(mc.gameSettings.keyBindRight),

        /**
         * The attack input
         */
        CLICK_LEFT(mc.gameSettings.keyBindAttack),

        /**
         * The use item input
         */
        CLICK_RIGHT(mc.gameSettings.keyBindUseItem);

        /**
         * The actual game {@link KeyBinding} being forced.
         */
        private KeyBinding keyBinding;

        Input(KeyBinding keyBinding) {
            this.keyBinding = keyBinding;
        }

        /**
         * @return The actual game {@link KeyBinding} being forced.
         */
        public final KeyBinding getKeyBinding() {
            return this.keyBinding;
        }
    }
}
