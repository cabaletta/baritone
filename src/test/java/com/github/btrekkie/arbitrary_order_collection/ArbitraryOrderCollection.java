package com.github.btrekkie.arbitrary_order_collection;

import java.util.Comparator;

/**
 * Provides objects ordered in an arbitrary, but consistent fashion, through the createValue() method.  To determine the
 * relative order of two values, use ArbitraryOrderValue.compareTo.  We may only compare values on which we have not
 * called "remove" that were created in the same ArbitraryOrderCollection instance.  Note that despite the name,
 * ArbitraryOrderCollection does not implement Collection.
 */
/* We implement an ArbitraryOrderCollection using a red-black tree.  We order the nodes arbitrarily.
 */
public class ArbitraryOrderCollection {
    /** The Comparator for ordering ArbitraryOrderNodes. */
    private static final Comparator<ArbitraryOrderNode> NODE_COMPARATOR = new Comparator<ArbitraryOrderNode>() {
        @Override
        public int compare(ArbitraryOrderNode node1, ArbitraryOrderNode node2) {
            return 0;
        }
    };

    /** The root node of the tree. */
    private ArbitraryOrderNode root = new ArbitraryOrderNode();

    /** Adds and returns a new value for ordering. */
    public ArbitraryOrderValue createValue() {
        ArbitraryOrderNode node = new ArbitraryOrderNode();
        root = root.insert(node, true, NODE_COMPARATOR);
        return new ArbitraryOrderValue(node);
    }

    /**
     * Removes the specified value from this collection.  Assumes we obtained the value by calling createValue() on this
     * instance of ArbitraryOrderCollection.  After calling "remove" on a value, we may no longer compare it to other
     * values.
     */
    public void remove(ArbitraryOrderValue value) {
        root = value.node.remove();
    }
}
