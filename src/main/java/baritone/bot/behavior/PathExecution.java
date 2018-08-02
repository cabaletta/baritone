package baritone.bot.behavior;

import baritone.bot.pathing.calc.IPath;
import baritone.bot.pathing.movements.Movement;
import baritone.bot.utils.Helper;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 8/1/2018 5:38 PM
 */
public class PathExecution extends Behavior implements Helper {
    private static final double MAX_DIST_FROM_PATH = 2;
    private final IPath path;

    public PathExecution(IPath path) {
        this.path = path;
    }

    public void onTick() {
        BlockPos playerFeet = playerFeet();
        // TODO copy logic from Path in resources
        if (path.isInPath(playerFeet)) {
            // TODO the old Path used to keep track of the index in the path
            // and only increment it when the movement said it was done, not when it detected that the player feet had
            // moved into the next position
            Movement movement = path.subsequentMovement(playerFeet);
            //movement.tick()
        } else {
            Tuple<Double, BlockPos> closest = path.closestPathPos(player.posX, player.posY, player.posZ);
            if (closest.getFirst() > MAX_DIST_FROM_PATH) {
                // TODO how to indicate failure? Exception?
            }
        }
    }
}
