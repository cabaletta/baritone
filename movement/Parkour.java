/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.movement;

import baritone.pathfinding.actions.Action;
import baritone.util.Manager;
import baritone.util.Out;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author leijurv
 */
public class Parkour extends Manager {
    public static boolean preemptivejump() {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        BlockPos playerFeet = thePlayer.getPosition0();
        EnumFacing dir = thePlayer.getHorizontalFacing();
        for (int height = 0; height < 3; height++) {
            BlockPos bp = playerFeet.offset(dir, 1).up(height);
            if (!Minecraft.getMinecraft().world.getBlockState(bp).getBlock().equals(Block.getBlockById(0))) {
                return Action.canWalkOn(playerFeet.offset(dir, 1));
            }
        }
        for (int height = 0; height < 3; height++) {
            BlockPos bp = playerFeet.offset(dir, 2).up(height);
            if (!Minecraft.getMinecraft().world.getBlockState(bp).getBlock().equals(Block.getBlockById(0))) {
                return Action.canWalkOn(playerFeet.offset(dir, 2));
            }
        }
        return Action.canWalkOn(playerFeet.offset(dir, 3));
    }
    public static void parkour() {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        BlockPos playerFeet = thePlayer.getPosition0();
        BlockPos down = playerFeet.down();
        BlockPos prev = down.offset(thePlayer.getHorizontalFacing().getOpposite());
        boolean onAir = Minecraft.getMinecraft().world.getBlockState(down).getBlock().equals(Block.getBlockById(0));
        boolean jumpAnyway = preemptivejump();
        if (onAir || jumpAnyway) {
            if ((thePlayer.isSprinting() && !Minecraft.getMinecraft().world.getBlockState(prev).getBlock().equals(Block.getBlockById(0))) || !Minecraft.getMinecraft().world.getBlockState(playerFeet.offset(thePlayer.getHorizontalFacing())).getBlock().equals(Block.getBlockById(0))) {
                double distX = Math.abs(thePlayer.posX - (prev.getX() + 0.5));
                distX *= Math.abs(prev.getX() - down.getX());
                double distZ = Math.abs(thePlayer.posZ - (prev.getZ() + 0.5));
                distZ *= Math.abs(prev.getZ() - down.getZ());
                double dist = distX + distZ;
                thePlayer.rotationYaw = Math.round(thePlayer.rotationYaw / 90) * 90;
                if (dist > 0.7) {
                    MovementManager.jumping = true;
                    Out.gui("Parkour jumping!!!", Out.Mode.Standard);
                }
            }
        }
    }
    @Override
    protected void onTick() {
        parkour();
    }
    @Override
    protected void onCancel() {
    }
    @Override
    protected void onStart() {
    }
}
