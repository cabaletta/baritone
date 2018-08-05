package baritone.bot.pathing.calc.openset;

import baritone.bot.pathing.calc.PathNode;
import baritone.bot.pathing.util.FibonacciHeap;

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
        PathNode pn = super.removeMin();
        pn.parent = null;
        return pn;
    }

    public void update(PathNode node) {
        super.decreaseKey(node.parent, node.combinedCost);
    }
}
