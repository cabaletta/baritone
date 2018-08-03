package baritone.bot.pathing.calc;

/**
 * Wrapper adapter between FibonacciHeap and OpenSet
 *
 * @author leijurv
 */
public class FibonacciHeapOpenSet extends FibonacciHeap implements IOpenSet {
    //isEmpty is already defined in FibonacciHeap
    @Override
    public void insert(PathNode node) {
        super.insert(node, node.estimatedCostToGoal + node.cost);
    }

    @Override
    public PathNode removeLowest() {
        return (PathNode) super.removeMin();
    }
}
