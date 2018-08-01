
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.movement;

import baritone.ui.LookManager;
import java.util.ArrayList;
import java.util.Comparator;
import baritone.Baritone;
import static baritone.Baritone.findPathInNewThread;
import static baritone.Baritone.goal;
import static baritone.Baritone.isAir;
import static baritone.Baritone.whatEntityAreYouLookingAt;
import baritone.pathfinding.goals.GoalRunAway;
import baritone.pathfinding.goals.GoalTwoBlocks;
import baritone.util.ManagerTick;
import baritone.util.Out;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * this entire class is sketchy i don't even know
 *
 * @author leijurv
 */
public class Combat extends ManagerTick {
    public static boolean mobHunting = false;
    public static boolean mobKilling = false;
    public static boolean playerHunt = false;
    public static Entity target = null;
    public static boolean wasTargetSetByMobHunt = false;
    @Override
    public boolean onTick0() {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        World theWorld = Minecraft.getMinecraft().world;
        BlockPos playerFeet = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        boolean healthOkToHunt = Minecraft.getMinecraft().player.getHealth() >= 12 || (target != null && target instanceof EntityPlayer);
        ArrayList<Entity> killAura = new ArrayList<Entity>();
        for (Entity entity : theWorld.loadedEntityList) {
            if (entity.isEntityAlive() && !(entity instanceof EntityEnderman)) {
                if ((mobKilling && entity instanceof EntityMob) || ((playerHunt && (entity instanceof EntityPlayer) && !(entity.getName().equals(thePlayer.getName())) && !couldBeInCreative((EntityPlayer) entity)))) {
                    if (distFromMe(entity) < 5) {
                        killAura.add(entity);
                    }
                }
            }
        }
        killAura.sort(new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return new Double(distFromMe(o1)).compareTo(distFromMe(o2));
            }
        });
        if (!killAura.isEmpty()) {
            Entity entity = killAura.get(0);
            AxisAlignedBB lol = entity.getEntityBoundingBox();
            switchtosword();
            Out.log("looking");
            LookManager.lookAtCoords((lol.minX + lol.maxX) / 2, (lol.minY + lol.maxY) / 2, (lol.minZ + lol.maxZ) / 2, true);
            if (entity.equals(Baritone.whatEntityAreYouLookingAt())) {
                MovementManager.isLeftClick = true;
                if (Baritone.tickNumber % 10 < 3) {
                    MovementManager.isLeftClick = false;
                }
                tickPath = false;
                Out.log("Doing it");
            }
        }
        ArrayList<Entity> huntMobs = new ArrayList<Entity>();
        for (Entity entity : theWorld.loadedEntityList) {
            if (entity.isEntityAlive() && distFromMe(entity) < 30 && !(entity instanceof EntityEnderman)) {
                if (!playerHunt && (entity instanceof EntityMob) && entity.posY > thePlayer.posY - 6) {
                    huntMobs.add(entity);
                }
                if ((playerHunt && (entity instanceof EntityPlayer) && !(entity.getName().equals(thePlayer.getName())) && !couldBeInCreative((EntityPlayer) entity))) {
                    huntMobs.add(entity);
                }
            }
        }
        huntMobs.sort(new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return new Double(distFromMe(o1)).compareTo(distFromMe(o2));
            }
        });
        if (mobHunting && (target == null || wasTargetSetByMobHunt)) {
            if (!huntMobs.isEmpty()) {
                Entity entity = huntMobs.get(0);
                if (!entity.equals(target)) {
                    if (!(!(entity instanceof EntityPlayer) && (target instanceof EntityPlayer) && playerHunt)) {//if playerhunt is true, dont overwrite a player target with a non player target
                        Out.gui("Mobhunting=true. Killing " + entity, Out.Mode.Minimal);
                        Baritone.currentPath = null;
                        target = entity;
                        wasTargetSetByMobHunt = true;
                    }
                }
            }
        }
        if (!healthOkToHunt && target != null && wasTargetSetByMobHunt && mobHunting) {
            if (Baritone.currentPath != null) {
                if (!(Baritone.currentPath.goal instanceof GoalRunAway)) {
                    Out.gui("Health too low, cancelling hunt", Out.Mode.Minimal);
                    Baritone.currentPath = null;
                }
            }
            MovementManager.clearMovement();
            BlockPos[] away = new BlockPos[Math.min(5, huntMobs.size())];
            for (int i = 0; i < away.length; i++) {
                away[i] = new BlockPos(huntMobs.get(i).posX, huntMobs.get(i).posY, huntMobs.get(i).posZ);
            }
            if (away.length != 0) {
                Baritone.goal = new GoalRunAway(35, away);
                if (Baritone.currentPath == null || (!Baritone.isThereAnythingInProgress && Baritone.tickNumber % 4 == 0)) {
                    Out.gui("Running away", Out.Mode.Minimal);
                    Baritone.findPathInNewThread(playerFeet, false);
                }
            }
        }
        if (target != null && target.isDead) {
            Out.gui(target + " is dead", Out.Mode.Standard);
            target = null;
            Baritone.currentPath = null;
            MovementManager.clearMovement();
        }
        if (target != null && healthOkToHunt) {
            BlockPos targetPos = new BlockPos(target.posX, target.posY, target.posZ);
            Baritone.goal = new GoalTwoBlocks(targetPos);
            if (Baritone.currentPath != null) {
                double movementSince = dist(targetPos, Baritone.currentPath.end);
                if (movementSince > 4 && !Baritone.isThereAnythingInProgress) {
                    Out.gui("They moved too much, " + movementSince + " blocks. recalculating", Out.Mode.Standard);
                    Baritone.findPathInNewThread(playerFeet, true);//this will overwrite currentPath
                }
            }
            double dist = distFromMe(target);
            boolean actuallyLookingAt = target.equals(Baritone.whatEntityAreYouLookingAt());
            //Out.gui(dist + " " + actuallyLookingAt, Out.Mode.Debug);
            if (dist > 4 && Baritone.currentPath == null) {
                Baritone.findPathInNewThread(playerFeet, true);
            }
            if (dist <= 4) {
                AxisAlignedBB lol = target.getEntityBoundingBox();
                switchtosword();
                boolean direction = LookManager.lookAtCoords((lol.minX + lol.maxX) / 2, (lol.minY + lol.maxY) / 2, (lol.minZ + lol.maxZ) / 2, true);
                if (direction && !actuallyLookingAt) {
                    Baritone.findPathInNewThread(playerFeet, false);
                }
            }
            if (actuallyLookingAt) {
                MovementManager.isLeftClick = true;
                if (Baritone.tickNumber % 10 < 3) {
                    MovementManager.isLeftClick = false;
                }
                tickPath = false;
            }
        }
        return false;
    }
    public static double distFromMe(Entity a) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        double diffX = player.posX - a.posX;
        double diffY = player.posY - a.posY;
        double diffZ = player.posZ - a.posZ;
        return Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
    }
    public static boolean couldBeInCreative(EntityPlayer player) {
        if (player.capabilities.isCreativeMode || player.capabilities.allowFlying || player.capabilities.isFlying) {
            return true;
        }
        BlockPos inFeet = new BlockPos(player.posX, player.posY, player.posZ);
        BlockPos standingOn = inFeet.down();
        return isAir(standingOn) && isAir(standingOn.north()) && isAir(standingOn.south()) && isAir(standingOn.east()) && isAir(standingOn.west()) && isAir(standingOn.north().west()) && isAir(standingOn.north().east()) && isAir(standingOn.south().west()) && isAir(standingOn.south().east());
    }
    public static double dist(BlockPos a, BlockPos b) {
        int diffX = a.getX() - b.getX();
        int diffY = a.getY() - b.getY();
        int diffZ = a.getZ() - b.getZ();
        return Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
    }
    public static void switchtosword() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        ItemStack[] inv = p.inventory.mainInventory;
        float bestDamage = 0;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv[i];
            if (inv[i] == null) {
                item = new ItemStack(Item.getByNameOrId("minecraft:apple"));
            }
            if (item.getItem() instanceof ItemSword) {
                float damage = ((ItemSword) (item.getItem())).getDamageVsEntity();
                if (damage > bestDamage) {
                    p.inventory.currentItem = i;
                    bestDamage = damage;
                }
            }
            if (item.getItem() instanceof ItemAxe) {
                if (bestDamage == 0) {
                    p.inventory.currentItem = i;
                }
            }
        }
    }
    public static String killCommand(String name) {//TODO make this use Memory.playerLocationMemory
        BlockPos playerFeet = Minecraft.getMinecraft().player.getPosition0();
        if (name.length() > 2) {
            for (EntityPlayer pl : Minecraft.getMinecraft().world.playerEntities) {
                String blah = pl.getName().trim().toLowerCase();
                if (!blah.equals(Minecraft.getMinecraft().player.getName().trim().toLowerCase())) {
                    Out.gui("Considering " + blah, Out.Mode.Debug);
                    if (Combat.couldBeInCreative(pl)) {
                        Out.gui("No, creative", Out.Mode.Minimal);
                        continue;
                    }
                    if (blah.contains(name) || name.contains(blah)) {
                        Combat.target = pl;
                        Combat.wasTargetSetByMobHunt = false;
                        BlockPos pos = new BlockPos(Combat.target.posX, Combat.target.posY, Combat.target.posZ);
                        goal = new GoalTwoBlocks(pos);
                        findPathInNewThread(playerFeet, false);
                        return "Killing " + pl;
                    }
                }
            }
        }
        Entity w = whatEntityAreYouLookingAt();
        if (w != null) {
            Combat.target = w;
            BlockPos pos = new BlockPos(Combat.target.posX, Combat.target.posY, Combat.target.posZ);
            goal = new GoalTwoBlocks(pos);
            Combat.wasTargetSetByMobHunt = false;
            findPathInNewThread(playerFeet, false);
            return "Killing " + w;
        }
        return "Couldn't find " + name;
    }
    @Override
    protected void onCancel() {
        target = null;
    }
    @Override
    protected void onStart() {
    }
    @Override
    protected boolean onEnabled(boolean enabled) {
        return true;
    }
}
