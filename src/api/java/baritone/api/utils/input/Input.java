/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.utils.input;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * An {@link Enum} representing the inputs that control the player's
 * behavior. This includes moving, interacting with blocks, jumping,
 * sneaking, and sprinting.
 */
public enum Input {

    /**
     * The move forward input
     */
    MOVE_FORWARD(s -> s.keyBindForward),

    /**
     * The move back input
     */
    MOVE_BACK(s -> s.keyBindBack),

    /**
     * The move left input
     */
    MOVE_LEFT(s -> s.keyBindLeft),

    /**
     * The move right input
     */
    MOVE_RIGHT(s -> s.keyBindRight),

    /**
     * The attack input
     */
    CLICK_LEFT(s -> s.keyBindAttack),

    /**
     * The use item input
     */
    CLICK_RIGHT(s -> s.keyBindUseItem),

    /**
     * The jump input
     */
    JUMP(s -> s.keyBindJump),

    /**
     * The sneak input
     */
    SNEAK(s -> s.keyBindSneak),

    /**
     * The sprint input
     */
    SPRINT(s -> s.keyBindSprint);

    /**
     * Map of {@link KeyBinding} to {@link Input}. Values should be queried through {@link #getInputForBind(KeyBinding)}
     */
    private static final Map<KeyBinding, Input> bindToInputMap = new HashMap<>();

    /**
     * The actual game {@link KeyBinding} being forced.
     */
    private final KeyBinding keyBinding;

    Input(Function<GameSettings, KeyBinding> keyBindingMapper) {
        /*

        Here, a Function is used because referring to a static field in this enum for the game instance,
        as it was before, wouldn't be possible in an Enum constructor unless the static field was in an
        interface that this class implemented. (Helper acted as this interface) I didn't feel like making
        an interface with a game instance field just to not have to do this.

         */
        this.keyBinding = keyBindingMapper.apply(Minecraft.getInstance().gameSettings);
    }

    /**
     * @return The actual game {@link KeyBinding} being forced.
     */
    public final KeyBinding getKeyBinding() {
        return this.keyBinding;
    }

    /**
     * Finds the {@link Input} constant that is associated with the specified {@link KeyBinding}.
     *
     * @param binding The {@link KeyBinding} to find the associated {@link Input} for
     * @return The {@link Input} associated with the specified {@link KeyBinding}
     */
    public static Input getInputForBind(KeyBinding binding) {
        return bindToInputMap.computeIfAbsent(binding, b -> Arrays.stream(values()).filter(input -> input.keyBinding == b).findFirst().orElse(null));
    }
}
