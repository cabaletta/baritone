package baritone.bot.pathing.goals;

import java.util.Arrays;
import java.util.Collection;
import net.minecraft.util.math.BlockPos;

/**
 * A composite of many goals, any one of which satisfies the composite.
 * For example, a GoalComposite of block goals for every oak log in loaded chunks
 * would result in it pathing to the easiest oak log to get to
 * @author avecowa
 */
public class GoalComposite implements Goal {

    public final Goal[] goals;

    public GoalComposite(Goal... goals) {
        this.goals = goals;
    }

    public GoalComposite(BlockPos... blocks) {
        this(Arrays.asList(blocks));
    }

    public GoalComposite(Collection<BlockPos> blocks) {
        this(blocks.stream().map(GoalBlock::new).toArray(Goal[]::new));
    }

    @Override
    public boolean isInGoal(BlockPos pos) {
        return Arrays.stream(this.goals).anyMatch(goal -> goal.isInGoal(pos));
    }

    @Override
    public double heuristic(BlockPos pos) {
        double min = Double.MAX_VALUE;
        for (Goal g : goals) {
            min = Math.min(min, g.heuristic(pos)); // whichever is closest
        }
        return min;
    }

    @Override
    public String toString() {
        return "GoalComposite" + Arrays.toString(goals);
    }

    public Goal[] goals() {
        return goals;
    }
}
