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
import baritone.api.Settings;
import baritone.api.behavior.ILookBehavior;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.utils.Rotation;

public final class LookBehavior extends Behavior implements ILookBehavior {

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

    public LookBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void updateTarget(Rotation target, boolean force) {
        this.target = target;
        this.force = force || !Baritone.settings().freeLook.value;
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.target == null) {
            return;
        }

        // Whether or not we're going to silently set our angles
        boolean silent = Baritone.settings().antiCheatCompatibility.value && !this.force;

        switch (event.getState()) {
            case PRE: {
                if (this.force) {
                    ctx.player().rotationYaw = this.target.getYaw();
                    float oldPitch = ctx.player().rotationPitch;
                    float desiredPitch = this.target.getPitch();
                    ctx.player().rotationPitch = desiredPitch;
                    ctx.player().rotationYaw += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
                    ctx.player().rotationPitch += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
                    if (desiredPitch == oldPitch && !Baritone.settings().freeLook.value) {
                        nudgeToLevel();
                    }
                    this.target = null;
                }
                if (silent) {
                    this.lastYaw = ctx.player().rotationYaw;
                    ctx.player().rotationYaw = this.target.getYaw();
                }
                break;
            }
            case POST: {
                if (silent) {
                    ctx.player().rotationYaw = this.lastYaw;
                    this.target = null;
                }
                break;
            }
            default:
                break;
        }
    }

    public void pig() {
        if (this.target != null) {
            ctx.player().rotationYaw = this.target.getYaw();
        }
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null) {

            event.setYaw(this.target.getYaw());

            // If we have antiCheatCompatibility on, we're going to use the target value later in onPlayerUpdate()
            // Also the type has to be MOTION_UPDATE because that is called after JUMP
            if (!Baritone.settings().antiCheatCompatibility.value && event.getType() == RotationMoveEvent.Type.MOTION_UPDATE && !this.force) {
                this.target = null;
            }
        }
    }

    /**
     * Nudges the player's pitch to a regular level. (Between {@code -20} and {@code 10}, increments are by {@code 1})
     */
    private void nudgeToLevel() {
        if (ctx.player().rotationPitch < -20) {
            ctx.player().rotationPitch++;
        } else if (ctx.player().rotationPitch > 10) {
            ctx.player().rotationPitch--;
        }
    }
}
