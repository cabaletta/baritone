/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding;

import baritone.pathfinding.goals.Goal;
import baritone.pathfinding.actions.ActionBridge;
import baritone.pathfinding.actions.Action;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import baritone.ui.LookManager;
import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.pathfinding.actions.ActionPlaceOrBreak;
import baritone.util.Out;
import baritone.util.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class Path {
    public final BlockPos start;
    public final BlockPos end;
    public final Goal goal;
    /**
     * The blocks on the path. Guaranteed that path.get(0) equals start and
     * path.get(path.size()-1) equals end
     */
    public final ArrayList<BlockPos> path;
    final ArrayList<Action> actions;
    /**
     * note that this ISN'T the number of nodes in this path, it's actually the
     * number of nodes used to calculate this path. this is here for idk why
     *
     */
    public final int numNodes;
    Path(Node start, Node end, Goal goal, int numNodes) {
        this.numNodes = numNodes;
        this.start = start.pos;
        this.end = end.pos;
        this.goal = goal;
        this.path = new ArrayList<BlockPos>();
        this.actions = new ArrayList<Action>();
        Node current = end;
        while (!current.equals(start)) {//assemble the path
            path.add(0, current.pos);
            actions.add(0, current.previousAction);
            current = current.previous;
        }
        path.add(0, start.pos);
        /*Out.log("Final path: " + path);
         Out.log("Final actions: " + actions);
         for (int i = 0; i < path.size() - 1; i++) {//print it all out
         int oldX = path.get(i).getX();
         int oldY = path.get(i).getY();
         int oldZ = path.get(i).getZ();
         int newX = path.get(i + 1).getX();
         int newY = path.get(i + 1).getY();
         int newZ = path.get(i + 1).getZ();
         int xDiff = newX - oldX;
         int yDiff = newY - oldY;
         int zDiff = newZ - oldZ;
         Out.log(actions.get(i) + ": " + xDiff + "," + yDiff + "," + zDiff);//print it all out
         }*/
    }
    /**
     * We don't really use this any more
     */
    public void showPathInStone() {
        IBlockState[] originalStates = new IBlockState[path.size()];
        for (int i = 0; i < path.size(); i++) {
            originalStates[i] = Baritone.get(path.get(i));
            Minecraft.getMinecraft().world.setBlockState(path.get(i), Block.getBlockById(1).getDefaultState());
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(Path.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            Thread.sleep(2500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Path.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = 0; i < path.size(); i++) {
            Minecraft.getMinecraft().world.setBlockState(path.get(i), originalStates[i]);
        }
    }
    /**
     * Where are we in the path? This is an index in the actions list
     */
    int pathPosition = 0;
    public double howFarAmIFromThePath(double x, double y, double z) {
        double best = -1;
        for (BlockPos pos : path) {
            double dist = distance(x, y, z, pos);
            if (dist < best || best == -1) {
                best = dist;
            }
        }
        return best;
    }
    public void calculatePathPosition() {
        BlockPos playerFeet = Baritone.playerFeet;
        for (int i = 0; i < path.size(); i++) {
            if (playerFeet.equals(path.get(i))) {
                pathPosition = i;
            }
        }
    }
    public static double distance(double x, double y, double z, BlockPos pos) {
        double xdiff = x - (pos.getX() + 0.5D);
        double ydiff = y - (pos.getY() + 0.5D);
        double zdiff = z - (pos.getZ() + 0.5D);
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }
    /**
     * How many ticks have I been more than MAX_DISTANCE_FROM_PATH away from the
     * path
     */
    int ticksAway = 0;
    /**
     * How far away from the path can I get and still be okay
     */
    static final double MAX_DISTANCE_FROM_PATH = 2;
    /**
     * How many ticks can I be more than MAX_DISTANCE_FROM_PATH before we
     * consider it a failure
     */
    static final int MAX_TICKS_AWAY = 20 * 10;
    /**
     * How many ticks have elapsed on this action
     */
    int ticksOnCurrent = 0;
    /**
     * Did I fail, either by being too far away for too long, or by having an
     * action take too long
     */
    public boolean failed = false;
    public boolean tick() {
        if (pathPosition >= path.size()) {
            Baritone.clearPath();//stop bugging me, I'm done
            return true;
        }
        BlockPos whereShouldIBe = path.get(pathPosition);
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        BlockPos whereAmI = thePlayer.getPosition0();
        if (pathPosition == path.size() - 1) {
            Out.log("On last path position");
            Baritone.clearPath();
            return true;
        }
        if (!whereShouldIBe.equals(whereAmI)) {
            Out.log("Should be at " + whereShouldIBe + " actually am at " + whereAmI);
            if (!Blocks.air.equals(Baritone.get(thePlayer.getPosition0().down()))) {//do not skip if standing on air, because our position isn't stable to skip
                for (int i = 0; i < pathPosition - 2 && i < path.size(); i++) {//this happens for example when you lag out and get teleported back a couple blocks
                    if (whereAmI.equals(path.get(i))) {
                        Out.gui("Skipping back " + (pathPosition - i) + " steps, to " + i, Out.Mode.Debug);
                        pathPosition = Math.max(i - 1, 0);
                        return false;
                    }
                }
                for (int i = pathPosition + 2; i < path.size(); i++) {//dont check pathPosition+1
                    if (whereAmI.equals(path.get(i))) {
                        Out.gui("Skipping forward " + (i - pathPosition) + " steps, to " + i, Out.Mode.Debug);
                        pathPosition = i - 1;
                        return false;
                    }
                }
            }
        }
        double distanceFromPath = howFarAmIFromThePath(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        if (distanceFromPath > MAX_DISTANCE_FROM_PATH) {
            ticksAway++;
            Out.log("FAR AWAY FROM PATH FOR " + ticksAway + " TICKS. Current distance: " + distanceFromPath + ". Threshold: " + MAX_DISTANCE_FROM_PATH);
            if (ticksAway > MAX_TICKS_AWAY) {
                Out.gui("Too far away from path for too long, cancelling path", Out.Mode.Standard);
                Out.log("Too many ticks");
                pathPosition = path.size() + 3;
                failed = true;
                return true;
            }
        } else {
            ticksAway = 0;
        }
        Out.log(actions.get(pathPosition));
        if (pathPosition < actions.size() - 1) {//if there are two ActionBridges in a row and they are at right angles, walk diagonally. This makes it so you walk at 45 degrees along a zigzag path instead of doing inefficient zigging and zagging
            if ((actions.get(pathPosition) instanceof ActionBridge) && (actions.get(pathPosition + 1) instanceof ActionBridge)) {
                ActionBridge curr = (ActionBridge) actions.get(pathPosition);
                ActionBridge next = (ActionBridge) actions.get(pathPosition + 1);
                if (curr.dx() != next.dx() || curr.dz() != next.dz()) {//two actions are not parallel, so this is a right angle
                    if (curr.amIGood() && next.amIGood()) {//nothing in the way
                        BlockPos cornerToCut1 = new BlockPos(next.to.getX() - next.from.getX() + curr.from.getX(), next.to.getY(), next.to.getZ() - next.from.getZ() + curr.from.getZ());
                        BlockPos cornerToCut2 = cornerToCut1.up();
                        //Block corner1 = Baritone.get(cornerToCut1).getBlock();
                        //Block corner2 = Baritone.get(cornerToCut2).getBlock();
                        //Out.gui("Cutting conner " + cornerToCut1 + " " + corner1, Out.Mode.Debug);
                        if (!Action.avoidWalkingInto(cornerToCut1) && !Action.avoidWalkingInto(cornerToCut2)) {
                            double x = (next.from.getX() + next.to.getX() + 1.0D) * 0.5D;
                            double z = (next.from.getZ() + next.to.getZ() + 1.0D) * 0.5D;
                            MovementManager.clearMovement();
                            if (!MovementManager.forward && curr.oneInTen != null && curr.oneInTen) {
                                MovementManager.clearMovement();
                                MovementManager.forward = LookManager.lookAtCoords(x, 0, z, false);
                            } else {
                                MovementManager.moveTowardsCoords(x, 0, z);
                            }
                            if (MovementManager.forward && !MovementManager.backward) {
                                thePlayer.setSprinting(true);
                            }
                            return false;
                        }
                    }
                }
            }
        }
        MovementManager.clearMovement();
        Action action = actions.get(pathPosition);
        if (action.calculateCost0(new ToolSet()) >= Action.COST_INF) {
            Out.gui("Something has changed in the world and this action has become impossible. Cancelling.", Out.Mode.Standard);
            pathPosition = path.size() + 3;
            failed = true;
            return true;
        }
        if (action.tick()) {
            Out.log("Action done, next path");
            pathPosition++;
            ticksOnCurrent = 0;
        } else {
            ticksOnCurrent++;
            if (ticksOnCurrent > action.cost(null) + 100) {
                Out.gui("This action has taken too long (" + ticksOnCurrent + " ticks, expected " + action.cost(null) + "). Cancelling.", Out.Mode.Standard);
                pathPosition = path.size() + 3;
                failed = true;
                return true;
            }
        }
        return false;
    }
    public HashSet<BlockPos> toMine() {
        HashSet<BlockPos> tm = new HashSet<>();
        for (int i = pathPosition; i < actions.size(); i++) {
            if (actions.get(i) instanceof ActionPlaceOrBreak) {
                tm.addAll(((ActionPlaceOrBreak) actions.get(i)).toMine());
            }
        }
        return tm;
    }
    public HashSet<BlockPos> toPlace() {
        HashSet<BlockPos> tp = new HashSet<>();
        for (int i = pathPosition; i < actions.size(); i++) {
            if (actions.get(i) instanceof ActionPlaceOrBreak) {
                tp.addAll(((ActionPlaceOrBreak) actions.get(i)).toPlace());
            }
        }
        return tp;
    }
}
