package baritone.ui;

import baritone.Baritone;
import baritone.pathfinding.goals.GoalXZ;
import baritone.util.Manager;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 *
 * @author leijurv
 */
public class LookManager extends Manager {

    public static boolean randomLooking = true;
    static final float MAX_YAW_CHANGE_PER_TICK = 360 / 20;
    static final float MAX_PITCH_CHANGE_PER_TICK = 360 / 20;
    static float previousYaw = 0;
    static float previousPitch = 0;
    /**
     * Something with smoothing between ticks
     */
    static float desiredNextYaw = 0;
    static float desiredNextPitch = 0;
    /**
     * The desired yaw, as set by whatever movement is happening. Remember to also
     * set lookingYaw to true if you really want the yaw to change
     *
     */
    static float desiredYaw;
    /**
     * The desired pitch, as set by whatever movement is happening. Remember to
     * also set lookingPitch to true if you really want the yaw to change
     *
     */
    static float desiredPitch;
    /**
     * Set to true if the movement wants the player's yaw to be moved towards
     * desiredYaw
     */
    static boolean lookingYaw = false;
    /**
     * Set to true if the movement wants the player's pitch to be moved towards
     * desiredPitch
     */
    static boolean lookingPitch = false;

    public static void frame(float partialTicks) {
        //Out.log("Part: " + partialTicks);
        if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().player == null) {
            return;
        }
        if (lookingPitch) {
            Minecraft.getMinecraft().player.rotationPitch = (desiredNextPitch - previousPitch) * partialTicks + previousPitch;
        }
        if (lookingYaw) {
            Minecraft.getMinecraft().player.rotationYaw = (desiredNextYaw - previousYaw) * partialTicks + previousYaw;
        }
    }
    /**
     * Because I had to do it the janky way
     */
    private static final double[][] BLOCK_SIDE_MULTIPLIERS = {{0, 0.5, 0.5}, {1, 0.5, 0.5}, {0.5, 0, 0.5}, {0.5, 1, 0.5}, {0.5, 0.5, 0}, {0.5, 0.5, 1}};

    /**
     * Called by our code in order to look in the direction of the center of a
     * block
     *
     * @param p the position to look at
     * @param alsoDoPitch whether to set desired pitch or just yaw
     * @return is the actual player yaw (and actual player pitch, if alsoDoPitch
     * is true) within ANGLE_THRESHOLD (currently 7°) of looking straight at
     * this block?
     */
    public static boolean lookAtBlock(BlockPos p, boolean alsoDoPitch) {
        if (couldIReachCenter(p)) {
            return lookAtCenterOfBlock(p, alsoDoPitch);
        }
        IBlockState b = Baritone.get(p);
        AxisAlignedBB bbox = b.getBoundingBox(Baritone.world, p);
        for (double[] mult : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = bbox.minX * mult[0] + bbox.maxX * (1 - mult[0]);//lol
            double yDiff = bbox.minY * mult[1] + bbox.maxY * (1 - mult[1]);
            double zDiff = bbox.minZ * mult[2] + bbox.maxZ * (1 - mult[2]);
            double x = p.getX() + xDiff;
            double y = p.getY() + yDiff;
            double z = p.getZ() + zDiff;
            if (couldIReachByLookingAt(p, x, y, z)) {
                return lookAtCoords(x, y, z, alsoDoPitch);
            }
        }
        return lookAtCenterOfBlock(p, alsoDoPitch);
    }

    public static boolean lookAtCenterOfBlock(BlockPos p, boolean alsoDoPitch) {
        IBlockState b = Baritone.get(p);
        AxisAlignedBB bbox = b.getBoundingBox(Baritone.world, p);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        if (b instanceof BlockFire) {//look at bottom of fire when putting it out
            yDiff = 0;
        }
        double x = p.getX() + xDiff;
        double y = p.getY() + yDiff;
        double z = p.getZ() + zDiff;
        return lookAtCoords(x, y, z, alsoDoPitch);
    }
    /**
     * The threshold for how close it tries to get to looking straight at things
     */
    public static final float ANGLE_THRESHOLD = 7;

    public static boolean couldIReach(BlockPos pos) {
        if (couldIReachCenter(pos)) {
            return true;
        }
        IBlockState b = Baritone.get(pos);
        AxisAlignedBB bbox = b.getBoundingBox(Baritone.world, pos);
        for (double[] mult : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = bbox.minX * mult[0] + bbox.maxX * (1 - mult[0]);//lol
            double yDiff = bbox.minY * mult[1] + bbox.maxY * (1 - mult[1]);
            double zDiff = bbox.minZ * mult[2] + bbox.maxZ * (1 - mult[2]);
            double x = pos.getX() + xDiff;
            double y = pos.getY() + yDiff;
            double z = pos.getZ() + zDiff;
            if (couldIReachByLookingAt(pos, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public static boolean couldIReachCenter(BlockPos pos) {
        float[] pitchAndYaw = pitchAndYawToCenter(pos);
        RayTraceResult blah = raytraceTowards(pitchAndYaw);
        return blah != null && blah.typeOfHit == RayTraceResult.Type.BLOCK && blah.getBlockPos().equals(pos);
    }

    public static boolean couldIReach(BlockPos pos, EnumFacing dir) {
        BlockPos side = pos.offset(dir);
        double faceX = (pos.getX() + side.getX() + 1.0D) * 0.5D;
        double faceY = (pos.getY() + side.getY()) * 0.5D;
        double faceZ = (pos.getZ() + side.getZ() + 1.0D) * 0.5D;
        RayTraceResult blah = raytraceTowards(faceX, faceY, faceZ);
        return blah != null && blah.typeOfHit == RayTraceResult.Type.BLOCK && blah.getBlockPos().equals(pos) && blah.sideHit == dir;
    }

    public static RayTraceResult raytraceTowards(double x, double y, double z) {
        return raytraceTowards(pitchAndYaw(x, y, z));
    }

    public static RayTraceResult raytraceTowards(float[] pitchAndYaw) {
        float yaw = pitchAndYaw[0];
        float pitch = pitchAndYaw[1];
        double blockReachDistance = (double) Minecraft.getMinecraft().playerController.getBlockReachDistance();
        Vec3d vec3 = Minecraft.getMinecraft().player.getPositionEyes(1.0F);
        Vec3d vec31 = getVectorForRotation(pitch, yaw);
        Vec3d vec32 = vec3.add(vec31.x * blockReachDistance, vec31.y * blockReachDistance, vec31.z * blockReachDistance);
        RayTraceResult blah = Minecraft.getMinecraft().world.rayTraceBlocks(vec3, vec32, false, false, true);
        return blah;
    }

    public static boolean couldIReachByLookingAt(BlockPos pos, double x, double y, double z) {
        RayTraceResult blah = raytraceTowards(x, y, z);
        return blah != null && blah.typeOfHit == RayTraceResult.Type.BLOCK && blah.getBlockPos().equals(pos);
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {//shamelessly copied from Entity.java
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public static GoalXZ fromAngleAndDirection(double distance) {
        double theta = ((double) Minecraft.getMinecraft().player.rotationYaw) * Math.PI / 180D;
        double x = Minecraft.getMinecraft().player.posX - Math.sin(theta) * distance;
        double z = Minecraft.getMinecraft().player.posZ + Math.cos(theta) * distance;
        return new GoalXZ((int) x, (int) z);
    }

    public static boolean lookingYaw() {
        return lookingYaw;
    }
    static double SPEED = 1000;

    /**
     * Smoothly moves between random pitches and yaws every second
     *
     * @return
     */
    public static float[] getRandom() {
        long now = (long) Math.ceil(((double) System.currentTimeMillis()) / SPEED);
        now *= SPEED;
        long prev = now - (long) SPEED;
        float frac = (System.currentTimeMillis() - prev) / ((float) SPEED);//fraction between previous second and next
        Random prevR = new Random(prev);//fite me
        Random nowR = new Random(now);
        float prevFirst = prevR.nextFloat() * 10 - 5;
        float prevSecond = prevR.nextFloat() * 10 - 5;
        float nowFirst = nowR.nextFloat() * 10 - 5;
        float nowSecond = nowR.nextFloat() * 10 - 5;
        float first = prevFirst + frac * (nowFirst - prevFirst);//smooth between previous and next second
        float second = prevSecond + frac * (nowSecond - prevSecond);
        return new float[]{first, second};
    }

    public static float[] pitchAndYawToCenter(BlockPos p) {
        IBlockState b = Baritone.get(p);
        AxisAlignedBB bbox = b.getBoundingBox(Baritone.world, p);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        if (b instanceof BlockFire) {//look at bottom of fire when putting it out
            yDiff = 0;
        }
        double x = p.getX() + xDiff;
        double y = p.getY() + yDiff;
        double z = p.getZ() + zDiff;
        return pitchAndYaw(x, y, z);
    }

    public static float[] pitchAndYaw(double x, double y, double z) {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        double yDiff = (thePlayer.posY + 1.62) - y;//lol
        double yaw = Math.atan2(thePlayer.posX - x, -thePlayer.posZ + z);
        double dist = Math.sqrt((thePlayer.posX - x) * (thePlayer.posX - x) + (-thePlayer.posZ + z) * (-thePlayer.posZ + z));
        double pitch = Math.atan2(yDiff, dist);
        return new float[]{(float) (yaw * 180 / Math.PI), (float) (pitch * 180 / Math.PI)};
    }
    static ArrayList<Exception> sketchiness = new ArrayList<>();

    public static void setDesiredYaw(float y) {
        sketchiness.add(new Exception("Desired yaw already set!"));
        if (lookingYaw) {
            /*for (Exception ex : sketchiness) {
             Logger.getLogger(LookManager.class.getName()).log(Level.SEVERE, null, ex);//print out everyone who has tried to set the desired yaw this tick to show the conflict
             }*/
            sketchiness.clear();
            return;
        }
        desiredYaw = y;
        lookingYaw = true;
    }

    /**
     * Look at coordinates
     *
     * @param x
     * @param y
     * @param z
     * @param alsoDoPitch also adjust the pitch? if false, y is ignored
     * @return is the actual player yaw (and actual player pitch, if alsoDoPitch
     * is true) within ANGLE_THRESHOLD (currently 7°) of looking straight at
     * these coordinates?
     */
    public static boolean lookAtCoords(double x, double y, double z, boolean alsoDoPitch) {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        double yDiff = (thePlayer.posY + 1.62) - y;
        double yaw = Math.atan2(thePlayer.posX - x, -thePlayer.posZ + z);
        double dist = Math.sqrt((thePlayer.posX - x) * (thePlayer.posX - x) + (-thePlayer.posZ + z) * (-thePlayer.posZ + z));
        double pitch = Math.atan2(yDiff, dist);
        setDesiredYaw((float) (yaw * 180 / Math.PI));
        float yawDist = Math.abs(desiredYaw - thePlayer.rotationYaw);
        boolean withinRange = yawDist < ANGLE_THRESHOLD || yawDist > 360 - ANGLE_THRESHOLD;
        if (alsoDoPitch) {
            lookingPitch = true;
            desiredPitch = (float) (pitch * 180 / Math.PI);
            float pitchDist = Math.abs(desiredPitch - thePlayer.rotationPitch);
            withinRange = withinRange && (pitchDist < ANGLE_THRESHOLD || pitchDist > 360 - ANGLE_THRESHOLD);
        }
        return withinRange;
    }

    @Override
    public void onTickPre() {
        if (lookingYaw) {
            Minecraft.getMinecraft().player.rotationYaw = desiredNextYaw;
        }
        if (lookingPitch) {
            Minecraft.getMinecraft().player.rotationPitch = desiredNextPitch;
        }
        lookingYaw = false;
        sketchiness.clear();
        lookingPitch = false;
    }

    public static void nudgeToLevel() {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        if (!lookingPitch) {
            if (thePlayer.rotationPitch < -20) {
                thePlayer.rotationPitch++;
            } else if (thePlayer.rotationPitch > 20) {
                thePlayer.rotationPitch--;
            }
        }
    }

    @Override
    public void onTickPost() {
        if (randomLooking) {
            desiredYaw += getRandom()[0];
            desiredPitch += getRandom()[1];
        }
        if (desiredPitch > 90) {
            desiredPitch = 90;
        }
        if (desiredPitch < -90) {
            desiredPitch = -90;
        }
        if (lookingYaw) {
            previousYaw = Minecraft.getMinecraft().player.rotationYaw;
            desiredYaw += 360;
            desiredYaw %= 360;
            float yawDistance = Minecraft.getMinecraft().player.rotationYaw - desiredYaw;
            if (yawDistance > 180) {
                yawDistance -= 360;
            } else if (yawDistance < -180) {
                yawDistance += 360;
            }
            if (Math.abs(yawDistance) > MAX_YAW_CHANGE_PER_TICK) {
                yawDistance = Math.signum(yawDistance) * MAX_YAW_CHANGE_PER_TICK;
            }
            desiredNextYaw = Minecraft.getMinecraft().player.rotationYaw - yawDistance;
        }
        if (lookingPitch) {
            previousPitch = Minecraft.getMinecraft().player.rotationPitch;
            desiredPitch += 360;
            desiredPitch %= 360;
            float pitchDistance = Minecraft.getMinecraft().player.rotationPitch - desiredPitch;
            if (pitchDistance > 180) {
                pitchDistance -= 360;
            } else if (pitchDistance < -180) {
                pitchDistance += 360;
            }
            if (Math.abs(pitchDistance) > MAX_PITCH_CHANGE_PER_TICK) {
                pitchDistance = Math.signum(pitchDistance) * MAX_PITCH_CHANGE_PER_TICK;
            }
            desiredNextPitch = Minecraft.getMinecraft().player.rotationPitch - pitchDistance;
        }
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected boolean onEnabled(boolean enabled) {
        return true;
    }
}
