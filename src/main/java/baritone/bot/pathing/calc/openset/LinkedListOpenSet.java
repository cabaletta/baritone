package baritone.bot.pathing.calc.openset;

import baritone.bot.pathing.calc.PathNode;

/**
 * A linked list implementation of an open set. This is the original implementation from MineBot.
 * It has incredibly fast insert performance, at the cost of O(n) removeLowest.
 *
 * @author leijurv
 */
public class LinkedListOpenSet implements IOpenSet {
    private Node first = null;

    @Override
    public boolean isEmpty() {
        return first == null;
    }

    @Override
    public void insert(PathNode pathNode) {
        Node node = new Node();
        node.val = pathNode;
        node.nextOpen = first;
        first = node;
    }

    @Override
    public void update(PathNode node) {

    }

    @Override
    public PathNode removeLowest() {
        if (first == null) {
            return null;
        }
        Node current = first.nextOpen;
        if (current == null) {
            Node n = first;
            first = null;
            return n.val;
        }
        Node previous = first;
        double bestValue = first.val.combinedCost;
        Node bestNode = first;
        Node beforeBest = null;
        while (current != null) {
            double comp = current.val.combinedCost;
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
            bestNode.nextOpen = null;
            return bestNode.val;
        }
        beforeBest.nextOpen = bestNode.nextOpen;
        bestNode.nextOpen = null;
        return bestNode.val;
    }

    public static class Node { //wrapper with next
        Node nextOpen;
        PathNode val;
    }
}
