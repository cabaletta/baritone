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

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Move into MovementHelper
public class MovementPrediction {

    private MovementPrediction() {
    }

    /**
     * Mutable
     */
    public static class PredictionResult {
        EntityPlayerSP player;
        private final Map<Potion, PotionEffect> activePotionsMap;
        public int tick = 0; // ticks from the present

        /**
         * Currently does not take into account blocks that prevent/negate fall damage (Slime, etc.)
         */
        float damageTaken = 0;

        /** Tick (from present) when we last jumped */
        private int lastJump = -10; // assume jump is allowed at initialisation

        public boolean collided = false;
        public boolean collidedVertically = false;
        public boolean collidedHorizontally = false;

        public double rotationYaw;

        boolean isJumping = false;
        public boolean isAirBorne;
        boolean isSneaking = false; // changed in update()
        public boolean onGround;

        double motionX;
        double motionY;
        double motionZ;

        AxisAlignedBB boundingBox;

        private final ArrayList<Vec3d> positionCache = new ArrayList<>();

        public double posX;
        public double posY;
        public double posZ;

        float fallDistance;

        public PredictionResult(EntityPlayerSP p) {
            player = p;
            activePotionsMap = p.getActivePotionMap();

            posX = p.posX;
            posY = p.posY;
            posZ = p.posZ;

            rotationYaw = p.rotationYaw;

            motionX = p.motionX;
            motionY = p.motionY;
            motionZ = p.motionZ;

            isAirBorne = p.isAirBorne;
            onGround = p.onGround;

            double playerWidth = 0.3; // 0.3 in each direction
            double playerHeight = 1.8; // modified while sneaking?
            boundingBox = new AxisAlignedBB(posX - playerWidth, posY, posZ - playerWidth, posX + playerWidth, posY + playerHeight, posZ + playerWidth);

            positionCache.add(new Vec3d(posX, posY, posZ)); // prevent null pointers
        }

        public void resetPositionToBB() {
            this.posX = (boundingBox.minX + boundingBox.maxX) / 2.0D;
            this.posY = boundingBox.minY;
            this.posZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0D;
        }

        public void updateFallState(double y, boolean onGroundIn, IBlockState iblockstate) {
            if (onGroundIn) {

                // Change fall damage if block negates it? HayBale?
                // if iblockstate.getBlock() instanceof Block... fallDistance = 0 etc..

                // Fall damage
                float f = getPotionAmplifier(MobEffects.JUMP_BOOST);
                int i = MathHelper.ceil((fallDistance - 3.0F - f));
                damageTaken += i;

                this.fallDistance = 0.0F;
            } else if (y < 0.0D) {
                this.fallDistance = (float) ((double) this.fallDistance - y);
            }
        }

        public Vec3d getPosition() {
            return positionCache.get(tick);
        }

        public Vec3d getPosition(int tick) {
            return positionCache.get(tick);
        }

        public boolean canJump() {
            return tick - lastJump >= 10 && onGround;
        }

        /**
         * returns the Amplifier of the potion if present, 0 otherwise.
         * returns 0 if considerPotionEffects setting is false.
         * Amplifier starts at 1
         */
        public int getPotionAmplifier(Potion potionIn) {
            if (Baritone.settings().considerPotionEffects.value && isPotionActive(potionIn)) {
                return activePotionsMap.get(potionIn).getAmplifier() + 1;
            }
            return 0;
        }

        public boolean isPotionActive(Potion potionIn) {
            PotionEffect effect = activePotionsMap.get(potionIn);
            if (effect == null) {
                return false;
            }
            if (effect.getDuration() < tick) {
                activePotionsMap.remove(potionIn);
                return false;
            }
            return true;
        }

