/*
 * This file was originally written by btrekkie under the MIT license, which is compatible with the LGPL license for this usage within Baritone
 * https://github.com/btrekkie/RedBlackNode/
 */

package com.github.btrekkie.interval_tree;

/**
 * An interval tree data structure, which supports adding or removing an interval and finding an arbitrary interval that
 * contains a specified value.
 */
/* The interval tree is ordered in ascending order of the start an interval, with ties broken by the end of the
 * interval.  Each node is augmented with the maximum ending value of an interval in the subtree rooted at the node.
 */
public class IntervalTree {
    /**
     * The root node of the tree.
     */
    private IntervalTreeNode root = IntervalTreeNode.LEAF;

    /**
     * Adds the specified interval to this.
     */
    public void addInterval(IntervalTreeInterval interval) {
        root = root.insert(new IntervalTreeNode(interval), true, null);
    }

    /**
     * Removes the specified interval from this, if it is present.
     *
     * @param interval The interval.
     * @return Whether the interval was present.
     */
    public boolean removeInterval(IntervalTreeInterval interval) {
        IntervalTreeNode node = root;
        while (!node.isLeaf()) {
            if (interval.start < node.interval.start) {
                node = node.left;
            } else if (interval.start > node.interval.start) {
                node = node.right;
            } else if (interval.end < node.interval.end) {
                node = node.left;
            } else if (interval.end > node.interval.end) {
                node = node.right;
            } else {
                root = node.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an aribtrary IntervalTreeInterval in this that contains the specified value.  Returns null if there is no
     * such interval.
     */
    public IntervalTreeInterval findInterval(double value) {
        IntervalTreeNode node = root;
        while (!node.isLeaf()) {
            if (value >= node.interval.start && value <= node.interval.end) {
                return node.interval;
            } else if (value <= node.left.maxEnd) {
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return null;
    }
}
