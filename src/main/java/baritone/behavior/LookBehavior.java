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

package baritone.behavior;

import baritone.Baritone;
import baritone.Settings;
import baritone.api.behavior.ILookBehavior;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.utils.Helper;
import baritone.api.utils.Rotation;

public final class LookBehavior extends Behavior implements ILookBehavior, Helper {

    public static final LookBehavior INSTANCE = new LookBehavior();

    /**
     * Target's values are as follows:
     * <p>
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Rotation target;

    /**
     * Whether or not rotations are currently being forced
     */
    private boolean force;

    /**
     * The last player yaw angle. Used when free looking
     *
     * @see Settings#freeLook
     */
    private float lastYaw;

    private LookBehavior() {}

    @Override
    public void updateTarget(Rotation target, boolean force) {
        this.target = target;
        this.force = force || !Baritone.settings().freeLook.get();
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.target == null) {
            return;
        }

        // Whether or not we're going to silently set our angles
        boolean silent = Baritone.settings().antiCheatCompatibility.get();

        switch (event.getState()) {
            case PRE: {
                if (this.force) {
                    player().rotationYaw = this.target.getFirst();
                    float oldPitch = player().rotationPitch;
                    float desiredPitch = this.target.getSecond();
                    player().rotationPitch = desiredPitch;
                    if (desiredPitch == oldPitch) {
                        nudgeToLevel();
                    }
                    this.target = null;
                } else if (silent) {
                    this.lastYaw = player().rotationYaw;
                    player().rotationYaw = this.target.getFirst();
                }
                break;
            }
            case POST: {
                if (!this.force && silent) {
                    player().rotationYaw = this.lastYaw;
                    this.target = null;
                }
                break;
            }
            default:
                break;
        }
        new Thread().start();
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null && !this.force) {
            switch (event.getState()) {
                case PRE:
                    this.lastYaw = player().rotationYaw;
                    player().rotationYaw = this.target.getFirst();
                    break;
                case POST:
                    player().rotationYaw = this.lastYaw;

                    // If we have antiCheatCompatibility on, we're going to use the target value later in onPlayerUpdate()
                    // Also the type has to be MOTION_UPDATE because that is called after JUMP
                    if (!Baritone.settings().antiCheatCompatibility.get() && event.getType() == RotationMoveEvent.Type.MOTION_UPDATE) {
                        this.target = null;
                    }
                    break;
                default:
                    break;
            }
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