        /**
         * Calculates the next tick.
         * Updates all variables to match the predicted next tick.
         */
        public void update(MovementState state) {
            isSneaking = state.getInputStates().getOrDefault(Input.SNEAK, false);
            isJumping = canJump() && state.getInputStates().getOrDefault(Input.JUMP, false);
            if (isJumping) lastJump = tick;
            rotationYaw = state.getTarget().rotation.getYaw();
            onLivingUpdate(this, state);
            positionCache.add(new Vec3d(posX, posY, posZ));
            tick++;
        }

        /**
         * Calculates up to the given tick
         * (does not go backwards)
         * May be inaccurate with large state changes
         *
         * @param state The key presses for the duration of this calculation
         * @param tick  The tick to calculate up to
         */
        public void setTick(MovementState state, int tick) {
            for (int i = this.tick; i < tick; i++) {
                update(state);
            }
        }

        public PredictionResult recalculate(MovementState state) {
            return getFutureLocation(player, state, tick);
        }
    }

    public static PredictionResult getFutureLocation(EntityPlayerSP p, MovementState state, int ticksInTheFuture) {
        PredictionResult r = new PredictionResult(p);
        for (int tick = 0; tick < ticksInTheFuture; tick++) {
            r.update(state);
        }
        return r;
    }

    /**
     * Checks the if movement collides with blocks (no stair steps, no sneak till edge)
     *
     * @param r The player parameters to update
     */
    public static void moveAndCheckCollisions(PredictionResult r) {
        double x = r.motionX;
        double y = r.motionY;
        double z = r.motionZ;

        // save initial values
        double initX = x;
        double initY = y;
        double initZ = z;

        // Calculate block collisions
        List<AxisAlignedBB> nearbyBBs = r.player.world.getCollisionBoxes(r.player, r.boundingBox.expand(x, y, z));

        if (y != 0) {
            int i = 0;
            for (int listSize = nearbyBBs.size(); i < listSize; ++i) {
                y = nearbyBBs.get(i).calculateYOffset(r.boundingBox, y);
            }
            r.boundingBox = r.boundingBox.offset(0, y, 0);
        }

        if (x != 0) {
            int i = 0;
            for (int listSize = nearbyBBs.size(); i < listSize; ++i) {
                x = nearbyBBs.get(i).calculateXOffset(r.boundingBox, x);
            }
            if (x != 0) {
                r.boundingBox = r.boundingBox.offset(x, 0, 0);
            }
        }

        if (z != 0) {
            int i = 0;
            for (int listSize = nearbyBBs.size(); i < listSize; ++i) {
                z = nearbyBBs.get(i).calculateZOffset(r.boundingBox, z);
            }
            if (z != 0) {
                r.boundingBox = r.boundingBox.offset(0, 0, z);
            }
        }


        // Set position
        r.resetPositionToBB();

        // update some movement related variables
        r.collidedHorizontally = initX != x || initZ != z;
        r.collidedVertically = initY != y;
        r.onGround = r.collidedVertically && initY < 0.0D; // collided vertically in the downwards direction
        r.collided = r.collidedHorizontally || r.collidedVertically;

        // Check block underneath for fences/etc. that could cause fall damage early
        int blockX = MathHelper.floor(r.posX);
        int blockYdown = MathHelper.floor(r.posY - 0.20000000298023224D);
        int blockZ = MathHelper.floor(r.posZ);
        BlockPos blockpos = new BlockPos(blockX, blockYdown, blockZ);
        IBlockState iblockstate = r.player.world.getBlockState(blockpos);
        if (iblockstate.getMaterial() == Material.AIR) {
            BlockPos blockpos1 = blockpos.down();
            IBlockState iblockstate1 = r.player.world.getBlockState(blockpos1);
            Block block1 = iblockstate1.getBlock();

            if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
                iblockstate = iblockstate1;
            }
        }

        // fall damage
        r.updateFallState(y, r.onGround, iblockstate);

        // Set motion to 0 if collision occurs
        if (initX != x) {
            r.motionX = 0.0D;
        }
        if (initZ != z) {
            r.motionZ = 0.0D;
        }

