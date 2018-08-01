/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone;

import baritone.movement.MovementManager;
import baritone.pathfinding.Path;
import baritone.pathfinding.PathFinder;
import baritone.pathfinding.actions.Action;
import baritone.pathfinding.actions.ActionPlaceOrBreak;
import baritone.pathfinding.goals.Goal;
import baritone.pathfinding.goals.GoalComposite;
import baritone.schematic.SchematicBuilder;
import baritone.ui.LookManager;
import baritone.util.Autorun;
import baritone.util.Manager;
import baritone.util.ManagerTick;
import baritone.util.Memory;
import baritone.util.Out;
import baritone.util.ToolSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 *
 * @author leijurv
 */
public class Baritone {

    public static BlockPos playerFeet = null;
    public static World world = null;
    public static boolean allowDiagonal = true;
    public static boolean farf5 = false;
    public static boolean slowPath = false;
    public static boolean pause = false;
    public static boolean overrideF3 = false;
    public static boolean sketchytracer = false;
    public static boolean allowVerticalMotion = true;
    public static boolean actuallyPutMessagesInChat = false;
    public static boolean isThereAnythingInProgress = false;
    public static boolean fullBright = true;
    public static boolean plsCancel = false;
    public static int tickNumber = 0;
    public static boolean ticktimer = false;
    public static boolean allowBreakOrPlace = true;
    public static boolean hasThrowaway = true;
    public static Path currentPath = null;
    public static Path nextPath = null;
    public static boolean calculatingNext = false;
    public static Goal goal = null;
    static int numTicksInInventoryOrCrafting = 0;
    public static BlockPos death;
    public static long lastDeath = 0;
    public static SchematicBuilder currentBuilder = null;

    public static IBlockState get(BlockPos pos) { // wrappers for future 1.13 compat
        return world.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return get(pos).getBlock();
    }

    public static boolean isBlockNormalCube(BlockPos pos) {
        IBlockState state = get(pos);
        return state.getBlock().isBlockNormalCube(state);
    }

