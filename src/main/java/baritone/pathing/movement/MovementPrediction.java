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
        int tick = 0; // ticks from the present

        /**
         * Currently does not take into account blocks that prevent/negate fall damage (Slime, etc.)
         */
        float damageTaken = 0;

        /** Tick (from present) when we last jumped */
        private int lastJump = -10; // assume jump is allowed at initialisation

        public boolean collided = false;
        public boolean collidedVertically = false;
        public boolean collidedHorizontally = false;

        double rotationYaw;

        boolean isJumping = false;
        boolean isAirBorne;
        boolean isSneaking = false; // changed in update()
        boolean onGround;
        boolean isInWeb;

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
            isInWeb = p.world.getBlockState(p.getPosition()).getBlock() instanceof BlockWeb;

            double playerWidth = 0.3; // 0.3 in each direction
            double playerHeight = 1.8; // modified while sneaking?
            boundingBox = new AxisAlignedBB(posX - playerWidth, posY, posZ - playerWidth, posX + playerWidth, posY + playerHeight, posZ + playerWidth);
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
            return new Vec3d(posX, posY, posZ);
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
    }

    public static PredictionResult getFutureLocation(EntityPlayerSP p, MovementState state, int ticksInTheFuture) {
        PredictionResult r = new PredictionResult(p);
        for (int tick = 0; tick < ticksInTheFuture; tick++) {
            r.update(state);
        }
        return r;
    }

    // Code from Entity class
    public static void moveAndCheckCollisions(PredictionResult r) {
        double x = r.motionX;
        double y = r.motionY;
        double z = r.motionZ;

        if (r.isInWeb) {
            r.isInWeb = false;
            x *= 0.25D;
            y *= 0.05000000074505806D;
            z *= 0.25D;
            r.motionX = 0.0D;
            r.motionY = 0.0D;
            r.motionZ = 0.0D;
        }

        // save initial values
        double initX = x;
        double initY = y;
        double initZ = z;

        double stepHeight = 0.5; // How high this entity can step up when running into a block to try to get over it (currently make note the entity will always step up this amount and not just the amount needed)

        // Sneak till edge (unless a block is available to step on)
        if (r.onGround && r.isSneaking) {
            //world.getCollisionBoxes = Gets a list of bounding boxes that intersect with the provided AABB (excluding the player)
            for (; x != 0.0D && r.player.world.getCollisionBoxes(r.player, r.boundingBox.offset(x, -stepHeight, 0.0D)).isEmpty(); initX = x) {
                if (x < 0.05D && x >= -0.05D) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= 0.05D;
                } else {
                    x += 0.05D;
                }
            }

            for (; z != 0.0D && r.player.world.getCollisionBoxes(r.player, r.boundingBox.offset(0.0D, -stepHeight, z)).isEmpty(); initZ = z) {
                if (z < 0.05D && z >= -0.05D) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= 0.05D;
                } else {
                    z += 0.05D;
                }
            }

            for (; x != 0.0D && z != 0.0D && r.player.world.getCollisionBoxes(r.player, r.boundingBox.offset(x, -stepHeight, z)).isEmpty(); initZ = z) {
                if (x < 0.05D && x >= -0.05D) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= 0.05D;
                } else {
                    x += 0.05D;
                }

                initX = x;

                if (z < 0.05D && z >= -0.05D) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= 0.05D;
                } else {
                    z += 0.05D;
                }
            }
        }

        // Calculate block collisions
        List<AxisAlignedBB> list1 = r.player.world.getCollisionBoxes(r.player, r.boundingBox.expand(x, y, z));
        AxisAlignedBB axisalignedbb = r.boundingBox;

        if (y != 0.0D) {
            int k = 0;

            for (int listSize = list1.size(); k < listSize; ++k) {
                y = list1.get(k).calculateYOffset(r.boundingBox, y);
            }
            r.boundingBox = r.boundingBox.offset(0.0D, y, 0.0D);
        }

        if (x != 0.0D) {
            int j5 = 0;

            for (int listSize = list1.size(); j5 < listSize; ++j5) {
                x = list1.get(j5).calculateXOffset(r.boundingBox, x);
            }

            if (x != 0.0D) {
                r.boundingBox = r.boundingBox.offset(x, 0.0D, 0.0D);
            }
        }

        if (z != 0.0D) {
            int k5 = 0;

            for (int listSize = list1.size(); k5 < listSize; ++k5) {
                z = list1.get(k5).calculateZOffset(r.boundingBox, z);
            }

            if (z != 0.0D) {
                r.boundingBox = r.boundingBox.offset(0.0D, 0.0D, z);
            }
        }

        boolean flag = r.onGround || initY != y && initY < 0.0D;

        // if stepHeight > 0
        if (flag && (initX != x || initZ != z)) {
            double d14 = x;
            double d6 = y;
            double d7 = z;
            AxisAlignedBB oldBoundingBox = r.boundingBox;
            r.boundingBox = axisalignedbb;
            y = stepHeight;
            List<AxisAlignedBB> list = r.player.world.getCollisionBoxes(r.player, r.boundingBox.expand(initX, y, initZ));
            AxisAlignedBB axisalignedbb2 = r.boundingBox;
            AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(initX, 0.0D, initZ);
            double d8 = y;
            int j1 = 0;

            for (int k1 = list.size(); j1 < k1; ++j1) {
                d8 = list.get(j1).calculateYOffset(axisalignedbb3, d8);
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0D, d8, 0.0D);
            double d18 = initX;
            int l1 = 0;

            for (int i2 = list.size(); l1 < i2; ++l1) {
                d18 = list.get(l1).calculateXOffset(axisalignedbb2, d18);
            }

            axisalignedbb2 = axisalignedbb2.offset(d18, 0.0D, 0.0D);
            double d19 = initZ;
            int j2 = 0;

            for (int k2 = list.size(); j2 < k2; ++j2) {
                d19 = list.get(j2).calculateZOffset(axisalignedbb2, d19);
            }

            axisalignedbb2 = axisalignedbb2.offset(0.0D, 0.0D, d19);
            AxisAlignedBB axisalignedbb4 = r.boundingBox;
            double d20 = y;
            int l2 = 0;

            for (int i3 = list.size(); l2 < i3; ++l2) {
                d20 = list.get(l2).calculateYOffset(axisalignedbb4, d20);
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0D, d20, 0.0D);
            double d21 = initX;
            int j3 = 0;

            for (int k3 = list.size(); j3 < k3; ++j3) {
                d21 = list.get(j3).calculateXOffset(axisalignedbb4, d21);
            }

            axisalignedbb4 = axisalignedbb4.offset(d21, 0.0D, 0.0D);
            double d22 = initZ;
            int l3 = 0;

            for (int i4 = list.size(); l3 < i4; ++l3) {
                d22 = list.get(l3).calculateZOffset(axisalignedbb4, d22);
            }

            axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d22);
            double d23 = d18 * d18 + d19 * d19;
            double d9 = d21 * d21 + d22 * d22;

            if (d23 > d9) {
                x = d18;
                z = d19;
                y = -d8;
                r.boundingBox = axisalignedbb2;
            } else {
                x = d21;
                z = d22;
                y = -d20;
                r.boundingBox = axisalignedbb4;
            }

            int j4 = 0;

            for (int k4 = list.size(); j4 < k4; ++j4) {
                y = list.get(j4).calculateYOffset(r.boundingBox, y);
            }

            r.boundingBox = r.boundingBox.offset(0.0D, y, 0.0D);

            if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                x = d14;
                y = d6;
                z = d7;
                r.boundingBox = oldBoundingBox;
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
                blockpos = blockpos1;
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

    /*
     * Code from Entity and EntityLivingBase
     * Unnecessary or unrelated code is stripped out
     *
     * Modified from:
     * https://www.mcpk.wiki/wiki/45_Strafe
     */

    public static void onLivingUpdate(PredictionResult r, MovementState state) {
        /*
         * moveStrafing and moveForward represent relative movement.
         * moveStrafing = 1.0 if moving left, -1.0 if moving right, else 0.0
         * moveForward = 1.0 if moving forward, -1.0 if moving backward, else 0.0
         *
         * Furthermore, moveStrafing and moveForward *= 0.3 if the player is sneaking.
         */

        float moveStrafing = 0;
        if (state.getInputStates().getOrDefault(Input.MOVE_LEFT, false)) {
            moveStrafing += 1;
        }
        if (state.getInputStates().getOrDefault(Input.MOVE_RIGHT, false)) {
            moveStrafing -= 1;
        }

        float moveForward = 0;
        if (state.getInputStates().getOrDefault(Input.MOVE_FORWARD, false)) {
            moveForward += 1;
        }
        if (state.getInputStates().getOrDefault(Input.MOVE_BACK, false)) {
            moveForward -= 1;
        }

        if (r.isSneaking) {
            moveForward *= 0.3;
            moveStrafing *= 0.3;
        }

        moveStrafing *= 0.98F;
        moveForward *= 0.98F;

        moveEntityWithHeading(r, moveStrafing, moveForward, state);
    }

    public static void moveEntityWithHeading(PredictionResult r, float strafe, float forward, MovementState state) {
        // inertia determines how much speed is conserved on the next tick
        float inertia = 0.91F;
        if (r.onGround) {
            inertia = r.player.world.getBlockState(new BlockPos(MathHelper.floor(r.posX), MathHelper.floor(r.boundingBox.minY) - 1, MathHelper.floor(r.posZ))).getBlock().slipperiness * 0.91F; // -1 is 0.5 in 1.15+
        }

        // acceleration = (0.6*0.91)^3 / (slipperiness*0.91)^3) -> redundant calculations...
        float acceleration = 0.16277136F / (inertia * inertia * inertia);

        double movementFactor;
        if (r.onGround) {
            float landMovementFactor = 0.1F;
            if (state.getInputStates().getOrDefault(Input.SPRINT, false)) {
                landMovementFactor *= 1.3F;
            }

            movementFactor = landMovementFactor * acceleration * (r.getPotionAmplifier(MobEffects.SPEED) * 0.2 - r.getPotionAmplifier(MobEffects.SLOWNESS) * 0.15 + 1);
            /* base: 0.1; x1.3 if sprinting, affected by potion effects. */
        } else {
            float airMovementFactor = 0.02F;
            if (state.getInputStates().getOrDefault(Input.SPRINT, false)) {
                airMovementFactor *= 1.3F;
            }
            movementFactor = airMovementFactor;
            /* base: 0.02; x1.3 if sprinting */
        }

        updateMotionXZ(r, strafe, forward, (float) movementFactor); /* add relative movement to motion */

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

    public static void updateMotionXZ(PredictionResult r, float strafe, float forward, float movementFactor) {
        /*
         * This function is extremely weird, and is solely responsible for the existence of 45° strafe
         * Also note that:
         *     - Sprint boost is contained within "movementFactor"
         *     - Sneak slowdown is contained within "strafe" and "forward"
         */
        float distance = strafe * strafe + forward * forward;
        if (distance >= 1.0E-4F) {
            distance = MathHelper.sqrt(distance);

            if (distance < 1.0F)
                distance = 1.0F;

            distance = movementFactor / distance;
            strafe = strafe * distance;
            forward = forward * distance;
            float sinYaw = MathHelper.sin((float) (r.rotationYaw * Math.PI / 180.0F));
            float cosYaw = MathHelper.cos((float) (r.rotationYaw * Math.PI / 180.0F));
            r.motionX += strafe * cosYaw - forward * sinYaw;
            r.motionZ += forward * cosYaw + strafe * sinYaw;
        }
    }
}
