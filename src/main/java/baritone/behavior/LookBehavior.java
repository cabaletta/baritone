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
import baritone.api.behavior.look.ITickableAimProcessor;
import baritone.api.event.events.*;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.behavior.look.ForkableRandom;
import baritone.utils.BaritoneMath;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayDeque;
import java.util.Deque;
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

    /**
     * the interpolation's current stage, to be used by {@link #onPlayerUpdate(PlayerUpdateEvent)} whenever {@link Settings#interpolatedLook} is true.
     */
    private static int stage = 0;
    private static Vec3 Source;
    private static Vec3 Destiny;
    private static Vec3 Center;

    //private final Deque<Float> smoothYawBuffer;
    //private final Deque<Float> smoothPitchBuffer;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
        this.smoothYawBuffer = new ArrayDeque<>();
        this.smoothPitchBuffer = new ArrayDeque<>();
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(rotation, Target.Mode.resolve(ctx, blockInteract));
    }

    @Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.IN) {
            this.processor.tick();
        }
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {

        if (this.target == null) {
            return;
        }

        if (Baritone.settings().interpolatedLook.value = false) {
            switch (event.getState()) {
                //PRE: onPlayerUpdate was called before rotation data was sent to the server
                case PRE: {
                    if (this.target.mode == Target.Mode.NONE) {
                        // Just return for PRE, we still want to set target to null on POST
                        return;
                    }

                    this.prevRotation = new Rotation(ctx.player().getYRot(), ctx.player().getXRot());
                    final Rotation actual = this.processor.peekRotation(this.target.rotation);
                    ctx.player().setYRot(actual.getYaw());
                    ctx.player().setXRot(actual.getPitch());
                    break;
                }
                //POST: onPlayerUpdate was called after rotation data was sent to the server
                case POST: {
                    // Reset the player's rotations back to their original values
                    if (this.prevRotation != null) {
                        if (this.target.mode == Target.Mode.SERVER) {
                            ctx.player().setYRot(this.prevRotation.getYaw());
                            ctx.player().setXRot(this.prevRotation.getPitch());
                        }// else if (ctx.player().isFallFlying() && Baritone.settings().elytraSmoothLook.value) {
                        //   ctx.player().setYRot((float) this.smoothYawBuffer.stream().mapToDouble(d -> d).average().orElse(this.prevRotation.getYaw()));
                        //   if (ctx.player().isFallFlying()) {
                        //       ctx.player().setXRot((float) this.smoothPitchBuffer.stream().mapToDouble(d -> d).average().orElse(this.prevRotation.getPitch()));
                        //   }
                        //}
                        /** TODO: reimplement elytra and falling */
                        //ctx.player().xRotO = prevRotation.getPitch();
                        //ctx.player().yRotO = prevRotation.getYaw();
                        this.prevRotation = null;
                    }
                    // The target is done being used for this game tick, so it can be invalidated
                    this.target = null;
                    break;
                }
                default:
                    break;
            }
        }else {
            // interpolatedLook == true
            switch (event.getState()) {
                case PRE: {
                    if(this.target.mode == Target.Mode.NONE) {
                        // same security case as above
                        return;
                    }

                    this.prevRotation = new Rotation(ctx.player().getYRot(), ctx.player().getXRot());
                    final Rotation actual = this.processor.peekRotation(this.target.rotation);

                    if (LookBehavior.stage == 0) {
                        LookBehavior.Center = ctx.playerHead();
                        LookBehavior.Source = (RotationUtils.calcLookDirectionFromRotation(this.prevRotation)).add(LookBehavior.Center);
                        LookBehavior.Destiny = (RotationUtils.calcLookDirectionFromRotation(actual)).add(LookBehavior.Center);

                        RotationUtils.alerp(LookBehavior.Source, LookBehavior.Destiny, LookBehavior.Center, BaritoneMath.normalize(LookBehavior.stage, Baritone.settings().interpolatedLookLength.value)); //debug, CO&PA
                        LookBehavior.stage ++;
                        /*
                        At this point, the camera rotation should still be the same as before, be it that Baritone was traveling or just exited from another mining phase.
                         */
                        break;

                    } else if (0 < LookBehavior.stage && LookBehavior.stage < Baritone.settings().interpolatedLookLength.value) {
                        final Rotation InterpD = RotationUtils.calcRotationFromVec3d(LookBehavior.Center, RotationUtils.alerp(LookBehavior.Source, LookBehavior.Destiny, LookBehavior.Center, BaritoneMath.normalize(LookBehavior.stage, Baritone.settings().interpolatedLookLength.value)), new Rotation(0, 0)); // love:slash:hate relationship with relative vectors
                        ctx.player().setYRot(InterpD.getYaw());
                        ctx.player().setXRot(InterpD.getPitch());
                        LookBehavior.stage ++;
                        /*
                        here we enter the main "loop", based on the look stage the player should be looking at any point between the path projected by alerp; at the moment of writing this I'm just assuming that Baritone does a check
                        to see if the player is looking at the block before starting to do the mining action, and is not some timer-hard-coded value.
                         */
                        break;

                    } else if (LookBehavior.stage == Baritone.settings().interpolatedLookLength.value) {
                        final Rotation InterpD = RotationUtils.calcRotationFromVec3d(LookBehavior.Center, RotationUtils.alerp(LookBehavior.Source, LookBehavior.Destiny, LookBehavior.Center, BaritoneMath.normalize(LookBehavior.stage, Baritone.settings().interpolatedLookLength.value)), new Rotation(0, 0)); // love:slash:hate relationship with relative vectors
                        ctx.player().setYRot(InterpD.getYaw());
                        ctx.player().setXRot(InterpD.getPitch());
                        LookBehavior.stage = 0;
                        LookBehavior.Source = null;
                        LookBehavior.Destiny = null;
                        /*
                        we've reached the final part of the "loop", after setting rotations we do some cleanup to be able to enter the next mining phase.
                        Center in theory shouldn't need to be nullified, but let's see...
                         */
                        break;

                    } else {
                        // wtf
                        LookBehavior.stage = 0;
                        LookBehavior.Source = null;
                        LookBehavior.Destiny = null;
                        // I hate code once debug everywhere
                        break;
                        // I really don't know how to debug this edge case
                    }
                }
                case POST: {
                    // Same as before, I really do wish that both PRE and POST are both done before frame generation, but that would be just not to trigger epilepsy
                    if (this.prevRotation != null) {
                        if (this.target.mode == Target.Mode.SERVER) {
                            ctx.player().setYRot(this.prevRotation.getYaw());
                            ctx.player().setXRot(this.prevRotation.getPitch());
                        }// else if (ctx.player().isFallFlying() && Baritone.settings().elytraSmoothLook.value) {
                        //   ctx.player().setYRot((float) this.smoothYawBuffer.stream().mapToDouble(d -> d).average().orElse(this.prevRotation.getYaw()));
                        //   if (ctx.player().isFallFlying()) {
                        //       ctx.player().setXRot((float) this.smoothPitchBuffer.stream().mapToDouble(d -> d).average().orElse(this.prevRotation.getPitch()));
                        //   }
                        //}
                        /** TODO: reimplement elytra and falling, again, for interpolatedLook */
                        //ctx.player().xRotO = prevRotation.getPitch();
                        //ctx.player().yRotO = prevRotation.getYaw();
                        if (LookBehavior.stage == Baritone.settings().interpolatedLookLength.value) {
                            this.prevRotation = null;
                        }
                    }
                    // The target is done being used for this game tick, so it can be invalidated
                    this.target = null;
                    break;
                }
                default:
                    break;
            }
            /*
             in paper(not the server software) this should work, but imma add a failed attempts counter...
             */

            //ATT1: at 10:40PM, 9/18/24 = Compile successful, executing... Stuck looking at 135.1, 0,1

            /*
            I think I had this problem before when testing alerp in a grapher, sometimes the compilers(ig?) decide that a couple of formulas are wrong because f me, in some others it does work perfectly.
            I will re-explore all the solutions per grapher.
             */
            /*
            After an embarrassingly long amount of time, I just realized I converted the angle to a vector before doing an alerp, and I need to add that vector to the player head position, otherwise the two points are near (0,0,0).
            Realized this while debugging, and at the same time I saw that the Static modifier is too powerful(not), it is kept on memory after exiting one server and entering another (including embedded, local or internal server).
             */

            //ATT2: at 3:05PM, 9/19/24 = Compile successful, executing... works.
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
        LookBehavior.stage = 0;
    }

    public void pig() {
        if (this.target != null) {
            final Rotation actual = this.processor.peekRotation(this.target.rotation);
            ctx.player().setYRot(actual.getYaw());
        }
    }

    public Optional<Rotation> getEffectiveRotation() {
        if (Baritone.settings().freeLook.value) {
            return Optional.ofNullable(this.serverRotation);
        }
        // If freeLook isn't on, just defer to the player's actual rotations
        return Optional.empty();
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null) {
            final Rotation actual = this.processor.peekRotation(this.target.rotation);
            event.setYaw(actual.getYaw());
            event.setPitch(actual.getPitch());
        }
    }

    private static final class AimProcessor extends AbstractAimProcessor {

        public AimProcessor(final IPlayerContext ctx) {
            super(ctx);
        }

        @Override
        protected Rotation getPrevRotation() {
            // Implementation will use LookBehavior.serverRotation
            return ctx.playerRotations();
        }
    }

    private static abstract class AbstractAimProcessor implements ITickableAimProcessor {

        protected final IPlayerContext ctx;
        private final ForkableRandom rand;
        private double randomYawOffset;
        private double randomPitchOffset;

        public AbstractAimProcessor(IPlayerContext ctx) {
            this.ctx = ctx;
            this.rand = new ForkableRandom();
        }

        private AbstractAimProcessor(final AbstractAimProcessor source) {
            this.ctx = source.ctx;
            this.rand = source.rand.fork();
            this.randomYawOffset = source.randomYawOffset;
            this.randomPitchOffset = source.randomPitchOffset;
        }

        @Override
        public final Rotation peekRotation(final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            float desiredYaw = rotation.getYaw();
            float desiredPitch = rotation.getPitch();

            // In other words, the target doesn't care about the pitch, so it used playerRotations().getPitch()
            // and it's safe to adjust it to a normal level
            if (desiredPitch == prev.getPitch()) {
                desiredPitch = nudgeToLevel(desiredPitch);
            }

            desiredYaw += this.randomYawOffset;
            desiredPitch += this.randomPitchOffset;

            return new Rotation(
                    this.calculateMouseMove(prev.getYaw(), desiredYaw),
                    this.calculateMouseMove(prev.getPitch(), desiredPitch)
            ).clamp();
        }

        @Override
        public final void tick() {
            // randomLooking
            this.randomYawOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;
            this.randomPitchOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;

            // randomLooking113
            double random = this.rand.nextDouble() - 0.5;
            if (Math.abs(random) < 0.1) {
                random *= 4;
            }
            this.randomYawOffset += random * Baritone.settings().randomLooking113.value;
        }

        @Override
        public final void advance(int ticks) {
            for (int i = 0; i < ticks; i++) {
                this.tick();
            }
        }

        @Override
        public Rotation nextRotation(final Rotation rotation) {
            final Rotation actual = this.peekRotation(rotation);
            this.tick();
            return actual;
        }

        @Override
        public final ITickableAimProcessor fork() {
            return new AbstractAimProcessor(this) {

                private Rotation prev = AbstractAimProcessor.this.getPrevRotation();

                @Override
                public Rotation nextRotation(final Rotation rotation) {
                    return (this.prev = super.nextRotation(rotation));
                }

                @Override
                protected Rotation getPrevRotation() {
                    return this.prev;
                }
            };
        }

        protected abstract Rotation getPrevRotation();

        /**
         * Nudges the player's pitch to a regular level. (Between {@code -20} and {@code 10}, increments are by {@code 1})
         */
        private float nudgeToLevel(float pitch) {
            if (pitch < -20) {
                return pitch + 1;
            } else if (pitch > 10) {
                return pitch - 1;
            }
            return pitch;
        }

        private float calculateMouseMove(float current, float target) {
            final float delta = target - current;
            final double deltaPx = angleToMouse(delta); // yes, even the mouse movements use double
            return current + mouseToAngle(deltaPx);
        }

        private double angleToMouse(float angleDelta) {
            final float minAngleChange = mouseToAngle(1);
            return Math.round(angleDelta / minAngleChange);
        }

        private float mouseToAngle(double mouseDelta) {
            // casting float literals to double gets us the precise values used by mc
            final double f = ctx.minecraft().options.sensitivity().get() * (double) 0.6f + (double) 0.2f;
            return (float) (mouseDelta * f * f * f * 8.0d) * 0.15f; // yes, one double and one float scaling factor
        }
    }

    private static class Target {

        public final Rotation rotation;
        public final Mode mode;

        public Target(Rotation rotation, Mode mode) {
            this.rotation = rotation;
            this.mode = mode;
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
