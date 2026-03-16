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

import baritone.api.utils.input.Input;
import net.minecraft.world.phys.Vec2;

public class PlayerMovementInput extends net.minecraft.client.player.ClientInput {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tick() {
        boolean forward = handler.isInputForcedDown(Input.MOVE_FORWARD);
        boolean backward = handler.isInputForcedDown(Input.MOVE_BACK);
        boolean left = handler.isInputForcedDown(Input.MOVE_LEFT);
        boolean right = handler.isInputForcedDown(Input.MOVE_RIGHT);
        boolean jump = handler.isInputForcedDown(Input.JUMP);
        boolean sneak = handler.isInputForcedDown(Input.SNEAK);

        this.keyPresses = new net.minecraft.world.entity.player.Input(forward, backward, left, right, jump, sneak, false);

        float forwardImpulse = (forward == backward) ? 0.0F : (forward ? 1.0F : -1.0F);
        float leftImpulse = (left == right) ? 0.0F : (left ? 1.0F : -1.0F);

        this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();
    }
}
