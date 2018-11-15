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

package baritone.bot.spec;

import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.input.Input;
import baritone.bot.IBaritoneUser;
import net.minecraft.util.MovementInput;

/**
 * @author Brady
 * @since 10/29/2018
 */
public class BotMovementInput extends MovementInput {

    private final IBaritoneUser user;

    public BotMovementInput(IBaritoneUser user) {
        this.user = user;
    }

    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;

        // These are placeholders until an input overrider is implemented for bots
        IInputOverrideHandler i = user.getBaritone().getInputOverrideHandler();
        boolean forward = i.isInputForcedDown(Input.MOVE_FORWARD);
        boolean back = i.isInputForcedDown(Input.MOVE_BACK);
        boolean left = i.isInputForcedDown(Input.MOVE_LEFT);
        boolean right = i.isInputForcedDown(Input.MOVE_RIGHT);
        jump = i.isInputForcedDown(Input.JUMP); // oppa
        boolean sneak = i.isInputForcedDown(Input.SNEAK);

        if (this.forwardKeyDown = forward) {
            this.moveForward++;
        }

        if (this.backKeyDown = back) {
            this.moveForward--;
        }

        if (this.leftKeyDown = left) {
            this.moveStrafe++;
        }

        if (this.rightKeyDown = right) {
            this.moveStrafe--;
        }

        if (this.sneak = sneak) {
            this.moveStrafe *= 0.3D;
            this.moveForward *= 0.3D;
        }
    }
}
