package baritone.bot.pathing.movements;

import baritone.bot.pathing.action.Action;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public abstract class Movement {

    /**
     * Flat list of ordered action
     */
    protected List<Action> actions;

    /**
     * Gets source block position for Movement.
     *
     * @return Movement's starting block position
     */
    public abstract BlockPos getSrc();

    /**
     * Gets the block position the movement should finish at
     *
     * @return Movement's final block position
     */
    public abstract BlockPos getDest();

    /**
     * Calculate the movement's cost
     *
     * @return Cost
     */
    public abstract double calcCost();
}
