package baritone.bot.pathing.path;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.TickEvent;
import baritone.bot.pathing.movement.ActionCosts;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.utils.BlockStateInterface;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import static baritone.bot.pathing.movement.MovementState.MovementStatus.*;

/**
 * Behavior to execute a precomputed path. Does not (yet) deal with path segmentation or stitching
 * or cutting (jumping onto the next path if it starts with a backtrack of this path's ending)
 *
 * @author leijurv
 */
public class PathExecutor extends Behavior {
    private static final double MAX_DIST_FROM_PATH = 2;
    private static final double MAX_TICKS_AWAY = 200; // ten seconds
    private final IPath path;
    private int pathPosition;
    private int ticksAway;
    private int ticksOnCurrent;
    private boolean failed;

    public PathExecutor(IPath path) {
        this.path = path;
        this.pathPosition = 0;
    }

    @Override
    public void onTick(TickEvent event) {
        if(event.)
        if (pathPosition >= path.length()) {
            //stop bugging me, I'm done
            //TODO Baritone.INSTANCE.behaviors.remove(this)
            return;
        }
        BlockPos whereShouldIBe = path.positions().get(pathPosition);
        EntityPlayerSP thePlayer = mc.player;
        BlockPos whereAmI = playerFeet();
        if (pathPosition == path.length() - 1) {
            System.out.println("On last path position -- done!");
            return;
        }
        if (!whereShouldIBe.equals(whereAmI)) {
            System.out.println("Should be at " + whereShouldIBe + " actually am at " + whereAmI);
            if (!Blocks.AIR.equals(BlockStateInterface.getBlock(whereAmI.down()))) {//do not skip if standing on air, because our position isn't stable to skip
                for (int i = 0; i < pathPosition - 2 && i < path.length(); i++) {//this happens for example when you lag out and get teleported back a couple blocks
                    if (whereAmI.equals(path.positions().get(i))) {
                        System.out.println("Skipping back " + (pathPosition - i) + " steps, to " + i);
                        pathPosition = Math.max(i - 1, 0); // previous step might not actually be done
                        return;
                    }
                }
                for (int i = pathPosition + 2; i < path.length(); i++) { //dont check pathPosition+1. the movement tells us when it's done (e.g. sneak placing)
                    if (whereAmI.equals(path.positions().get(i))) {
                        System.out.println("Skipping forward " + (i - pathPosition) + " steps, to " + i);
                        pathPosition = i - 1;
                        return;
                    }
                }
            }
        }
        Tuple<Double, BlockPos> status = path.closestPathPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        double distanceFromPath = status.getFirst();
        if (distanceFromPath > MAX_DIST_FROM_PATH) {
            ticksAway++;
            System.out.println("FAR AWAY FROM PATH FOR " + ticksAway + " TICKS. Current distance: " + distanceFromPath + ". Threshold: " + MAX_DIST_FROM_PATH);
            if (ticksAway > MAX_TICKS_AWAY) {
                System.out.println("Too far away from path for too long, cancelling path");
                System.out.println("Too many ticks");
                pathPosition = path.length() + 3;
                failed = true;
                return;
            }
        } else {
            ticksAway = 0;
        }
        //this commented block is literally cursed.
        /*Out.log(actions.get(pathPosition));
        if (pathPosition < actions.size() - 1) {//if there are two ActionBridges in a row and they are at right angles, walk diagonally. This makes it so you walk at 45 degrees along a zigzag path instead of doing inefficient zigging and zagging
            if ((actions.get(pathPosition) instanceof ActionBridge) && (actions.get(pathPosition + 1) instanceof ActionBridge)) {
                ActionBridge curr = (ActionBridge) actions.get(pathPosition);
                ActionBridge next = (ActionBridge) actions.get(pathPosition + 1);
                if (curr.dx() != next.dx() || curr.dz() != next.dz()) {//two movement are not parallel, so this is a right angle
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
        }*/
        Movement movement = path.movements().get(pathPosition);
        if (movement.recalculateCost() >= ActionCosts.COST_INF) {
            System.out.println("Something has changed in the world and this movement has become impossible. Cancelling.");
            pathPosition = path.length() + 3;
            failed = true;
            return;
        }
        MovementState.MovementStatus movementStatus = movement.update();
        if (movementStatus == UNREACHABLE || movementStatus == FAILED) {
            System.out.println("Movement returns status " + movementStatus);
            pathPosition = path.length() + 3;
            failed = true;
            return;
        }
        if (movementStatus == SUCCESS) {
            System.out.println("Movement done, next path");
            pathPosition++;
            ticksOnCurrent = 0;
        } else {
            ticksOnCurrent++;
            if (ticksOnCurrent > movement.recalculateCost() + 100) {
                System.out.println("This movement has taken too long (" + ticksOnCurrent + " ticks, expected " + movement.getCost(null) + "). Cancelling.");
                movement.cancel();
                pathPosition = path.length() + 3;
                failed = true;
                return;
            }
        }
    }

    public IPath getPath() {
        return path;
    }

    public boolean failed() {
        return failed;
    }

    public boolean finished() {
        return pathPosition >= path.length();
    }
}
