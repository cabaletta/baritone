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
import baritone.api.behavior.look.IAimProcessor;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.RotationMoveEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.behavior.look.AimProcessor;
import baritone.behavior.look.ForkableRandom;
import baritone.behavior.look.HumanizedRotationSmoother;
import baritone.behavior.look.VisualRotationHelper;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

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
     * The last player rotation. Used to restore the player's angle when using free look.
     *
     * @see Settings#freeLook
     */
    private Rotation prevRotation;

    private final AimProcessor processor;
    private final HumanizedRotationSmoother visualSmoother;
    private final ForkableRandom visualRandom;
    private Rotation effectiveRotation;
    private Rotation visualRotation;
    private double visualJitterYaw;
    private double visualJitterPitch;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
        this.visualSmoother = new HumanizedRotationSmoother();
        this.visualRandom = new ForkableRandom();
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(rotation, Target.Mode.resolve(ctx, blockInteract), blockInteract);
    }

    @Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {

        switch (event.getState()) {
            case PRE: {
                if (this.target == null || this.target.mode == Target.Mode.NONE) {
                    return;
                }

                this.prevRotation = this.currentVisualRotation();
                this.effectiveRotation = this.nextEffectiveRotation(this.target);
                ctx.player().setYRot(this.effectiveRotation.getYaw());
                ctx.player().setXRot(this.effectiveRotation.getPitch());
                break;
            }
            case POST: {
                Target tickTarget = this.target;
                Rotation visibleBase = this.prevRotation != null ? this.prevRotation : this.currentVisualRotation();
                if (tickTarget != null) {
                    this.updateVisualRotation(tickTarget, visibleBase);
                } else if (this.prevRotation != null) {
                    this.applyVisibleRotation(this.prevRotation, this.prevRotation);
                }

                this.prevRotation = null;
                this.target = null;
                this.effectiveRotation = null;
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) {
            return;
        }

        final ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket.Rot || packet instanceof ServerboundMovePlayerPacket.PosRot) {
            this.serverRotation = new Rotation(packet.getYRot(0.0f), packet.getXRot(0.0f));
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        this.serverRotation = null;
        this.target = null;
        this.effectiveRotation = null;
        this.visualRotation = null;
    }

    public void pig() {
        if (this.target != null) {
            final Rotation actual = this.effectiveRotation == null ? this.peekEffectiveRotation(this.target) : this.effectiveRotation;
            ctx.player().setYRot(actual.getYaw());
        }
    }

    public Optional<Rotation> getEffectiveRotation() {
        if (Baritone.settings().freeLook.value) {
            if (this.effectiveRotation != null) {
                return Optional.of(this.effectiveRotation);
            }
            return Optional.ofNullable(this.serverRotation);
        }
        // If freeLook isn't on, just defer to the player's actual rotations
        return Optional.empty();
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null) {
            final Rotation actual = this.effectiveRotation == null ? this.peekEffectiveRotation(this.target) : this.effectiveRotation;
            event.setYaw(actual.getYaw());
            event.setPitch(actual.getPitch());
        }
    }

    private Rotation nextEffectiveRotation(Target tickTarget) {
        if (tickTarget.blockInteract || ctx.player().isFallFlying()) {
            return this.processor.nextRotation(tickTarget.rotation);
        }
        return this.processor.nextRotationWithoutHumanizer(tickTarget.rotation);
    }

    private Rotation peekEffectiveRotation(Target tickTarget) {
        if (tickTarget.blockInteract || ctx.player().isFallFlying()) {
            return this.processor.peekRotation(tickTarget.rotation);
        }
        return this.processor.peekRotationForReachability(tickTarget.rotation);
    }

    private void updateVisualRotation(Target tickTarget, Rotation visibleBase) {
        Rotation from = this.visualRotation != null ? this.visualRotation : visibleBase;
        Rotation to = this.visualTarget(tickTarget, visibleBase);
        Rotation next = this.visualSmoother.next(from, to, this.visualConfig(tickTarget));
        this.visualRotation = next;
        this.applyVisibleRotation(from, next);
    }

    private Rotation visualTarget(Target tickTarget, Rotation visibleBase) {
        if (tickTarget != null && tickTarget.mode == Target.Mode.CLIENT && tickTarget.blockInteract && this.effectiveRotation != null) {
            return this.effectiveRotation;
        }
        if (tickTarget != null && tickTarget.blockInteract && tickTarget.mode == Target.Mode.SERVER) {
            return visibleBase;
        }
        if (tickTarget != null && !tickTarget.blockInteract && Baritone.settings().humanizeLookMovementPosture.value && !ctx.player().isFallFlying()) {
            Rotation posture = VisualRotationHelper.movementPosture(tickTarget.rotation, Baritone.settings().humanizeLookMovementPitch.value);
            return this.addVisualJitter(posture);
        }
        if (this.effectiveRotation != null) {
            return this.effectiveRotation;
        }
        return tickTarget == null ? visibleBase : tickTarget.rotation;
    }

    private Rotation addVisualJitter(Rotation posture) {
        double jitter = Baritone.settings().humanizeLookJitter.value;
        if (jitter <= 0.0D) {
            this.visualJitterYaw = 0.0D;
            this.visualJitterPitch = 0.0D;
            return posture;
        }

        double frequency = Math.max(0.0D, Math.min(1.0D, Baritone.settings().humanizeLookVisualJitterFrequency.value));
        double targetYaw = (this.visualRandom.nextDouble() - 0.5D) * jitter;
        double targetPitch = (this.visualRandom.nextDouble() - 0.5D) * jitter;
        this.visualJitterYaw += (targetYaw - this.visualJitterYaw) * frequency;
        this.visualJitterPitch += (targetPitch - this.visualJitterPitch) * frequency;

        return new Rotation(
                posture.getYaw() + (float) this.visualJitterYaw,
                posture.getPitch() + (float) this.visualJitterPitch
        ).normalizeAndClamp();
    }

    private HumanizedRotationSmoother.Config visualConfig(Target tickTarget) {
        boolean blockInteract = tickTarget != null && tickTarget.mode == Target.Mode.CLIENT && tickTarget.blockInteract;
        double maxSpeed = blockInteract
                ? Baritone.settings().humanizeLookMaxDegreesPerTick.value
                : Baritone.settings().humanizeLookMovementMaxDegreesPerTick.value;
        return new HumanizedRotationSmoother.Config(
                Baritone.settings().humanizeLook.value,
                maxSpeed,
                Baritone.settings().humanizeLookMinDegreesPerTick.value,
                Baritone.settings().humanizeLookAcceleration.value,
                0.0D,
                0.0D,
                blockInteract ? Baritone.settings().humanizeLookOvershootChance.value : 0.0D,
                blockInteract ? Baritone.settings().humanizeLookMaxOvershoot.value : 0.0D
        );
    }

    private Rotation currentVisualRotation() {
        return new Rotation(ctx.player().getYRot(), ctx.player().getXRot());
    }

    private void applyVisibleRotation(Rotation from, Rotation to) {
        Rotation current = Baritone.settings().humanizeLookVisualInterpolation.value
                ? VisualRotationHelper.interpolate(from, to, 1.0F)
                : to;
        if (Baritone.settings().humanizeLookVisualInterpolation.value) {
            ctx.player().yRotO = from.getYaw();
            ctx.player().xRotO = from.getPitch();
        } else {
            ctx.player().yRotO = current.getYaw();
            ctx.player().xRotO = current.getPitch();
        }
        ctx.player().setYRot(current.getYaw());
        ctx.player().setXRot(current.getPitch());
    }

    private static class Target {

        public final Rotation rotation;
        public final Mode mode;
        public final boolean blockInteract;

        public Target(Rotation rotation, Mode mode, boolean blockInteract) {
            this.rotation = rotation;
            this.mode = mode;
            this.blockInteract = blockInteract;
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

            static Mode resolve(IPlayerContext ctx, boolean blockInteract) {
                final Settings settings = Baritone.settings();
                final boolean antiCheat = settings.antiCheatCompatibility.value;
                final boolean blockFreeLook = settings.blockFreeLook.value;

                if (ctx.player().isFallFlying()) {
                    // always need to set angles while flying
                    return settings.elytraFreeLook.value ? SERVER : CLIENT;
                } else if (settings.freeLook.value) {
                    // Regardless of if antiCheatCompatibility is enabled, if a blockInteract is requested then the player
                    // rotation needs to be set somehow, otherwise Baritone will halt since objectMouseOver() will just be
                    // whatever the player is mousing over visually. Let's just settle for setting it silently.
                    if (blockInteract) {
                        return blockFreeLook ? SERVER : CLIENT;
                    }
                    return antiCheat ? SERVER : NONE;
                }

                // all freeLook settings are disabled so set the angles
                return CLIENT;
            }
        }
    }
}
