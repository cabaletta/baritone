package baritone.bot.pathing.goals;

import net.minecraft.util.math.BlockPos;

/**
 * Useful for mining (getting to diamond / iron level)
 * @author leijurv
 */
public class GoalYLevel implements Goal {

    final int level;

    public GoalYLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return pos.getY() == level;
    }

    @Override
    public double heuristic(BlockPos pos) {
        return 20 * Math.abs(pos.getY() - level);//the number 20 was chosen somewhat randomly.
        //TODO fix that
    }

    @Override
    public String toString() {
        return "Goal{y=" + level + "}";
    }
}