    /**
     * Called by minecraft.java
     */
    public static void onTick() {
        try {
            long start = System.currentTimeMillis();
            onTick1();
            long end = System.currentTimeMillis();
            long time = end - start;
            if (ticktimer && time > 3) {
                Out.gui("Tick took " + time + "ms", Out.Mode.Debug);
                Out.log("Tick took " + time + "ms");
            }
        } catch (Exception ex) {
            Out.log("Exception");
            Logger.getLogger(Baritone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void onTick1() {
        if (pause) {
            Manager.tick(LookManager.class, true);
            MovementManager.clearMovement();
            return;
        }
        world = Minecraft.getMinecraft().world;
        if (world == null || Minecraft.getMinecraft().player == null) {
            Baritone.cancelPath();
            Baritone.plsCancel = true;
            return;
        }
        MovementManager.leftPressTime = 0;
        MovementManager.rightPressTime = 0;
        if (MovementManager.isLeftClick) {
            MovementManager.leftPressTime = 5;
        }
        if (MovementManager.isRightClick) {
            MovementManager.rightPressTime = 5;
        }
        Manager.tick(LookManager.class, true);

        /*MovementManager.isLeftClick = false;
         MovementManager.isRightClick = false;
         MovementManager.jumping = false;
         MovementManager.sneak = false;*/
        MovementManager.clearMovement();
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        World theWorld = Minecraft.getMinecraft().world;
        playerFeet = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        if (thePlayer.isDead && System.currentTimeMillis() > lastDeath + 10000) {
            death = playerFeet;
            lastDeath = System.currentTimeMillis();
            Out.gui("Saved death position (" + death + "). do /death to set goal.", Out.Mode.Minimal);
            //impact handles this
            //thePlayer.respawnPlayer();
            //Minecraft.getMinecraft().displayGuiScreen(null);
        }
        tickNumber++;
        hasThrowaway = ActionPlaceOrBreak.hasthrowaway();
        ManagerTick.tickPath = true;
        Manager.tick(LookManager.class);
        BlockPos ts = whatAreYouLookingAt();
        if (ts != null) {
            Memory.scanBlock(ts);
        }
        if (currentBuilder != null) {
            currentBuilder.tick();
        }
        if (currentPath != null && ManagerTick.tickPath) {
            if (currentPath.tick()) {
                Goal currentPathGoal = currentPath == null ? null : currentPath.goal;
                if (currentPath != null && currentPath.failed) {
                    clearPath();
                    Out.gui("Recalculating because path failed", Out.Mode.Standard);
                    nextPath = null;
                    if (isAir(playerFeet.down())) {//sometimes we are jumping and we make a path that starts in the air and then jumps up, which is impossible
                        Out.gui("DOING THE JANKY THING, WARNING", Out.Mode.Debug);
                        findPathInNewThread(playerFeet.down(), true);
                    } else {
                        findPathInNewThread(playerFeet, true);
                    }
                    return;
                } else {
                    clearPath();
                }
                currentPath = null;
                if (goal.isInGoal(playerFeet)) {
                    Out.gui("All done. At goal", Out.Mode.Standard);
                    nextPath = null;
                } else {
                    Out.gui("Done with segment", Out.Mode.Debug);
                    if (nextPath != null || calculatingNext) {
                        if (calculatingNext) {
                            calculatingNext = false;
                            Out.gui("Patiently waiting to finish", Out.Mode.Debug);
                        } else {
                            currentPath = nextPath;
                            nextPath = null;
                            if (!currentPath.start.equals(playerFeet)) {
                                //Out.gui("The next path starts at " + currentPath.start + " but I'm at " + playerFeet + ". not doing it", true);
                                currentPath = null;
                                findPathInNewThread(playerFeet, true);
                            } else {
                                Out.gui("Going onto next", Out.Mode.Debug);
                                if (!currentPath.goal.isInGoal(currentPath.end)) {
                                    planAhead();
                                }
                            }
                        }
                    } else {
                        Out.gui("Hmm. I'm not actually at the goal. Recalculating.", Out.Mode.Debug);
                        findPathInNewThread(playerFeet, (currentPathGoal != null && goal != null) ? !(currentPathGoal instanceof GoalComposite) && currentPathGoal.toString().equals(goal.toString()) : true);
                    }
                }
            } else {
                if (Action.isWater(theWorld.getBlockState(playerFeet).getBlock())) {
                    //if (Action.isWater(theWorld.getBlockState(playerFeet.down()).getBlock()) || !Action.canWalkOn(playerFeet.down()) || Action.isWater(theWorld.getBlockState(playerFeet.up()).getBlock())) {
                    //if water is deeper than one block, or we can't walk on what's below the water, or our head is in water, jump
                    Out.log("Jumping because in water");
                    MovementManager.jumping = true;
                    //}
                }
                if (nextPath != null) {
                    for (int i = 1; i < 20 && i < nextPath.path.size(); i++) {
                        if (playerFeet.equals(nextPath.path.get(i))) {
                            Out.gui("Jumping to position " + i + " in nextpath", Out.Mode.Debug);
                            currentPath = nextPath;
                            currentPath.calculatePathPosition();
                            nextPath = null;
                            if (!currentPath.goal.isInGoal(currentPath.end)) {
                                planAhead();
                            }
                            break;
                        }
                    }
                }
                LookManager.nudgeToLevel();
            }
        }
        /*if (Minecraft.getMinecraft().currentScreen != null && (Minecraft.getMinecraft().currentScreen instanceof GuiCrafting || Minecraft.getMinecraft().currentScreen instanceof GuiInventory || Minecraft.getMinecraft().currentScreen instanceof GuiFurnace)) {
            MovementManager.isLeftClick = false;
            MovementManager.leftPressTime = -5;
            numTicksInInventoryOrCrafting++;
            if (numTicksInInventoryOrCrafting > 20 * 20) {
                Minecraft.getMinecraft().player.closeScreen();
                numTicksInInventoryOrCrafting = 0;
            }
        } else {
            numTicksInInventoryOrCrafting = 0;
        }*/ // impact does this
        if (isThereAnythingInProgress && Action.isWater(theWorld.getBlockState(playerFeet).getBlock())) {
            if (Action.isWater(theWorld.getBlockState(playerFeet.down()).getBlock()) || !Action.canWalkOn(playerFeet.down()) || Action.isWater(theWorld.getBlockState(playerFeet.up()).getBlock())) {
                //if water is deeper than one block, or we can't walk on what's below the water, or our head is in water, jump
                Out.log("Jumping because in water and pathfinding");
                MovementManager.jumping = true;
            }
        }
        Manager.tick(LookManager.class, false);
        MovementManager.tick();
    }

    public static boolean isPathFinding() {
        return isThereAnythingInProgress;
    }

    /**
     * Clears movement, clears the current path, and lets go of left click. It
     * purposefully does NOT clear nextPath.
     */
    public static void clearPath() {
        currentPath = null;
        MovementManager.letGoOfLeftClick();
        MovementManager.clearMovement();
    }

    public static String info(BlockPos bp) {
        IBlockState state = get(bp);
        Block block = state.getBlock();
        return bp + " " + block + " can walk on: " + Action.canWalkOn(bp) + " can walk through: " + Action.canWalkThrough(bp) + " is full block: " + block.isFullBlock(state) + " is full cube: " + block.isFullCube(state) + " is liquid: " + Action.isLiquid(block) + " is flow: " + Action.isFlowing(bp, state);
    }

    /**
     * Cancel the path
     *
     */
    public static void cancelPath() {
        nextPath = null;
        currentBuilder = null;
        clearPath();
    }

    public static boolean isAir(BlockPos pos) {
        return Baritone.get(pos).getBlock().equals(Blocks.AIR);
    }

    public static void findPathInNewThread(final boolean talkAboutIt) {
        findPathInNewThread(playerFeet, talkAboutIt);
    }

    /**
     * In a new thread, pathfind to target blockpos
     *
     * @param start
     * @param talkAboutIt
     */
    public static void findPathInNewThread(final BlockPos start, final boolean talkAboutIt) {
        if (isThereAnythingInProgress) {
            return;
        }
        isThereAnythingInProgress = true;
        new Thread() {
            @Override
            public void run() {
                if (talkAboutIt) {
                    Out.gui("Starting to search for path from " + start + " to " + goal, Out.Mode.Debug);
                }
                try {
                    currentPath = findPath(start);
                } catch (Exception e) {
                }
                isThereAnythingInProgress = false;
                if (!currentPath.goal.isInGoal(currentPath.end)) {
                    if (talkAboutIt) {
                        Out.gui("I couldn't get all the way to " + goal + ", but I'm going to get as close as I can. " + currentPath.numNodes + " nodes considered", Out.Mode.Standard);
                    }
                    planAhead();
                } else if (talkAboutIt) {
                    Out.gui("Finished finding a path from " + start + " to " + goal + ". " + currentPath.numNodes + " nodes considered", Out.Mode.Debug);
                }
            }
        }.start();
    }

    /**
     * In a new thread, pathfind from currentPath.end to goal. Store resulting
     * path in nextPath (or in currentPath if calculatingNext was set to false
     * in the meantime).
     */
    public static void planAhead() {
        if (isThereAnythingInProgress) {
            return;
        }
        isThereAnythingInProgress = true;
        new Thread() {
            @Override
            public void run() {
                Out.gui("Planning ahead", Out.Mode.Debug);
                calculatingNext = true;
                Path path = findPath(currentPath.end);
                isThereAnythingInProgress = false;
                Out.gui("Done planning ahead " + calculatingNext, Out.Mode.Debug);
                if (calculatingNext) {
                    nextPath = path;
                } else {
                    currentPath = path;
                    if (!plsCancel) {
                        planAhead();
                    }
                }
                calculatingNext = false;
                Out.gui(path.numNodes + " nodes considered, calculated " + path.start + " to " + path.end, Out.Mode.Debug);
            }
        }.start();
    }

    /**
     * Actually do the pathfinding
     *
     * @param start
     * @return
     */
    private static Path findPath(BlockPos start) {
        if (goal == null) {
            Out.gui("babe, please. there is no goal", Out.Mode.Minimal);
            return null;
        }
        try {
            PathFinder pf = new PathFinder(start, goal);
            Path path = pf.calculatePath();
            return path;
        } catch (Exception e) {
            Logger.getLogger(Baritone.class.getName()).log(Level.SEVERE, null, e);
            isThereAnythingInProgress = false;
            return null;
        }
    }

    /**
     * What block is the player looking at
     *
     * @return the position of it
     */
    public static BlockPos whatAreYouLookingAt() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            return mc.objectMouseOver.getBlockPos();
        }
        return null;
    }

    public static void switchToBestTool() {
        BlockPos pos = whatAreYouLookingAt();
        if (pos == null) {
            return;
        }
        IBlockState state = Baritone.get(pos);
        if (state.getBlock().equals(Blocks.AIR)) {
            return;
        }
        switchtotool(state);
    }

    public static void switchtotool(IBlockState b) {
        Baritone.switchtotool(b, new ToolSet());
    }

    public static void switchtotool(IBlockState b, ToolSet ts) {
        Minecraft.getMinecraft().player.inventory.currentItem = ts.getBestSlot(b);
    }

    public static Entity whatEntityAreYouLookingAt() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            return mc.objectMouseOver.entityHit;
        }
        return null;
    }

    public static List<String> getDebugGui() {
        if (!overrideF3) {
            return null;
        }
        List<String> list = new ArrayList<String>();
        list.add("§5[§dBaritone§5]§f");
        Class<LookManager> c = LookManager.class;
        list.add("§r[§" + (Manager.enabled(c) ? "a" : "c") + c.getSimpleName() + "§r]");
        list.add("");
        list.add("Current goal: " + goal);
        list.add("");
        if (currentPath != null) {
            list.add("Current path start: " + currentPath.start);
            list.add("Current path end: " + currentPath.end);
            list.add("Current path ends in current goal: " + (goal == null ? null : goal.isInGoal(currentPath.end)));
            if (!currentPath.goal.equals(goal)) {
                list.add("Current path goal: " + currentPath.goal);
            }
        }
        if (nextPath != null) {
            list.add("");
            list.add("Next path start: " + nextPath.start);
            list.add("Next path end: " + nextPath.end);
            list.add("Next path ends in current goal: " + (goal == null ? null : goal.isInGoal(nextPath.end)));
        }
        return list;
    }

    public boolean isNull() throws NullPointerException {
        NullPointerException up = new NullPointerException("You are disgusting");
        throw up;
        //To use this surround the call to this message with a try-catch then compare the length of the NullPointerException.getMessage()
    }
}
