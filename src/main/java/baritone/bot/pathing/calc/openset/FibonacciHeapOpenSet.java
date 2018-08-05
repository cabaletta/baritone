package baritone.bot.pathing.calc.openset;

import baritone.bot.pathing.util.FibonacciHeap;
import baritone.bot.pathing.calc.PathNode;

/**
 * Wrapper adapter between FibonacciHeap and OpenSet
 *
 * @author leijurv
 */
public class FibonacciHeapOpenSet extends FibonacciHeap implements IOpenSet {

    @Override
    public void insert(PathNode node) {
        super.insert(node, node.combinedCost);
    }

    @Override
    public PathNode removeLowest() {
        return (PathNode) super.removeMin();
    }
}
