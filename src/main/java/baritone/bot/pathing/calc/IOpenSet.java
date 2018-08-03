package baritone.bot.pathing.calc;

/**
 * An open set for A* or similar graph search algorithm
 *
 * @author leijurv
 */
public interface IOpenSet {
    boolean isEmpty();

    void insert(PathNode node);

    PathNode removeLowest();
}
