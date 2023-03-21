/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/RedBlackNode/
 */

package com.github.btrekkie.interval_tree;

import baritone.builder.utils.com.github.btrekkie.red_black_node.RedBlackNode;

/**
 * A node in an IntervalTree.  See the comments for the implementation of IntervalTree.  Its compareTo method orders
 * nodes as suggested in the comments for the implementation of IntervalTree.
 */
class IntervalTreeNode extends RedBlackNode<IntervalTreeNode> {
    /**
     * The dummy leaf node.
     */
    public static final IntervalTreeNode LEAF = new IntervalTreeNode();

    /**
     * The interval stored in this node.
     */
    public IntervalTreeInterval interval;

    /**
     * The maximum ending value of an interval in the subtree rooted at this node.
     */
    public double maxEnd;

    public IntervalTreeNode(IntervalTreeInterval interval) {
        this.interval = interval;
        maxEnd = interval.end;
    }

    /**
     * Constructs a new dummy leaf node.
     */
    private IntervalTreeNode() {
        interval = null;
        maxEnd = Double.NEGATIVE_INFINITY;
    }

    @Override
    public boolean augment() {
        double newMaxEnd = Math.max(interval.end, Math.max(left.maxEnd, right.maxEnd));
        if (newMaxEnd == maxEnd) {
            return false;
        } else {
            maxEnd = newMaxEnd;
            return true;
        }
    }

    @Override
    public void assertNodeIsValid() {
        double expectedMaxEnd;
        if (isLeaf()) {
            expectedMaxEnd = Double.NEGATIVE_INFINITY;
        } else {
            expectedMaxEnd = Math.max(interval.end, Math.max(left.maxEnd, right.maxEnd));
        }
        if (maxEnd != expectedMaxEnd) {
            throw new RuntimeException("The node's maxEnd does not match that of the children");
        }
    }

    @Override
    public int compareTo(IntervalTreeNode other) {
        if (interval.start != interval.end) {
            return Double.compare(interval.start, other.interval.start);
        } else {
            return Double.compare(interval.end, other.interval.end);
        }
    }

    @Override
    public void assertSubtreeIsValid() {
        super.assertSubtreeIsValid();
        assertOrderIsValid(null);
    }
}
