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

package baritone.utils;

import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.input.Input;
import net.minecraft.util.MovementInput;

public class PlayerMovementInput extends MovementInput {

    private final IInputOverrideHandler handler;

    public PlayerMovementInput(IInputOverrideHandler handler) {
        this.handler = handler;
    }

    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;

        jump = handler.isInputForcedDown(Input.JUMP); // oppa gangnam style

        if (this.forwardKeyDown = handler.isInputForcedDown(Input.MOVE_FORWARD)) {
            this.moveForward++;
        }

        if (this.backKeyDown = handler.isInputForcedDown(Input.MOVE_BACK)) {
            this.moveForward--;
        }

        if (this.leftKeyDown = handler.isInputForcedDown(Input.MOVE_LEFT)) {
            this.moveStrafe++;
        }

        if (this.rightKeyDown = handler.isInputForcedDown(Input.MOVE_RIGHT)) {
            this.moveStrafe--;
        }

        if (this.sneak = handler.isInputForcedDown(Input.SNEAK)) {
            this.moveStrafe *= 0.3D;
            this.moveForward *= 0.3D;
        }
    }
}
