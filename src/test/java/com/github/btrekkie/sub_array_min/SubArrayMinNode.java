package com.github.btrekkie.sub_array_min;

import baritone.builder.utils.com.github.btrekkie.red_black_node.RedBlackNode;

/**
 * A node in a SubArrayMin object.  See the comments for the implementation of that class.
 */
class SubArrayMinNode extends RedBlackNode<SubArrayMinNode> {
    /**
     * The dummy leaf node.
     */
    public static final SubArrayMinNode LEAF = new SubArrayMinNode();

    /**
     * The element stored in the node.  The value is unspecified if this is a leaf node.
     */
    public final int value;

    /**
     * The number of elements in the subtree rooted at this node.
     */
    public int size;

    /**
     * The minimum element in the subtree rooted at this node.  This is Integer.MAX_VALUE if this is a leaf node.
     */
    public int min;

    public SubArrayMinNode(int value) {
        this.value = value;
    }

    private SubArrayMinNode() {
        value = 0;
        min = Integer.MAX_VALUE;
    }

    @Override
    public boolean augment() {
        int newSize = left.size + right.size + 1;
        int newMin = Math.min(value, Math.min(left.min, right.min));
        if (newSize == size && newMin == min) {
            return false;
        } else {
            size = newSize;
            min = newMin;
            return true;
        }
    }

    @Override
    public void assertNodeIsValid() {
        int expectedSize;
        int expectedMin;
        if (isLeaf()) {
            expectedSize = 0;
            expectedMin = Integer.MAX_VALUE;
        } else {
            expectedSize = left.size + right.size + 1;
            expectedMin = Math.min(value, Math.min(left.min, right.min));
        }
        if (size != expectedSize || min != expectedMin) {
            throw new RuntimeException("The node's size or minimum value does not match that of the children");
        }
    }
}
