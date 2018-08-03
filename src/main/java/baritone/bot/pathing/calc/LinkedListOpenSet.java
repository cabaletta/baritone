package baritone.bot.pathing.calc;

/**
 *
 */
public class LinkedListOpenSet implements IOpenSet {
    private PathNode first = null;

    public boolean isEmpty() {
        return first == null;
    }

    public void insert(PathNode node) {
        node.nextOpen = first;
        first = node;
    }

    public PathNode removeLowest() {
        if (first == null) {
            return null;
        }
        PathNode current = first.nextOpen;
        if (current == null) {
            PathNode n = first;
            first = null;
            return n;
        }
        PathNode previous = first;
        double bestValue = first.estimatedCostToGoal + first.cost;
        PathNode bestNode = first;
        PathNode beforeBest = null;
        while (current != null) {
            double comp = current.estimatedCostToGoal + current.cost;
            if (comp < bestValue) {
                bestValue = comp;
                bestNode = current;
                beforeBest = previous;
            }
            previous = current;
            current = current.nextOpen;
        }
        if (beforeBest == null) {
            first = first.nextOpen;
            return bestNode;
        }
        beforeBest.nextOpen = bestNode.nextOpen;
        return bestNode;
    }
}