        // Calculate landing collisions
        Block block = iblockstate.getBlock();
        // replaced block.onLanded()
        if (initY != y) {
            if (block instanceof BlockSlime && !r.isSneaking) {
                if (r.motionY < 0.0D) {
                    r.motionY = -r.motionY;
                }
            } else if (block instanceof BlockBed && !r.isSneaking) {
                if (r.motionY < 0.0D) {
                    r.motionY = -r.motionY * 0.6600000262260437D;
                }
            } else {
                r.motionY = 0;
            }
        }
    }

    public static void onLivingUpdate(PredictionResult r, MovementState state) {
        /*
         * moveStrafing and moveForward represent relative movement.
         * moveStrafing = 1.0 if moving left, -1.0 if moving right, else 0.0
         * moveForward = 1.0 if moving forward, -1.0 if moving backward, else 0.0
         *
         * Furthermore, moveStrafing and moveForward *= 0.3 if the player is sneaking.
         */

        double strafe = 0;
        if (state.getInputStates().getOrDefault(Input.MOVE_LEFT, false)) {
            strafe += 1;
        }
        if (state.getInputStates().getOrDefault(Input.MOVE_RIGHT, false)) {
            strafe -= 1;
        }

        double forward = 0;
        if (state.getInputStates().getOrDefault(Input.MOVE_FORWARD, false)) {
            forward += 1;
        }
        if (state.getInputStates().getOrDefault(Input.MOVE_BACK, false)) {
            forward -= 1;
        }

        if (r.isSneaking) {
            forward *= 0.3;
            strafe *= 0.3;
        }

        strafe *= 0.98F;
        forward *= 0.98F;

        // inertia determines how much speed is conserved on the next tick
        double inertia = 0.91;
        if (r.onGround) {
            inertia = r.player.world.getBlockState(new BlockPos(MathHelper.floor(r.posX), MathHelper.floor(r.boundingBox.minY) - 1, MathHelper.floor(r.posZ))).getBlock().slipperiness * 0.91F; // -1 is 0.5 in 1.15+
        }

        // acceleration = (0.6*0.91)^3 / (slipperiness*0.91)^3) -> redundant calculations...
        double acceleration = 0.16277136F / (inertia * inertia * inertia);

        double moveMod;
        if (r.onGround) {
            moveMod = 0.1 * acceleration * (r.getPotionAmplifier(MobEffects.SPEED) * 0.2 - r.getPotionAmplifier(MobEffects.SLOWNESS) * 0.15 + 1);
        } else {
            moveMod = 0.02F;
        }

        if (state.getInputStates().getOrDefault(Input.SPRINT, false)) {
            moveMod *= 1.3F;
        }

        double distance = strafe * strafe + forward * forward;
        if (distance >= 1.0E-4F) {
            distance = MathHelper.sqrt(distance);

            if (distance < 1.0F)
                distance = 1.0F;

            distance = moveMod / distance;
            strafe = strafe * distance;
            forward = forward * distance;
            float sinYaw = MathHelper.sin((float) (r.rotationYaw * Math.PI / 180.0F));
            float cosYaw = MathHelper.cos((float) (r.rotationYaw * Math.PI / 180.0F));
            r.motionX += strafe * cosYaw - forward * sinYaw;
            r.motionZ += forward * cosYaw + strafe * sinYaw;
        }

        if (r.isJumping) {
            r.motionY = 0.42 + r.getPotionAmplifier(MobEffects.JUMP_BOOST) * 0.1;
            if (state.getInputStates().getOrDefault(Input.SPRINT, false)) {
                double f = r.rotationYaw * 0.017453292; // radians
                r.motionX -= Math.sin(f) * 0.2;
                r.motionZ += Math.cos(f) * 0.2;
            }
            r.isAirBorne = true;
        }

        // new location
        moveAndCheckCollisions(r);

        // ending motion
        r.motionX *= inertia;
        r.motionZ *= inertia;
        r.motionY = (r.motionY - 0.08) * 0.98; // gravity and drag
    }
}
