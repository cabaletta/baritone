/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import net.minecraft.client.settings.KeyBinding;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface with the game's control system allowing the ability to
 * force down certain controls, having the same effect as if we were actually
 * physically forcing down the assigned key.
 *
 * @author Brady
 * @since 7/31/2018 11:20 PM
 */
public final class InputOverrideHandler implements Helper {

    public InputOverrideHandler() {}

    /**
     * Maps keybinds to whether or not we are forcing their state down.
     */
    private final Map<KeyBinding, Boolean> inputForceStateMap = new HashMap<>();

    public final void clearAllKeys() {
        inputForceStateMap.clear();
    }

    /**
     * Returns whether or not we are forcing down the specified {@link KeyBinding}.
     *
     * @param key The KeyBinding object
     * @return Whether or not it is being forced down
     */
    public final boolean isInputForcedDown(KeyBinding key) {
        return inputForceStateMap.getOrDefault(key, false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    public final void setInputForceState(Input input, boolean forced) {
        inputForceStateMap.put(input.getKeyBinding(), forced);
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
        CLICK_RIGHT(mc.gameSettings.keyBindUseItem),

        /**
         * The jump input
         */
        JUMP(mc.gameSettings.keyBindJump),

        /**
         * The sneak input
         */
        SNEAK(mc.gameSettings.keyBindSneak),

        /**
         * The sprint input
         */
        SPRINT(mc.gameSettings.keyBindSprint);

        /**
         * The actual game {@link KeyBinding} being forced.
         */
        private final KeyBinding keyBinding;

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
