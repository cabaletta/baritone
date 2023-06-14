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
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import net.minecraft.network.play.client.CPacketPlayer;

import java.util.Optional;

public final class LookBehavior extends Behavior implements ILookBehavior {

    /**
     * The current look target, may be {@code null}.
     */
    private Target target;

    /**
     * The rotation known to the server. Returned by {@link #getEffectiveRotation()} for use in {@link IPlayerContext}.
     */
    private Rotation serverRotation;

    /**
     * The last player rotation. Used when free looking
     *
     * @see Settings#freeLook
     */
    private Rotation prevRotation;

    public LookBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(rotation, blockInteract);
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.target == null) {
            return;
        }
        switch (event.getState()) {
            case PRE: {
                if (this.target.mode == Target.Mode.NONE) {
                    return;
                }
                if (this.target.mode == Target.Mode.SERVER) {
                    this.prevRotation = new Rotation(ctx.player().rotationYaw, ctx.player().rotationPitch);
                }

                final float oldYaw = ctx.playerRotations().getYaw();
                final float oldPitch = ctx.playerRotations().getPitch();

                float desiredYaw = this.target.rotation.getYaw();
                float desiredPitch = this.target.rotation.getPitch();

                // In other words, the target doesn't care about the pitch, so it used playerRotations().getPitch()
                // and it's safe to adjust it to a normal level
                if (desiredPitch == oldPitch) {
                    desiredPitch = nudgeToLevel(desiredPitch);
                }

                desiredYaw += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
                desiredPitch += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;

                ctx.player().rotationYaw = calculateMouseMove(oldYaw, desiredYaw);
                ctx.player().rotationPitch = calculateMouseMove(oldPitch, desiredPitch);

                if (this.target.mode == Target.Mode.CLIENT) {
                    // The target can be invalidated now since it won't be needed for RotationMoveEvent
                    this.target = null;
                }
                break;
            }
            case POST: {
                // Reset the player's rotations back to their original values
                if (this.prevRotation != null) {
                    ctx.player().rotationYaw = this.prevRotation.getYaw();
                    ctx.player().rotationPitch = this.prevRotation.getPitch();
                    this.prevRotation = null;
                }
                // The target is done being used for this game tick, so it can be invalidated
                this.target = null;
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof CPacketPlayer)) {
            return;
        }

        final CPacketPlayer packet = (CPacketPlayer) event.getPacket();
        if (packet instanceof CPacketPlayer.Rotation || packet instanceof CPacketPlayer.PositionRotation) {
            this.serverRotation = new Rotation(packet.getYaw(0.0f), packet.getPitch(0.0f));
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        this.serverRotation = null;
        this.target = null;
    }

    public void pig() {
        if (this.target != null) {
            ctx.player().rotationYaw = this.target.rotation.getYaw();
        }
    }

    public Optional<Rotation> getEffectiveRotation() {
        if (Baritone.settings().freeLook.value || Baritone.settings().blockFreeLook.value) {
            return Optional.ofNullable(this.serverRotation);
        }
        // If neither of the freeLook settings are on, just defer to the player's actual rotations
        return Optional.empty();
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null) {
            event.setYaw(this.target.rotation.getYaw());
        }
    }

    /**
     * Nudges the player's pitch to a regular level. (Between {@code -20} and {@code 10}, increments are by {@code 1})
     */
    private static float nudgeToLevel(float pitch) {
        if (pitch < -20) {
            return pitch + 1;
        } else if (pitch > 10) {
            return pitch - 1;
        }
        return pitch;
    }

    private float calculateMouseMove(float current, float target) {
        final float delta = target - current;
        final int deltaPx = angleToMouse(delta);
        return current + mouseToAngle(deltaPx);
    }

    private int angleToMouse(float angleDelta) {
        final float minAngleChange = mouseToAngle(1);
        return Math.round(angleDelta / minAngleChange);
    }

    private float mouseToAngle(int mouseDelta) {
        final float f = ctx.minecraft().gameSettings.mouseSensitivity * 0.6f + 0.2f;
        return mouseDelta * f * f * f * 8.0f * 0.15f;
    }

    private static class Target {

        public final Rotation rotation;
        public final Mode mode;

        public Target(Rotation rotation, boolean blockInteract) {
            this.rotation = rotation;
            this.mode = Mode.resolve(blockInteract);
        }

        enum Mode {
            /**
             * Rotation will be set client-side and is visual to the player
             */
            CLIENT,

            /**
             * Rotation will be set server-side and is silent to the player
             */
            SERVER,

            /**
             * Rotation will remain unaffected on both the client and server
             */
            NONE;

            static Mode resolve(boolean blockInteract) {
                final Settings settings = Baritone.settings();
                final boolean antiCheat = settings.antiCheatCompatibility.value;
                final boolean blockFreeLook = settings.blockFreeLook.value;
                final boolean freeLook = settings.freeLook.value;

                if (!freeLook && !blockFreeLook) return CLIENT;
                if (!blockFreeLook && blockInteract) return CLIENT;

                // Regardless of if antiCheatCompatibility is enabled, if a blockInteract is requested then the player
                // rotation needs to be set somehow, otherwise Baritone will halt since objectMouseOver() will just be
                // whatever the player is mousing over visually. Let's just settle for setting it silently.
                if (antiCheat || blockInteract) return SERVER;

                // Pathing regularly without antiCheatCompatibility, don't set the player rotation
                return NONE;
            }
        }
    }
}
