package com.github.btrekkie.interval_tree.test;

import com.github.btrekkie.interval_tree.IntervalTree;
import com.github.btrekkie.interval_tree.IntervalTreeInterval;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntervalTreeTest {
    /**
     * Tests IntervalTree.
     */
    @Test
    public void test() {
        IntervalTree tree = new IntervalTree();
        assertNull(tree.findInterval(0.5));
        assertNull(tree.findInterval(-1));
        tree.addInterval(new IntervalTreeInterval(5, 7));
        tree.addInterval(new IntervalTreeInterval(42, 48));
        tree.addInterval(new IntervalTreeInterval(-1, 2));
        tree.addInterval(new IntervalTreeInterval(6, 12));
        tree.addInterval(new IntervalTreeInterval(21, 23));
        assertTrue(tree.removeInterval(new IntervalTreeInterval(-1, 2)));
        assertFalse(tree.removeInterval(new IntervalTreeInterval(-1, 2)));
        tree.addInterval(new IntervalTreeInterval(-6, -2));
        assertEquals(new IntervalTreeInterval(6, 12), tree.findInterval(8));
        assertNull(tree.findInterval(0));
        assertEquals(new IntervalTreeInterval(21, 23), tree.findInterval(21));
        assertEquals(new IntervalTreeInterval(42, 48), tree.findInterval(48));
        IntervalTreeInterval interval = tree.findInterval(6.5);
        assertTrue(new IntervalTreeInterval(5, 7).equals(interval) || new IntervalTreeInterval(6, 12).equals(interval));

        tree = new IntervalTree();
        for (int i = 0; i < 500; i++) {
            tree.addInterval(new IntervalTreeInterval(2 * i, 2 * i + 1));
        }
        for (int i = 0; i < 250; i++) {
            tree.removeInterval(new IntervalTreeInterval(4 * i + 2, 4 * i + 3));
        }
        assertNull(tree.findInterval(123.5));
        assertEquals(new IntervalTreeInterval(124, 125), tree.findInterval(124.5));
        assertEquals(new IntervalTreeInterval(776, 777), tree.findInterval(776));
        assertEquals(new IntervalTreeInterval(0, 1), tree.findInterval(0.5));
        assertEquals(new IntervalTreeInterval(996, 997), tree.findInterval(997));
    }
}
