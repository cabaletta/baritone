package com.github.btrekkie.red_black_node.test;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests RedBlackNode.  Most of the testing for RedBlackNode takes place in TreeListTest, IntervalTreeTest,
 * SubArrayMinTest, and ArbitraryOrderCollectionTest, which test realistic use cases of RedBlackNode.  TreeListTest
 * tests most of the RedBlackNode methods, while IntervalTreeTest tests the "insert" method, SubArrayMinTest tests
 * "lca", ArbitraryOrderCollectionTest tests compareTo, and RedBlackNodeTest tests assertSubtreeIsValid() and
 * assertOrderIsValid.
 */
public class RedBlackNodeTest {
    /**
     * Returns whether the subtree rooted at the specified node is valid, as in TestRedBlackNode.assertSubtreeIsValid().
     */
    private boolean isSubtreeValid(TestRedBlackNode node) {
        try {
            node.assertSubtreeIsValid();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Returns whether the nodes in the subtree rooted at the specified node are ordered correctly, as in
     * TestRedBlackNode.assertOrderIsValid.
     *
     * @param comparator A comparator indicating how the nodes should be ordered.  If this is null, we use the nodes'
     *                   natural ordering, as in TestRedBlackNode.compare.
     */
    private boolean isOrderValid(TestRedBlackNode node, Comparator<TestRedBlackNode> comparator) {
        try {
            node.assertOrderIsValid(null);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Tests RedBlackNode.assertSubtreeIsValid() and RedBlackNode.assertOrderIsValid.
     */
    @Test
    public void testAssertIsValid() {
        // Create a perfectly balanced tree of height 3
        TestRedBlackNode node0 = new TestRedBlackNode(0);
        TestRedBlackNode node1 = new TestRedBlackNode(1);
        TestRedBlackNode node2 = new TestRedBlackNode(2);
        TestRedBlackNode node3 = new TestRedBlackNode(3);
        TestRedBlackNode node4 = new TestRedBlackNode(4);
        TestRedBlackNode node5 = new TestRedBlackNode(5);
        TestRedBlackNode node6 = new TestRedBlackNode(6);
        node0.parent = node1;
        node0.left = TestRedBlackNode.LEAF;
        node0.right = TestRedBlackNode.LEAF;
        node1.parent = node3;
        node1.left = node0;
        node1.right = node2;
        node1.isRed = true;
        node2.parent = node1;
        node2.left = TestRedBlackNode.LEAF;
        node2.right = TestRedBlackNode.LEAF;
        node3.left = node1;
        node3.right = node5;
        node4.parent = node5;
        node4.left = TestRedBlackNode.LEAF;
        node4.right = TestRedBlackNode.LEAF;
        node5.parent = node3;
        node5.left = node4;
        node5.right = node6;
        node5.isRed = true;
        node6.parent = node5;
        node6.left = TestRedBlackNode.LEAF;
        node6.right = TestRedBlackNode.LEAF;

        node3.left = node3;
        node3.right = node3;
        node3.parent = node3;
        assertFalse(isSubtreeValid(node3));
        node3.left = node1;
        node3.right = node5;
        node3.parent = null;

        node0.parent = node3;
        assertFalse(isSubtreeValid(node3));
        node0.parent = node1;

        node1.right = node0;
        assertFalse(isSubtreeValid(node3));
        node1.right = node2;

        node5.isRed = false;
        assertFalse(isSubtreeValid(node3));
        assertTrue(isSubtreeValid(node5));
        node5.isRed = true;

        node3.isRed = true;
        assertFalse(isSubtreeValid(node3));
        assertTrue(isSubtreeValid(node5));
        node3.isRed = false;

        node0.isRed = true;
        node2.isRed = true;
        node4.isRed = true;
        node6.isRed = true;
        assertFalse(isSubtreeValid(node3));
        node0.isRed = false;
        node2.isRed = false;
        node4.isRed = false;
        node6.isRed = false;

        TestRedBlackNode.LEAF.isRed = true;
        assertFalse(isSubtreeValid(node3));
        TestRedBlackNode.LEAF.isRed = false;

        TestRedBlackNode.LEAF.isValid = false;
        assertFalse(isSubtreeValid(node3));
        assertFalse(isSubtreeValid(TestRedBlackNode.LEAF));
        TestRedBlackNode.LEAF.isValid = true;

        node1.isValid = false;
        assertFalse(isSubtreeValid(node3));
        node1.isValid = true;

        node3.value = 2;
        node2.value = 3;
        assertFalse(isOrderValid(node3, null));
        assertFalse(
                isOrderValid(node3, new Comparator<TestRedBlackNode>() {
                    @Override
                    public int compare(TestRedBlackNode node1, TestRedBlackNode node2) {
                        return node1.value - node2.value;
                    }
                }));
        node3.value = 3;
        node2.value = 2;

        node2.value = 4;
        node4.value = 2;
        assertFalse(isOrderValid(node3, null));
        node2.value = 2;
        node4.value = 4;

        node0.value = 1;
        node1.value = 0;
        assertFalse(isOrderValid(node3, null));
        node0.value = 0;
        node1.value = 1;

        // Do all of the assertions for which the tree is supposed to be valid at the end, to make sure we didn't make a
        // mistake undoing any of the modifications
        assertTrue(isSubtreeValid(node3));
        assertTrue(isSubtreeValid(node1));
        assertTrue(isSubtreeValid(node0));
        assertTrue(isSubtreeValid(TestRedBlackNode.LEAF));
        assertTrue(isOrderValid(node3, null));
        assertTrue(isOrderValid(node1, null));
        assertTrue(isOrderValid(node0, null));
        assertTrue(isOrderValid(TestRedBlackNode.LEAF, null));
        assertTrue(
                isOrderValid(node3, new Comparator<TestRedBlackNode>() {
                    @Override
                    public int compare(TestRedBlackNode node1, TestRedBlackNode node2) {
                        return node1.value - node2.value;
                    }
                }));
    }
}
