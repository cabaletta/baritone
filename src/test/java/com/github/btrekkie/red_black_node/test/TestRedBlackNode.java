package com.github.btrekkie.red_black_node.test;

import com.github.btrekkie.red_black_node.RedBlackNode;

/**
 * A RedBlackNode for RedBlackNodeTest.
 */
class TestRedBlackNode extends RedBlackNode<TestRedBlackNode> {
    /**
     * The dummy leaf node.
     */
    public static final TestRedBlackNode LEAF = new TestRedBlackNode();

    /**
     * The value stored in this node.  "value" is unspecified if this is a leaf node.
     */
    public int value;

    /**
     * Whether this node is considered valid, as in assertNodeIsValid().
     */
    public boolean isValid = true;

    public TestRedBlackNode(int value) {
        this.value = value;
    }

    /**
     * Constructs a new dummy leaf node.
     */
    private TestRedBlackNode() {

    }

    @Override
    public void assertNodeIsValid() {
        if (!isValid) {
            throw new RuntimeException("isValid is false");
        }
    }

    @Override
    public int compareTo(TestRedBlackNode other) {
        return value - other.value;
    }
}
