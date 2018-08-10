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

package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.utils.Rotation;

public class LookBehavior extends Behavior {

    public static final LookBehavior INSTANCE = new LookBehavior();

    private LookBehavior() {}

    /**
     * Target's values are as follows:
     * <p>
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Rotation target;

    public void updateTarget(Rotation target) {
        this.target = target;
    }

    @Override
    public void onPlayerUpdate() {
        if (target != null) {
            player().rotationYaw = target.getFirst();
            float oldPitch = player().rotationPitch;
            float desiredPitch = target.getSecond();
            player().rotationPitch = desiredPitch;
            if (desiredPitch == oldPitch) {
                nudgeToLevel();
            }
            target = null;
        }
    }

    private void nudgeToLevel() {
        if (player().rotationPitch < -20) {
            player().rotationPitch++;
        } else if (player().rotationPitch > 10) {
            player().rotationPitch--;
        }
    }
}
