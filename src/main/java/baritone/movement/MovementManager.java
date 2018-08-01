/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.movement;

import baritone.Baritone;
import baritone.ui.LookManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class MovementManager {

    public static boolean forward = false;
    public static int leftPressTime = 0;
    public static boolean isRightClick = false;
    public static int rightPressTime = 0;
    public static boolean right = false;
    public static boolean left = false;
    public static boolean jumping = false;
    public static boolean sneak = false;
    public static boolean isLeftClick = false;
    public static boolean backward = false;

    /**
     * calls moveTowardsCoords on the center of this block
     *
     * @param p
     * @return am I moving, or am I still rotating
     */
    public static boolean moveTowardsBlock(BlockPos p) {
        return moveTowardsBlock(p, true);
    }

    public static boolean moveTowardsBlock(BlockPos p, boolean rotate) {
        Block b = Baritone.get(p).getBlock();
        double xDiff = (b.getBlockBoundsMinX() + b.getBlockBoundsMaxX()) / 2;
        double yolo = (b.getBlockBoundsMinY() + b.getBlockBoundsMaxY()) / 2;
        double zDiff = (b.getBlockBoundsMinZ() + b.getBlockBoundsMaxZ()) / 2;
        if (b instanceof BlockLadder || b instanceof BlockVine) {
            xDiff = 0.5;
            yolo = 0.5;
            zDiff = 0.5;
        }
        double x = p.getX() + xDiff;
        double y = p.getY() + yolo;
        double z = p.getZ() + zDiff;
        return moveTowardsCoords(x, y, z, rotate);
    }

    /**
     * Clears movement, but nothing else. Includes jumping and sneaking, but not
     * left clicking.
     */
    public static void clearMovement() {
        jumping = false;
        forward = false;
        left = false;
        right = false;
        backward = false;
        sneak = false;
        isRightClick = false;
        //rightPressTime = 0;
        isLeftClick = false;
        //leftPressTime = 0;
    }

    /**
     * Do not question the logic. Called by Minecraft.java
     *
     * @return
     */
    public static boolean rightIsPressed() {
        if (rightPressTime <= 0) {
            return false;
        } else {
            --rightPressTime;
            return true;
        }
    }

    /**
     * Called by our code
     */
    public static void letGoOfLeftClick() {
        leftPressTime = 0;
        isLeftClick = false;
    }

    /**
     * Do not question the logic. Called by Minecraft.java
     *
     * @return
     */
    public static boolean getLeftIsPressed() {
        return isLeftClick && leftPressTime >= -2;
    }

    /**
     * Do not question the logic. Called by Minecraft.java
     *
     * @return
     */
    public static boolean leftIsPressed() {
        if (leftPressTime <= 0) {
            return false;
        } else {
            --leftPressTime;
            return true;
        }
    }

    /**
     * Do not question the logic. Called by Minecraft.java
     *
     * @return
     */
    public static boolean getRightIsPressed() {
        return isRightClick && rightPressTime >= -2;
    }

    public static boolean moveTowardsCoords(double x, double y, double z) {
        return moveTowardsCoords(x, y, z, true);
    }

    /**
     * Move towards coordinates, not necesarily forwards. e.g. if coordinates
     * are closest to being directly behind us, go backwards. This minimizes
     * time spent waiting for rotating
     *
     * @param x
     * @param y
     * @param z
     * @param doRotate
     * @return true if we are moving, false if we are still rotating. we will
     * rotate until within ANGLE_THRESHOLD (currently 7°) of moving in correct
     * direction
     */
    public static boolean moveTowardsCoords(double x, double y, double z, boolean doRotate) {
        boolean rotate = doRotate;
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        float currentYaw = thePlayer.rotationYaw;
        float yaw = (float) (Math.atan2(thePlayer.posX - x, -thePlayer.posZ + z) * 180 / Math.PI);
        float diff = yaw - currentYaw;
        if (diff < 0) {
            diff += 360;
        }
        float distanceToForward = Math.min(Math.abs(diff - 0), Math.abs(diff - 360)) % 360;
        float distanceToForwardRight = Math.abs(diff - 45) % 360;
        float distanceToRight = Math.abs(diff - 90) % 360;
        float distanceToBackwardRight = Math.abs(diff - 135) % 360;
        float distanceToBackward = Math.abs(diff - 180) % 360;
        float distanceToBackwardLeft = Math.abs(diff - 225) % 360;
        float distanceToLeft = Math.abs(diff - 270) % 360;
        float distanceToForwardLeft = Math.abs(diff - 315) % 360;
        float tmp = Math.round(diff / 45) * 45;
        if (tmp > 359) {
            tmp -= 360;
        }
        if (rotate) {
            if (LookManager.lookingYaw()) {//if something else has set the yaw, just move anyway
                rotate = false;
            }
            LookManager.setDesiredYaw(yaw - tmp);
        }
        double t = rotate ? LookManager.ANGLE_THRESHOLD : 23;
        if (distanceToForward < t || distanceToForward > 360 - t) {
            forward = true;
            return true;
        }
        if (distanceToForwardLeft < t || distanceToForwardLeft > 360 - t) {
            forward = true;
            left = true;
            return true;
        }
        if (distanceToForwardRight < t || distanceToForwardRight > 360 - t) {
            forward = true;
            right = true;
            return true;
        }
        if (distanceToBackward < t || distanceToBackward > 360 - t) {
            backward = true;
            return true;
        }
        if (distanceToBackwardLeft < t || distanceToBackwardLeft > 360 - t) {
            backward = true;
            left = true;
            return true;
        }
        if (distanceToBackwardRight < t || distanceToBackwardRight > 360 - t) {
            backward = true;
            right = true;
            return true;
        }
        if (distanceToLeft < t || distanceToLeft > 360 - t) {
            left = true;
            return true;
        }
        if (distanceToRight < t || distanceToRight > 360 - t) {
            right = true;
            return true;
        }
        return false;
    }

    public static void rightClickMouse() {

        Minecraft.getMinecraft().rightClickMouse();
        throw new UnsupportedOperationException("Not public");
    }
}
